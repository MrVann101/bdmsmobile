const db = require('../config/db');

/**
 * Persists an immutable audit log entry for sensitive staff/admin actions.
 * Pass a pg transaction client when logging inside BEGIN ... COMMIT blocks.
 */
async function writeAuditLog(req, action, targetId = null, details = null, metadata = null, client = db) {
    const performedBy = req && req.user ? req.user.id : null;
    if (!performedBy || !action) {
        throw new Error('writeAuditLog requires an authenticated req.user.id and action');
    }

    const forwarded = req && req.headers ? req.headers['x-forwarded-for'] : null;
    const ipAddress = (req && req.ip) || (typeof forwarded === 'string' ? forwarded.split(',')[0].trim() : null);
    const userAgent = req && req.headers ? req.headers['user-agent'] || null : null;

    const result = await client.query(
        `INSERT INTO "auditLogs"
            ("performedBy", action, "targetId", details, "ipAddress", "userAgent", metadata)
         VALUES ($1, $2, $3, $4, $5, $6, $7)
         RETURNING *`,
        [performedBy, action, targetId, details, ipAddress, userAgent, metadata ? JSON.stringify(metadata) : null]
    );

    return result.rows[0];
}

module.exports = {
    writeAuditLog
};
