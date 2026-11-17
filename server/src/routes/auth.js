const crypto = require('crypto');
const express = require('express');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const db = require('../config/db');
const { getJwtSecret } = require('../middleware/auth');
const emailService = require('../services/emailService');
const { isValidIsoDate } = require('../utils/validators');

const router = express.Router();

const SAFE_PROFILE_RETURNING = `
    id, username, "fullName", email, "mobileNumber", sex, birthdate,
    "civilStatus", citizenship, birthplace, purok, address,
    "residencyType", "residencyPeriod", sector, "avatarUrl",
    "consentAcceptedAt", role, "registrationMethod", "isActive", "createdAt"
`;

const ALLOWED_OTP_PURPOSES = new Set(['registration', 'password_reset']);
const MAX_OTP_SEND_PER_15M = 5;
const MAX_OTP_VERIFY_ATTEMPTS = 5;
const otpFailedAttempts = new Map();

function isStrongPassword(password) {
    if (typeof password !== 'string' || password.length < 8) return false;
    return /[A-Z]/.test(password) && /[0-9]/.test(password) && /[^A-Za-z0-9]/.test(password);
}

function normalizeTextArray(value) {
    if (!value) return null;
    if (Array.isArray(value)) {
        const cleaned = value.map(v => String(v).trim()).filter(Boolean);
        return cleaned.length > 0 ? cleaned : null;
    }
    return null;
}

// Send 6-digit email OTP (stored in PostgreSQL otpCodes, 10-minute expiry)
router.post('/send-otp', async (req, res) => {
    try {
        const body = req.body || {};
        const email = typeof body.email === 'string' ? body.email.trim().toLowerCase() : '';
        const purpose = typeof body.purpose === 'string' && body.purpose.trim()
            ? body.purpose.trim()
            : 'registration';

        if (!ALLOWED_OTP_PURPOSES.has(purpose)) {
            return res.status(400).json({ error: 'Invalid OTP purpose' });
        }

        if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            return res.status(400).json({ error: 'A valid email address is required' });
        }

        if (purpose === 'registration') {
            const existing = await db.query('SELECT id FROM profiles WHERE LOWER(email) = LOWER($1)', [email]);
            if (existing.rows.length > 0) {
                return res.status(409).json({ error: 'An account with this email is already registered' });
            }
        }

        const rateResult = await db.query(
            `SELECT COUNT(*)::int AS count
             FROM "otpCodes"
             WHERE LOWER(email) = LOWER($1)
               AND purpose = $2
               AND "createdAt" > now() - interval '15 minutes'`,
            [email, purpose]
        );
        if ((rateResult.rows[0] ? rateResult.rows[0].count : 0) >= MAX_OTP_SEND_PER_15M) {
            return res.status(429).json({
                error: 'Too many verification code requests. Please wait 15 minutes before trying again.',
                retryAfterSeconds: 900
            });
        }

        const code = String(crypto.randomInt(100000, 1000000));
        const emailResult = await emailService.sendEmail({
            to: email,
            subject: 'BDMS Mobile — Your Verification Code',
            text: `Your BDMS Mobile verification code is ${code}. It expires in 10 minutes.`,
            html: `<p>Your <strong>BDMS Mobile</strong> verification code is:</p>
                   <h2 style="letter-spacing:4px;">${code}</h2>
                   <p>This code expires in 10 minutes. If you did not request this, ignore this email.</p>`
        });

        if (!emailResult.success) {
            return res.status(502).json({
                error: emailResult.error || 'Failed to send verification email'
            });
        }

        // Cleanup old expired OTPs and invalidate earlier active codes for this email+purpose
        await db.query('DELETE FROM "otpCodes" WHERE "expiresAt" < now() - interval \'1 day\'');
        await db.query(
            `UPDATE "otpCodes"
             SET "consumedAt" = now()
             WHERE LOWER(email) = LOWER($1)
               AND purpose = $2
               AND "consumedAt" IS NULL`,
            [email, purpose]
        );
        await db.query(
            `INSERT INTO "otpCodes" (email, code, purpose, "expiresAt")
             VALUES ($1, $2, $3, now() + interval '10 minutes')`,
            [email, code, purpose]
        );

        otpFailedAttempts.delete(`${email}:${purpose}`);

        return res.json({ success: true, message: 'Verification code sent to your email' });
    } catch (err) {
        console.error('Send OTP error:', err);
        return res.status(500).json({ error: 'Server error while sending verification code' });
    }
});

