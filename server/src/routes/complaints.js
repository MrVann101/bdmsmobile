const express = require('express');
const db = require('../config/db');
const { authenticateToken, requireRole } = require('../middleware/auth');
const { complaintRateLimit } = require('../middleware/rateLimitMiddleware');
const { uploadComplaintPhotos, getUploadRelativePath } = require('../middleware/uploadMiddleware');
const { isValidUuid, unlinkFilesSafe } = require('../utils/validators');

const router = express.Router();

const ALLOWED_CATEGORIES = new Set(['noise', 'sanitation', 'dispute', 'infrastructure', 'other']);

function computeSlaHours(category) {
    return category === 'infrastructure' ? 72 : 48;
}

router.get('/', authenticateToken, async (req, res) => {
    try {
        let query = 'SELECT * FROM complaints';
        const params = [];

        if (req.user.role === 'resident') {
            query += ' WHERE "userId" = $1 ORDER BY "createdAt" DESC';
            params.push(req.user.id);
        } else {
            query += ' ORDER BY (priority = \'high\') DESC, "slaDueAt" ASC';
        }

        const result = await db.query(query, params);
        return res.json(result.rows);
    } catch (err) {
        console.error('Error fetching complaints:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

router.get('/:id', authenticateToken, async (req, res) => {
    try {
        if (!isValidUuid(req.params.id)) {
            return res.status(400).json({ error: 'Invalid complaint ID' });
        }
        const result = await db.query('SELECT * FROM complaints WHERE "complaintId" = $1', [req.params.id]);
        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Complaint not found' });
        }
        const record = result.rows[0];
        if (req.user.role === 'resident' && record.userId !== req.user.id) {
            return res.status(403).json({ error: 'Access denied' });
        }
        return res.json(record);
    } catch (err) {
        console.error('Error fetching complaint detail:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

router.post('/', authenticateToken, requireRole('resident'), complaintRateLimit, uploadComplaintPhotos, async (req, res) => {
    try {
        const body = req.body || {};
        const rawCategory = typeof body.category === 'string' ? body.category.trim().toLowerCase() : '';
        const cleanDescription = typeof body.description === 'string' ? body.description.trim() : '';

        if (!ALLOWED_CATEGORIES.has(rawCategory)) {
            unlinkFilesSafe(req);
            return res.status(400).json({
                error: 'Category must be one of: noise, sanitation, dispute, infrastructure, other'
            });
        }
        if (!cleanDescription) {
            unlinkFilesSafe(req);
            return res.status(400).json({ error: 'Description is required' });
        }

        const slaHours = computeSlaHours(rawCategory);
        const slaDueAt = new Date(Date.now() + slaHours * 60 * 60 * 1000);
        const photoUrls = Array.isArray(req.files) && req.files.length > 0
            ? req.files.map((f) => getUploadRelativePath('complaints', f.filename))
            : null;

        const result = await db.query(
            `INSERT INTO complaints ("userId", purok, category, description, "photoUrls", "slaDueAt")
             VALUES ($1, $2, $3, $4, $5, $6)
             RETURNING *`,
            [req.user.id, req.user.purok, rawCategory, cleanDescription, photoUrls, slaDueAt]
        );

        return res.status(201).json(result.rows[0]);
    } catch (err) {
        unlinkFilesSafe(req);
        console.error('Error creating complaint:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

router.patch('/:id/resolve', authenticateToken, requireRole('staff', 'admin'), async (req, res) => {
    try {
        if (!isValidUuid(req.params.id)) {
            return res.status(400).json({ error: 'Invalid complaint ID' });
        }
        const { status, staffResponse } = req.body || {};
        const nextStatus = status || 'resolved';
        const result = await db.query(
            `UPDATE complaints
             SET status = $1,
                 "staffResponse" = $2,
                 "handledBy" = $3,
                 "resolvedAt" = CASE WHEN $1 IN ('resolved', 'dismissed') THEN now() ELSE "resolvedAt" END
             WHERE "complaintId" = $4
             RETURNING *`,
            [nextStatus, staffResponse || null, req.user.id, req.params.id]
        );
        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Complaint not found' });
        }
        return res.json(result.rows[0]);
    } catch (err) {
        console.error('Error resolving complaint:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router;
