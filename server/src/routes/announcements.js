const express = require('express');
const db = require('../config/db');
const { authenticateToken, requireRole } = require('../middleware/auth');

const router = express.Router();

router.get('/', authenticateToken, async (req, res) => {
    try {
        if (req.user.role === 'resident') {
            const result = await db.query(
                `SELECT *
                 FROM announcements
                 WHERE ("expiresAt" IS NULL OR "expiresAt" > now())
                   AND (
                       "targetPurok" IS NULL
                       OR cardinality("targetPurok") = 0
                       OR ($1::text IS NOT NULL AND $1::text = ANY("targetPurok"))
                   )
                 ORDER BY (category = 'emergency') DESC, "createdAt" DESC`,
                [req.user.purok || null]
            );
            return res.json(result.rows);
        }

        const result = await db.query('SELECT * FROM announcements ORDER BY "createdAt" DESC');
        return res.json(result.rows);
    } catch (err) {
        console.error('Error fetching announcements:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

router.post('/', authenticateToken, requireRole('staff', 'admin'), async (req, res) => {
    try {
        const { title, body, category, targetPurok, expiresAt } = req.body;
        const result = await db.query(
            `INSERT INTO announcements (title, body, category, "targetPurok", "postedBy", "expiresAt")
             VALUES ($1, $2, $3, $4, $5, $6)
             RETURNING *`,
            [title, body, category || 'general', targetPurok || null, req.user.id, expiresAt || null]
        );
        return res.status(201).json(result.rows[0]);
    } catch (err) {
        console.error('Error creating announcement:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router;
