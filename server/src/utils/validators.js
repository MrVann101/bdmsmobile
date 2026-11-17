const fs = require('fs');

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function isValidUuid(value) {
    return typeof value === 'string' && UUID_REGEX.test(value.trim());
}

function isValidIsoDate(value, { allowFuture = false } = {}) {
    if (typeof value !== 'string') return false;
    const trimmed = value.trim();
    if (!/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) return false;

    const [yStr, mStr, dStr] = trimmed.split('-');
    const year = Number(yStr);
    const month = Number(mStr);
    const day = Number(dStr);
    if (month < 1 || month > 12 || day < 1 || day > 31) return false;

    const date = new Date(Date.UTC(year, month - 1, day));
    if (
        date.getUTCFullYear() !== year ||
        date.getUTCMonth() !== month - 1 ||
        date.getUTCDate() !== day
    ) {
        return false;
    }

    if (!allowFuture) {
        const now = new Date();
        const todayUtc = Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate());
        if (date.getTime() > todayUtc) {
            return false;
        }
    }
    return true;
}

function unlinkFilesSafe(req) {
    const files = [];
    if (req && req.file) files.push(req.file);
    if (req && Array.isArray(req.files)) files.push(...req.files);
    for (const file of files) {
        if (file && file.path) {
            fs.promises.unlink(file.path).catch(() => {});
        }
    }
}

module.exports = {
    isValidUuid,
    isValidIsoDate,
    unlinkFilesSafe
};