// Verify 6-digit OTP, mark consumed, and issue a 15-minute verification proof token
router.post('/verify-otp', async (req, res) => {
    try {
        const body = req.body || {};
        const email = typeof body.email === 'string' ? body.email.trim().toLowerCase() : '';
        const code = typeof body.code === 'string' ? body.code.trim() : '';
        const purpose = typeof body.purpose === 'string' && body.purpose.trim()
            ? body.purpose.trim()
            : 'registration';

        if (!ALLOWED_OTP_PURPOSES.has(purpose)) {
            return res.status(400).json({ error: 'Invalid OTP purpose' });
        }

        if (!email || !/^\d{6}$/.test(code)) {
            return res.status(400).json({ error: 'Email and 6-digit verification code are required' });
        }

        const attemptKey = `${email}:${purpose}`;
        const priorFails = otpFailedAttempts.get(attemptKey) || 0;
        if (priorFails >= MAX_OTP_VERIFY_ATTEMPTS) {
            return res.status(429).json({
                error: 'Too many failed verification attempts. Please request a new code.'
            });
        }

        const result = await db.query(
            `SELECT "otpId"
             FROM "otpCodes"
             WHERE LOWER(email) = LOWER($1)
               AND code = $2
               AND purpose = $3
               AND "consumedAt" IS NULL
               AND "expiresAt" > now()
             ORDER BY "createdAt" DESC
             LIMIT 1`,
            [email, code, purpose]
        );

        if (result.rows.length === 0) {
            const nextFails = priorFails + 1;
            otpFailedAttempts.set(attemptKey, nextFails);
            if (nextFails >= MAX_OTP_VERIFY_ATTEMPTS) {
                await db.query(
                    `UPDATE "otpCodes"
                     SET "consumedAt" = now()
                     WHERE LOWER(email) = LOWER($1)
                       AND purpose = $2
                       AND "consumedAt" IS NULL`,
                    [email, purpose]
                );
                return res.status(429).json({
                    error: 'Too many failed verification attempts. Please request a new code.'
                });
            }
            return res.status(400).json({ error: 'Invalid or expired verification code' });
        }

        await db.query(
            'UPDATE "otpCodes" SET "consumedAt" = now() WHERE "otpId" = $1',
            [result.rows[0].otpId]
        );
        otpFailedAttempts.delete(attemptKey);

        const otpVerificationToken = jwt.sign(
            { email, purpose, otpVerified: true },
            getJwtSecret(),
            { expiresIn: '15m' }
        );

        return res.json({
            success: true,
            verified: true,
            otpVerificationToken,
            message: 'OTP verified successfully'
        });
    } catch (err) {
        console.error('Verify OTP error:', err);
        return res.status(500).json({ error: 'Server error while verifying code' });
    }
});

