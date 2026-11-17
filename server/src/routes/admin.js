const express = require('express');
const router = express.Router();
const db = require('../config/db');
const { authenticateToken, requireRole } = require('../middleware/auth');
const bcrypt = require('bcrypt');

router.get('/users', authenticateToken, requireRole('admin'), async (req, res) => {
    try {
        const q = req.query.q || '';
        const result = await db.query(
            `SELECT id, username, "fullName", email, "mobileNumber", purok, role, "isActive", "createdAt"
             FROM profiles
             WHERE "fullName" ILIKE $1 OR email ILIKE $1
             ORDER BY "createdAt" DESC`,
            [`%${q}%`]
        );
        res.json(result.rows);
    } catch (err) {
        console.error('Error searching users:', err);
        res.status(500).json({ error: 'Server error' });
    }
});

router.post('/users', authenticateToken, requireRole('admin'), async (req, res) => {
    try {
        const { username, fullName, email, password, mobileNumber, purok, role } = req.body;
        const salt = await bcrypt.genSalt(10);
        const passwordHash = await bcrypt.hash(password || 'Staff@123', salt);
        const uname = username || email.split('@')[0];

        const result = await db.query(
            `INSERT INTO profiles (username, "fullName", email, "passwordHash", "mobileNumber", purok, role)
             VALUES ($1, $2, $3, $4, $5, $6, $7)
             RETURNING id, username, "fullName", email, "mobileNumber", purok, role, "isActive"`,
            [uname, fullName, email, passwordHash, mobileNumber, purok, role || 'staff']
        );
        res.status(201).json(result.rows[0]);
    } catch (err) {
        console.error('Error creating user:', err);
        if (err.code === '23505') return res.status(400).json({ error: 'Email or username already exists' });
        res.status(500).json({ error: 'Server error' });
    }
});

router.patch('/users/:id/role', authenticateToken, requireRole('admin'), async (req, res) => {
    try {
        const { role } = req.body;
        const result = await db.query(
            'UPDATE profiles SET role = $1 WHERE id = $2 RETURNING id, username, "fullName", email, role',
            [role, req.params.id]
        );
        if (result.rows.length === 0) return res.status(404).json({ error: 'User not found' });
        res.json(result.rows[0]);
    } catch (err) {
        console.error('Error updating user role:', err);
        res.status(500).json({ error: 'Server error' });
    }
});

router.patch('/users/:id/active', authenticateToken, requireRole('admin'), async (req, res) => {
    try {
        const { isActive } = req.body;
        const result = await db.query(
            'UPDATE profiles SET "isActive" = $1 WHERE id = $2 RETURNING id, username, "fullName", email, "isActive"',
            [isActive, req.params.id]
        );
        if (result.rows.length === 0) return res.status(404).json({ error: 'User not found' });
        res.json(result.rows[0]);
    } catch (err) {
        console.error('Error updating user active status:', err);
        res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router;
