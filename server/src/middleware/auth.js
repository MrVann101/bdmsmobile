const jwt = require('jsonwebtoken');
const db = require('../config/db');
const { isValidUuid } = require('../utils/validators');
require('dotenv').config();

function getJwtSecret() {
    const secret = process.env.JWT_SECRET;
    if (!secret || !secret.trim()) {
        throw new Error('JWT_SECRET environment variable is not configured');
    }
    return secret;
}

const authenticateToken = async (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) {
        return res.status(401).json({ error: 'Access token required' });
    }

    let decoded;
    try {
        decoded = jwt.verify(token, getJwtSecret());
    } catch (err) {
        return res.status(403).json({ error: 'Invalid or expired token' });
    }

    if (!decoded || !isValidUuid(decoded.id) || !decoded.role) {
        return res.status(401).json({ error: 'Malformed authentication token' });
    }

    try {
        const result = await db.query(
            'SELECT id, role, purok, email, "fullName", "isActive" FROM profiles WHERE id = $1',
            [decoded.id]
        );
        if (result.rows.length === 0) {
            return res.status(401).json({ error: 'Account no longer exists' });
        }

        const user = result.rows[0];
        if (!user.isActive) {
            return res.status(403).json({ error: 'Account is deactivated' });
        }

        req.user = {
            id: user.id,
            role: user.role,
            purok: user.purok,
            email: user.email,
            fullName: user.fullName,
            isActive: user.isActive
        };
        return next();
    } catch (err) {
        console.error('Auth verification DB error:', err);
        return res.status(500).json({ error: 'Authentication verification failed' });
    }
};

const requireRole = (...roles) => {
    return (req, res, next) => {
        if (!req.user || !roles.includes(req.user.role)) {
            return res.status(403).json({ error: 'Access denied: insufficient permissions' });
        }
        return next();
    };
};

module.exports = {
    authenticateToken,
    requireRole,
    getJwtSecret
};
