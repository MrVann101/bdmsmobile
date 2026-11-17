const express = require('express');
const db = require('../config/db');
const { authenticateToken } = require('../middleware/auth');
const { uploadAvatar, getUploadRelativePath } = require('../middleware/uploadMiddleware');
const { unlinkFilesSafe } = require('../utils/validators');

const router = express.Router();

const OWN_PROFILE_COLUMNS = `
    id, username, "fullName", email, "mobileNumber",
    sex, birthdate, "civilStatus", citizenship, birthplace,
    purok, address, "residencyType", "residencyPeriod", sector,
    "ctcNumber", "ctcOrNumber", disabilities, "avatarUrl",
    "consentAcceptedAt", role, "registrationMethod", "registeredBy",
    "isActive", "createdAt"
`;

// Get own full profile (includes own CTC/disabilities, NEVER passwordHash)
router.get('/me', authenticateToken, async (req, res) => {
    try {
        const result = await db.query(
            `SELECT ${OWN_PROFILE_COLUMNS} FROM profiles WHERE id = $1`,
            [req.user.id]
        );
        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Profile not found' });
        }
        return res.json(result.rows[0]);
    } catch (err) {
        console.error('Error loading own profile:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

const PROTECTED_FIELDS = new Set([
    'id',
    'username',
    'email',
    'passwordHash',
    'purok',
    'role',
    'isActive',
    'registrationMethod',
    'registeredBy',
    'consentAcceptedAt',
    'avatarUrl',
    'createdAt'
]);

// Update safe editable profile fields (Purok, role, identity, active state cannot be changed here)
router.patch('/me', authenticateToken, async (req, res) => {
    try {
        const body = req.body && typeof req.body === 'object' ? req.body : {};
        if (body.purok !== undefined) {
            return res.status(403).json({ error: 'Purok cannot be changed from profile settings' });
        }
        for (const restrictedKey of PROTECTED_FIELDS) {
            if (Object.prototype.hasOwnProperty.call(body, restrictedKey)) {
                return res.status(403).json({ error: `Restricted field '${restrictedKey}' cannot be updated here` });
            }
        }

        const allowedFields = [
            ['mobileNumber', '"mobileNumber"'],
            ['civilStatus', '"civilStatus"'],
            ['citizenship', 'citizenship'],
            ['address', 'address'],
            ['residencyType', '"residencyType"'],
            ['residencyPeriod', '"residencyPeriod"'],
            ['ctcNumber', '"ctcNumber"'],
            ['ctcOrNumber', '"ctcOrNumber"'],
            ['sector', 'sector'],
            ['disabilities', 'disabilities']
        ];

        const setClauses = [];
        const params = [];
        let paramIdx = 1;

        for (const [bodyKey, sqlCol] of allowedFields) {
            if (Object.prototype.hasOwnProperty.call(body, bodyKey)) {
                let value = body[bodyKey];
                if (bodyKey === 'sector' || bodyKey === 'disabilities') {
                    if (value !== null) {
                        if (!Array.isArray(value) || !value.every(item => typeof item === 'string')) {
                            return res.status(400).json({ error: `${bodyKey} must be an array of strings` });
                        }
                        const cleaned = value.map(item => item.trim()).filter(Boolean);
                        value = cleaned.length > 0 ? cleaned : null;
                    }
                } else if (value !== null) {
                    if (typeof value !== 'string') {
                        return res.status(400).json({ error: `${bodyKey} must be a string` });
                    }
                    const trimmed = value.trim();
                    value = trimmed.length > 0 ? trimmed : null;
                }
                setClauses.push(`${sqlCol} = $${paramIdx}`);
                params.push(value);
                paramIdx++;
            }
        }

        if (setClauses.length === 0) {
            return res.status(400).json({ error: 'No updatable profile fields provided' });
        }

        params.push(req.user.id);
        const result = await db.query(
            `UPDATE profiles
             SET ${setClauses.join(', ')}
             WHERE id = $${paramIdx}
             RETURNING ${OWN_PROFILE_COLUMNS}`,
            params
        );

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Profile not found' });
        }
        return res.json(result.rows[0]);
    } catch (err) {
        console.error('Error updating profile:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

// Upload own avatar photo
router.post('/me/avatar', authenticateToken, uploadAvatar, async (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ error: 'Avatar image file is required' });
        }

        const avatarUrl = getUploadRelativePath('avatars', req.file.filename);
        const result = await db.query(
            `UPDATE profiles
             SET "avatarUrl" = $1
             WHERE id = $2
             RETURNING ${OWN_PROFILE_COLUMNS}`,
            [avatarUrl, req.user.id]
        );

        if (result.rows.length === 0) {
            unlinkFilesSafe(req);
            return res.status(404).json({ error: 'Profile not found' });
        }
        return res.json(result.rows[0]);
    } catch (err) {
        unlinkFilesSafe(req);
        console.error('Error uploading avatar:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router;