// Resident online registration (requires OTP proof and writes all 6-step onboarding fields)
router.post('/register', async (req, res) => {
    const body = req.body || {};
    const {
        username,
        fullName,
        email,
        password,
        mobileNumber,
        sex,
        birthdate,
        civilStatus,
        citizenship,
        birthplace,
        purok,
        address,
        residencyType,
        residencyPeriod,
        sector,
        ctcNumber,
        ctcOrNumber,
        disabilities,
        consentAccepted,
        consentAcceptedAt,
        otpVerificationToken,
        otpCode
    } = body;

    const cleanEmail = typeof email === 'string' ? email.trim().toLowerCase() : '';
    const cleanUsername = typeof username === 'string' && username.trim()
        ? username.trim()
        : (cleanEmail ? cleanEmail.split('@')[0] : '');
    const cleanFullName = typeof fullName === 'string' ? fullName.trim() : '';

    if (!cleanUsername || !cleanFullName || !cleanEmail || !password) {
        return res.status(400).json({ error: 'Username, full name, email, and password are required' });
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(cleanEmail)) {
        return res.status(400).json({ error: 'A valid email address is required' });
    }

    if (!isStrongPassword(password)) {
        return res.status(400).json({
            error: 'Password must be at least 8 characters with 1 uppercase letter, 1 number, and 1 special character'
        });
    }

    if (!purok || typeof purok !== 'string' || !purok.trim()) {
        return res.status(400).json({ error: 'Purok is required' });
    }

    const cleanBirthdate = typeof birthdate === 'string' && birthdate.trim() ? birthdate.trim() : null;
    if (cleanBirthdate && !isValidIsoDate(cleanBirthdate, { allowFuture: false })) {
        return res.status(400).json({ error: 'Birthdate must be a valid past/present YYYY-MM-DD date' });
    }

    const hasConsent = Boolean(consentAccepted || consentAcceptedAt);
    if (!hasConsent) {
        return res.status(400).json({ error: 'RA 10173 privacy consent must be accepted' });
    }

    const consentTimestamp = consentAcceptedAt ? new Date(consentAcceptedAt) : new Date();
    if (Number.isNaN(consentTimestamp.getTime())) {
        return res.status(400).json({ error: 'Invalid consent timestamp' });
    }

    let verifiedViaToken = false;
    if (typeof otpVerificationToken === 'string' && otpVerificationToken.trim()) {
        try {
            const claims = jwt.verify(otpVerificationToken.trim(), getJwtSecret());
            if (
                claims &&
                claims.otpVerified === true &&
                claims.purpose === 'registration' &&
                typeof claims.email === 'string' &&
                claims.email.trim().toLowerCase() === cleanEmail
            ) {
                verifiedViaToken = true;
            }
        } catch (_tokenErr) {
            return res.status(400).json({ error: 'Email verification expired or invalid. Please request a new code.' });
        }
    }

    const cleanOtpCode = typeof otpCode === 'string' ? otpCode.trim() : '';
    if (!verifiedViaToken && !/^\d{6}$/.test(cleanOtpCode)) {
        return res.status(400).json({ error: 'Email verification via OTP is required before registration' });
    }

    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');

        if (!verifiedViaToken) {
            const otpLookup = await client.query(
                `SELECT "otpId"
                 FROM "otpCodes"
                 WHERE LOWER(email) = LOWER($1)
                   AND code = $2
                   AND purpose = 'registration'
                   AND "consumedAt" IS NULL
                   AND "expiresAt" > now()
                 ORDER BY "createdAt" DESC
                 LIMIT 1
                 FOR UPDATE`,
                [cleanEmail, cleanOtpCode]
            );
            if (otpLookup.rows.length === 0) {
                await client.query('ROLLBACK');
                return res.status(400).json({ error: 'Invalid or expired verification code' });
            }
            await client.query(
                'UPDATE "otpCodes" SET "consumedAt" = now() WHERE "otpId" = $1',
                [otpLookup.rows[0].otpId]
            );
        }

        const purokCheck = await client.query('SELECT name FROM puroks WHERE name = $1', [purok.trim()]);
        if (purokCheck.rows.length === 0) {
            await client.query('ROLLBACK');
            return res.status(400).json({ error: 'Selected Purok does not exist' });
        }

        const salt = await bcrypt.genSalt(10);
        const passwordHash = await bcrypt.hash(password, salt);

        const result = await client.query(
            `INSERT INTO profiles (
                username, "fullName", email, "passwordHash", "mobileNumber",
                sex, birthdate, "civilStatus", citizenship, birthplace,
                purok, address, "residencyType", "residencyPeriod", sector,
                "ctcNumber", "ctcOrNumber", disabilities, "consentAcceptedAt",
                role, "registrationMethod"
             ) VALUES (
                $1, $2, $3, $4, $5,
                $6, $7, $8, $9, $10,
                $11, $12, $13, $14, $15,
                $16, $17, $18, $19,
                'resident', 'online'
             )
             RETURNING ${SAFE_PROFILE_RETURNING}`,
            [
                cleanUsername,
                cleanFullName,
                cleanEmail,
                passwordHash,
                mobileNumber || null,
                sex || null,
                cleanBirthdate,
                civilStatus || null,
                citizenship || null,
                birthplace || null,
                purok.trim(),
                address || null,
                residencyType || null,
                residencyPeriod || null,
                normalizeTextArray(sector),
                ctcNumber || null,
                ctcOrNumber || null,
                normalizeTextArray(disabilities),
                consentTimestamp.toISOString()
            ]
        );

        await client.query('COMMIT');

        const user = result.rows[0];
        const token = jwt.sign(
            { id: user.id, role: user.role, purok: user.purok },
            getJwtSecret(),
            { expiresIn: '1h' }
        );

        return res.status(201).json({ token, user });
    } catch (err) {
        try {
            await client.query('ROLLBACK');
        } catch (_rollbackErr) {
            // ignore rollback error
        }
        console.error('Registration error:', err);
        if (err.code === '23505') {
            return res.status(409).json({ error: 'Email or username is already registered' });
        }
        return res.status(500).json({ error: 'Server error during registration' });
    } finally {
        client.release();
    }
});

// Login
router.post('/login', async (req, res) => {
    try {
        const body = req.body || {};
        const email = typeof body.email === 'string' ? body.email.trim().toLowerCase() : '';
        const password = typeof body.password === 'string' ? body.password : '';

        if (!email || !password) {
            return res.status(400).json({ error: 'Email and password are required' });
        }

        const result = await db.query('SELECT * FROM profiles WHERE LOWER(email) = LOWER($1)', [email]);
        if (result.rows.length === 0) {
            return res.status(401).json({ error: 'Invalid email or password' });
        }

        const user = result.rows[0];
        const validPassword = await bcrypt.compare(password, user.passwordHash);
        if (!validPassword) {
            return res.status(401).json({ error: 'Invalid email or password' });
        }

        if (!user.isActive) {
            return res.status(403).json({ error: 'Account is deactivated' });
        }

        const token = jwt.sign(
            { id: user.id, role: user.role, purok: user.purok },
            getJwtSecret(),
            { expiresIn: '1h' }
        );

        return res.json({
            token,
            user: {
                id: user.id,
                username: user.username,
                fullName: user.fullName,
                email: user.email,
                mobileNumber: user.mobileNumber,
                purok: user.purok,
                avatarUrl: user.avatarUrl,
                role: user.role,
                isActive: user.isActive,
                createdAt: user.createdAt
            }
        });
    } catch (err) {
        console.error('Login error:', err);
        return res.status(500).json({ error: 'Server error during login' });
    }
});

module.exports = router;
