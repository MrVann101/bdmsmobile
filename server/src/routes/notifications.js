const express = require('express');
const db = require('../config/db');
const { authenticateToken, requireRole } = require('../middleware/auth');
const { isValidUuid } = require('../utils/validators');

const router = express.Router();

router.get('/me', authenticateToken, async (req, res) => {
    try {
        const result = await db.query(
            'SELECT * FROM notifications WHERE "userId" = $1 ORDER BY "createdAt" DESC',
            [req.user.id]
        );
        return res.json(result.rows);
    } catch (err) {
        console.error('Error fetching notifications:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

router.patch('/:id/read', authenticateToken, async (req, res) => {
    try {
        if (!isValidUuid(req.params.id)) {
            return res.status(400).json({ error: 'Invalid notification ID' });
        }
        const result = await db.query(
            `UPDATE notifications
             SET "isRead" = TRUE
             WHERE "notificationId" = $1 AND "userId" = $2
             RETURNING *`,
            [req.params.id, req.user.id]
        );
        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Notification not found' });
        }
        return res.json(result.rows[0]);
    } catch (err) {
        console.error('Error marking notification read:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

router.get('/', authenticateToken, requireRole('admin'), async (req, res) => {
    try {
        let query = 'SELECT * FROM notifications';
        const params = [];
        if (req.query.deliveryStatus) {
            query += ' WHERE "deliveryStatus" = $1';
            params.push(req.query.deliveryStatus);
        }
        query += ' ORDER BY "createdAt" DESC';
        const result = await db.query(query, params);
        return res.json(result.rows);
    } catch (err) {
        console.error('Error fetching admin notifications:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router;
