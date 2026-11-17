const express = require('express');
const db = require('../config/db');
const { authenticateToken, requireRole } = require('../middleware/auth');
const { emergencyRateLimit } = require('../middleware/rateLimitMiddleware');
const { uploadEmergencyPhoto, getUploadRelativePath } = require('../middleware/uploadMiddleware');
const { isValidUuid, unlinkFilesSafe } = require('../utils/validators');

const router = express.Router();

const ALLOWED_DISASTER_TYPES = new Set(['flood', 'fire', 'typhoon', 'earthquake', 'landslide']);
const ALLOWED_NEEDS = new Set(['food', 'water', 'medicine', 'shelter', 'evacuation']);

function parseNeedsInput(rawNeeds) {
    if (rawNeeds === undefined || rawNeeds === null || rawNeeds === '') {
        return { values: [], error: null };
    }

    let list;
    if (Array.isArray(rawNeeds)) {
        list = rawNeeds;
    } else if (typeof rawNeeds === 'string') {
        const trimmed = rawNeeds.trim();
        if (!trimmed) return { values: [], error: null };
        if (trimmed.startsWith('[')) {
            try {
                const parsed = JSON.parse(trimmed);
                if (!Array.isArray(parsed)) {
                    return { values: [], error: 'needs must be a valid array' };
                }
                list = parsed;
            } catch (_e) {
                return { values: [], error: 'needs must be a valid JSON array or comma-separated list' };
            }
        } else {
            list = trimmed.split(',');
        }
    } else {
        return { values: [], error: 'needs must be an array or comma-separated string' };
    }

    const normalized = [];
    const seen = new Set();
    for (const rawItem of list) {
        const item = String(rawItem).trim().toLowerCase();
        if (!item) continue;
        if (!ALLOWED_NEEDS.has(item)) {
            return {
                values: [],
                error: `Invalid need '${item}'. Allowed: food, water, medicine, shelter, evacuation`
            };
        }
        if (!seen.has(item)) {
            seen.add(item);
            normalized.push(item);
        }
    }
    return { values: normalized, error: null };
}

router.get('/', authenticateToken, async (req, res) => {
    try {
        if (req.user.role === 'resident') {
            const result = await db.query(
                'SELECT * FROM "emergencyReports" WHERE "userId" = $1 ORDER BY "createdAt" DESC',
                [req.user.id]
            );
            return res.json(result.rows);
        }

        const result = await db.query('SELECT * FROM "emergencyReports" ORDER BY "createdAt" DESC');
        return res.json(result.rows);
    } catch (err) {
        console.error('Error fetching emergency reports:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

router.get('/grouped-by-purok', authenticateToken, requireRole('staff', 'admin'), async (_req, res) => {
    try {
        const result = await db.query('SELECT * FROM "emergencyReports" ORDER BY "createdAt" DESC');
        const grouped = {};
        result.rows.forEach((report) => {
            const p = report.purok || 'Unassigned';
            if (!grouped[p]) grouped[p] = [];
            grouped[p].push(report);
        });
        return res.json(grouped);
    } catch (err) {
        console.error('Error grouping emergency reports:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

router.get('/:id', authenticateToken, async (req, res) => {
    try {
        if (!isValidUuid(req.params.id)) {
            return res.status(400).json({ error: 'Invalid emergency report ID' });
        }
        const result = await db.query('SELECT * FROM "emergencyReports" WHERE "reportId" = $1', [req.params.id]);
        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Emergency report not found' });
        }
        const record = result.rows[0];
        if (req.user.role === 'resident' && record.userId !== req.user.id) {
            return res.status(403).json({ error: 'Access denied' });
        }
        return res.json(record);
    } catch (err) {
        console.error('Error fetching emergency report detail:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

router.post('/', authenticateToken, requireRole('resident'), emergencyRateLimit, uploadEmergencyPhoto, async (req, res) => {
    try {
        if (!req.user.purok) {
            unlinkFilesSafe(req);
            return res.status(400).json({ error: 'Your profile has no assigned Purok' });
        }

        const body = req.body || {};
        const rawDisaster = typeof body.disasterType === 'string'
            ? body.disasterType.trim().toLowerCase()
            : '';

        if (!ALLOWED_DISASTER_TYPES.has(rawDisaster)) {
            unlinkFilesSafe(req);
            return res.status(400).json({
                error: 'disasterType must be one of: flood, fire, typhoon, earthquake, landslide'
            });
        }

        const rawCount = body.householdMembersAffected;
        const countStr = typeof rawCount === 'number' ? String(rawCount) : String(rawCount || '').trim();
        if (!/^\d+$/.test(countStr)) {
            unlinkFilesSafe(req);
            return res.status(400).json({ error: 'householdMembersAffected must be a positive integer' });
        }
        const affected = Number(countStr);
        if (!Number.isSafeInteger(affected) || affected < 1) {
            unlinkFilesSafe(req);
            return res.status(400).json({ error: 'householdMembersAffected must be at least 1' });
        }

        const parsedNeeds = parseNeedsInput(body.needs);
        if (parsedNeeds.error) {
            unlinkFilesSafe(req);
            return res.status(400).json({ error: parsedNeeds.error });
        }
        const needs = parsedNeeds.values;

        const confirmDuplicate = body.confirmDuplicate === true || body.confirmDuplicate === 'true';

        if (!confirmDuplicate) {
            const dup = await db.query(
                `SELECT "reportId"
                 FROM "emergencyReports"
                 WHERE "userId" = $1
                   AND "disasterType" = $2
                   AND "createdAt" > now() - interval '10 minutes'
                 LIMIT 1`,
                [req.user.id, rawDisaster]
            );
            if (dup.rows.length > 0) {
                unlinkFilesSafe(req);
                return res.status(409).json({
                    error: 'You already submitted a similar emergency report in the last 10 minutes. Submit again?',
                    code: 'DUPLICATE_EMERGENCY_WARNING'
                });
            }
        }

        const photoUrl = req.file ? getUploadRelativePath('emergencies', req.file.filename) : null;

        const result = await db.query(
            `INSERT INTO "emergencyReports" ("userId", purok, "disasterType", "householdMembersAffected", needs, "photoUrl")
             VALUES ($1, $2, $3, $4, $5, $6)
             RETURNING *`,
            [req.user.id, req.user.purok, rawDisaster, affected, needs.length > 0 ? needs : null, photoUrl]
        );

        return res.status(201).json(result.rows[0]);
    } catch (err) {
        unlinkFilesSafe(req);
        console.error('Error creating emergency report:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

router.patch('/:id/verify', authenticateToken, requireRole('staff', 'admin'), async (req, res) => {
    try {
        if (!isValidUuid(req.params.id)) {
            return res.status(400).json({ error: 'Invalid emergency report ID' });
        }
        const result = await db.query(
            'UPDATE "emergencyReports" SET status = \'verified\' WHERE "reportId" = $1 RETURNING *',
            [req.params.id]
        );
        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Report not found' });
        }
        return res.json(result.rows[0]);
    } catch (err) {
        console.error('Error verifying emergency report:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router;
