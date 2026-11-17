const db = require('../config/db');

const ALLOWED_TABLES = {
    complaints: 'complaints',
    emergencyReports: '"emergencyReports"'
};

function createHourlyRateLimit({ tableKey, maxPerHour = 5, label = 'submissions' }) {
    const sqlTable = ALLOWED_TABLES[tableKey];
    if (!sqlTable) {
        throw new Error(`Unsupported rate-limit table key: ${tableKey}`);
    }

    return async (req, res, next) => {
        if (!req.user || !req.user.id) {
            return res.status(401).json({ error: 'Authentication required' });
        }

        try {
            const result = await db.query(
                `SELECT COUNT(*)::int AS count,
                        MIN("createdAt") AS oldest
                 FROM ${sqlTable}
                 WHERE "userId" = $1
                   AND "createdAt" > now() - interval '1 hour'`,
                [req.user.id]
            );
            const row = result.rows[0] || {};
            const recentCount = row.count || 0;

            if (recentCount >= maxPerHour) {
                let retryAfterSeconds = 60;
                if (row.oldest) {
                    const resetAt = new Date(row.oldest).getTime() + 3600 * 1000;
                    retryAfterSeconds = Math.max(1, Math.ceil((resetAt - Date.now()) / 1000));
                }
                res.setHeader('Retry-After', String(retryAfterSeconds));
                return res.status(429).json({
                    error: `Rate limit exceeded. Maximum ${maxPerHour} ${label} allowed per hour.`,
                    retryAfterSeconds
                });
            }

            return next();
        } catch (err) {
            console.error(`Rate limit check failed for ${tableKey}:`, err);
            return res.status(500).json({ error: 'Unable to verify submission limit' });
        }
    };
}

module.exports = {
    createHourlyRateLimit,
    complaintRateLimit: createHourlyRateLimit({
        tableKey: 'complaints',
        maxPerHour: 5,
        label: 'complaints'
    }),
    emergencyRateLimit: createHourlyRateLimit({
        tableKey: 'emergencyReports',
        maxPerHour: 5,
        label: 'emergency reports'
    })
};
