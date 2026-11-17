const fs = require('fs');
const path = require('path');
const express = require('express');
const db = require('../config/db');
const { authenticateToken } = require('../middleware/auth');
const { UPLOADS_ROOT } = require('../middleware/uploadMiddleware');

const router = express.Router();

const ALLOWED_TYPES = new Set([
    'avatars',
    'clearances',
    'complaints',
    'emergencies',
    'announcements',
    'templates',
    'certificates'
]);

const RESTRICTED_TYPES = new Set([
    'clearances',
    'complaints',
    'emergencies',
    'certificates',
    'announcements',
    'templates'
]);

async function residentCanAccessFile(user, type, relPath, apiPath) {
    if (type === 'templates') {
        return false;
    }
    if (type === 'clearances') {
        const res = await db.query(
            `SELECT 1 FROM "documentRequests"
             WHERE "userId" = $1 AND "purokClearanceUrl" IN ($2, $3)
             LIMIT 1`,
            [user.id, relPath, apiPath]
        );
        return res.rows.length > 0;
    }
    if (type === 'certificates') {
        const res = await db.query(
            `SELECT 1 FROM "documentRequests"
             WHERE "userId" = $1 AND "certificateUrl" IN ($2, $3)
             LIMIT 1`,
            [user.id, relPath, apiPath]
        );
        return res.rows.length > 0;
    }
    if (type === 'complaints') {
        const res = await db.query(
            `SELECT 1 FROM complaints
             WHERE "userId" = $1 AND ($2 = ANY("photoUrls") OR $3 = ANY("photoUrls"))
             LIMIT 1`,
            [user.id, relPath, apiPath]
        );
        return res.rows.length > 0;
    }
    if (type === 'emergencies') {
        const res = await db.query(
            `SELECT 1 FROM "emergencyReports"
             WHERE "userId" = $1 AND "photoUrl" IN ($2, $3)
             LIMIT 1`,
            [user.id, relPath, apiPath]
        );
        return res.rows.length > 0;
    }
    if (type === 'announcements') {
        const res = await db.query(
            `SELECT 1 FROM announcements
             WHERE "attachmentUrl" IN ($1, $2)
               AND ("expiresAt" IS NULL OR "expiresAt" > now())
               AND (
                   "targetPurok" IS NULL
                   OR cardinality("targetPurok") = 0
                   OR ($3::text IS NOT NULL AND $3::text = ANY("targetPurok"))
               )
             LIMIT 1`,
            [relPath, apiPath, user.purok || null]
        );
        return res.rows.length > 0;
    }
    return false;
}

router.get('/:type/:filename', authenticateToken, async (req, res) => {
    try {
        const { type, filename } = req.params;
        if (!ALLOWED_TYPES.has(type)) {
            return res.status(400).json({ error: 'Invalid upload category' });
        }

        const safeFilename = path.basename(filename);
        if (!safeFilename || safeFilename !== filename || safeFilename.includes('..')) {
            return res.status(400).json({ error: 'Invalid filename' });
        }

        const targetPath = path.resolve(UPLOADS_ROOT, type, safeFilename);
        const typeRoot = path.resolve(UPLOADS_ROOT, type);
        if (!targetPath.startsWith(typeRoot + path.sep)) {
            return res.status(400).json({ error: 'Invalid file path' });
        }

        if (RESTRICTED_TYPES.has(type) && req.user.role === 'resident') {
            const relPath = `uploads/${type}/${safeFilename}`;
            const apiPath = `/api/uploads/${type}/${safeFilename}`;
            const allowed = await residentCanAccessFile(req.user, type, relPath, apiPath);
            if (!allowed) {
                return res.status(403).json({ error: 'Access denied to this file' });
            }
        }

        if (!fs.existsSync(targetPath)) {
            return res.status(404).json({ error: 'File not found' });
        }

        return res.sendFile(targetPath);
    } catch (err) {
        console.error('Authenticated upload retrieval error:', err);
        return res.status(500).json({ error: 'Unable to retrieve file' });
    }
});

module.exports = router;
