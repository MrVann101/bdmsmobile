const express = require('express');
const router = express.Router();
const db = require('../config/db');
const { authenticateToken, requireRole } = require('../middleware/auth');

router.get('/', async (req, res) => {
    try {
        const result = await db.query('SELECT name, description, "createdAt" FROM puroks ORDER BY name ASC');
        res.json(result.rows);
    } catch (err) {
        console.error('Error fetching puroks:', err);
        res.status(500).json({ error: 'Server error' });
    }
});

router.post('/', authenticateToken, requireRole('admin'), async (req, res) => {
    try {
        const { name, description } = req.body;
        if (!name) return res.status(400).json({ error: 'Purok name required' });

        const result = await db.query(
            'INSERT INTO puroks (name, description) VALUES ($1, $2) RETURNING name, description',
            [name, description]
        );
        res.status(201).json(result.rows[0]);
    } catch (err) {
        console.error('Error creating purok:', err);
        res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router;
