const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const express = require('express');
const db = require('../config/db');
const fees = require('../config/fees');
const { authenticateToken, requireRole } = require('../middleware/auth');
const { uploadClearance, getUploadRelativePath, UPLOADS_ROOT } = require('../middleware/uploadMiddleware');
const { isValidUuid, unlinkFilesSafe } = require('../utils/validators');

const router = express.Router();

const RESIDENT_COLUMNS = `
    "requestId", "userId", "documentType", "controlNumber", purpose, "deliveryMethod",
    fee, status, "paymentStatus", "paymentMethod", "paymentOrNumber", "rejectionReason",
    "businessName", "businessAddress", "businessType", "isWalkIn",
    "purokClearanceUrl", "purokClearanceVerified", "purokClearanceVerifiedBy",
    "purokClearanceVerifiedAt", "certificateUrl", "processedBy", "createdAt", "completedAt"
`;

const ALLOWED_STATUSES = new Set(['pending', 'processing', 'ready', 'completed', 'rejected']);
const ALLOWED_DELIVERY_METHODS = new Set(['pickup', 'email', 'both']);

function generateControlNumber(documentType) {
    const clean = (documentType || 'DOC').replace(/[^A-Za-z0-9]/g, '');
    const prefix = (clean.slice(0, 4) || 'DOC').toUpperCase();
    const rand3 = String(crypto.randomInt(100, 1000));
    return `${prefix}-${Date.now()}${rand3}`;
}

function listDocumentTypes(_req, res) {
    const list = Object.keys(fees).map((type) => ({
        documentType: type,
        fee: fees[type]
    }));
    return res.json(list);
}

router.get('/types', authenticateToken, listDocumentTypes);

router.get('/', authenticateToken, async (req, res) => {
    try {
        const statusFilter = typeof req.query.status === 'string' && req.query.status.trim()
            ? req.query.status.trim().toLowerCase()
            : null;

        if (statusFilter && !ALLOWED_STATUSES.has(statusFilter)) {
            return res.status(400).json({ error: 'Invalid status filter' });
        }

        let query;
        const params = [];

        if (req.user.role === 'resident') {
            query = `SELECT ${RESIDENT_COLUMNS} FROM "documentRequests" WHERE "userId" = $1`;
            params.push(req.user.id);
            if (statusFilter) {
                query += ' AND status = $2';
                params.push(statusFilter);
            }
        } else {
            query = 'SELECT * FROM "documentRequests"';
            if (statusFilter) {
                query += ' WHERE status = $1';
                params.push(statusFilter);
            }
        }

        query += ' ORDER BY "createdAt" DESC';
        const result = await db.query(query, params);
        return res.json(result.rows);
    } catch (err) {
        console.error('Error fetching document requests:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

router.get('/:id', authenticateToken, async (req, res) => {
    try {
        if (!isValidUuid(req.params.id)) {
            return res.status(400).json({ error: 'Invalid document request ID' });
        }
        const columns = req.user.role === 'resident' ? RESIDENT_COLUMNS : '*';
        const result = await db.query(
            `SELECT ${columns} FROM "documentRequests" WHERE "requestId" = $1`,
            [req.params.id]
        );
        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Document request not found' });
        }

        const record = result.rows[0];
        if (req.user.role === 'resident' && record.userId !== req.user.id) {
            return res.status(403).json({ error: 'Access denied' });
        }

        return res.json(record);
    } catch (err) {
        console.error('Error fetching document request detail:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

router.get('/:id/certificate', authenticateToken, async (req, res) => {
    try {
        if (!isValidUuid(req.params.id)) {
            return res.status(400).json({ error: 'Invalid document request ID' });
        }
        const result = await db.query(
            'SELECT "userId", status, "certificateUrl" FROM "documentRequests" WHERE "requestId" = $1',
            [req.params.id]
        );
        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Document request not found' });
        }

        const record = result.rows[0];
        if (req.user.role === 'resident' && record.userId !== req.user.id) {
            return res.status(403).json({ error: 'Access denied' });
        }

        if ((record.status !== 'ready' && record.status !== 'completed') || !record.certificateUrl) {
            return res.status(404).json({ error: 'Certificate PDF is not available yet' });
        }

        const safeFile = path.basename(record.certificateUrl);
        const certPath = path.resolve(UPLOADS_ROOT, 'certificates', safeFile);
        const certRoot = path.resolve(UPLOADS_ROOT, 'certificates');
        if (!certPath.startsWith(certRoot + path.sep) || !fs.existsSync(certPath)) {
            return res.status(404).json({ error: 'Generated certificate file not found on server' });
        }

        res.setHeader('Content-Type', 'application/pdf');
        return res.sendFile(certPath);
    } catch (err) {
        console.error('Error downloading certificate:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

router.post('/', authenticateToken, requireRole('resident'), uploadClearance, async (req, res) => {
    try {
        const body = req.body || {};
        const {
            documentType,
            purpose,
            deliveryMethod,
            businessName,
            businessAddress,
            businessType
        } = body;

        if (!documentType || !Object.prototype.hasOwnProperty.call(fees, documentType)) {
            unlinkFilesSafe(req);
            return res.status(400).json({ error: 'Valid documentType is required' });
        }

        const cleanPurpose = typeof purpose === 'string' ? purpose.trim() : '';
        if (!cleanPurpose) {
            unlinkFilesSafe(req);
            return res.status(400).json({ error: 'Purpose is required' });
        }

        const method = typeof deliveryMethod === 'string' && deliveryMethod.trim()
            ? deliveryMethod.trim().toLowerCase()
            : 'pickup';
        if (!ALLOWED_DELIVERY_METHODS.has(method)) {
            unlinkFilesSafe(req);
            return res.status(400).json({ error: 'deliveryMethod must be pickup, email, or both' });
        }

        const cleanBusinessName = typeof businessName === 'string' ? businessName.trim() : '';
        const cleanBusinessAddress = typeof businessAddress === 'string' ? businessAddress.trim() : '';
        const cleanBusinessType = typeof businessType === 'string' ? businessType.trim() : '';

        if (documentType === 'Business Clearance') {
            if (!cleanBusinessName || !cleanBusinessAddress || !cleanBusinessType) {
                unlinkFilesSafe(req);
                return res.status(400).json({
                    error: 'businessName, businessAddress, and businessType are required for Business Clearance'
                });
            }
        }

        if (!req.file) {
            return res.status(400).json({
                error: 'Purok Clearance photo is required for online document requests'
            });
        }

        const fee = fees[documentType];
        const clearanceUrl = getUploadRelativePath('clearances', req.file.filename);

        let inserted = null;
        for (let attempt = 0; attempt < 3; attempt++) {
            const controlNumber = generateControlNumber(documentType);
            try {
                const result = await db.query(
                    `INSERT INTO "documentRequests" (
                        "userId", "documentType", "controlNumber", purpose, "deliveryMethod",
                        fee, "businessName", "businessAddress", "businessType",
                        "isWalkIn", "purokClearanceUrl", "purokClearanceVerified"
                     ) VALUES (
                        $1, $2, $3, $4, $5,
                        $6, $7, $8, $9,
                        false, $10, false
                     )
                     RETURNING ${RESIDENT_COLUMNS}`,
                    [
                        req.user.id,
                        documentType,
                        controlNumber,
                        cleanPurpose,
                        method,
                        fee,
                        documentType === 'Business Clearance' ? cleanBusinessName : null,
                        documentType === 'Business Clearance' ? cleanBusinessAddress : null,
                        documentType === 'Business Clearance' ? cleanBusinessType : null,
                        clearanceUrl
                    ]
                );
                inserted = result.rows[0];
                break;
            } catch (insertErr) {
                if (insertErr.code === '23505' && attempt < 2) {
                    continue;
                }
                throw insertErr;
            }
        }

        return res.status(201).json(inserted);
    } catch (err) {
        unlinkFilesSafe(req);
        console.error('Error creating document request:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

router.patch('/:id', authenticateToken, requireRole('staff', 'admin'), async (req, res) => {
    try {
        if (!isValidUuid(req.params.id)) {
            return res.status(400).json({ error: 'Invalid document request ID' });
        }
        const { status, paymentStatus, paymentMethod, paymentOrNumber, rejectionReason, staffNotes } = req.body || {};
        const result = await db.query(
            `UPDATE "documentRequests"
             SET status = COALESCE($1, status),
                 "paymentStatus" = COALESCE($2, "paymentStatus"),
                 "paymentMethod" = COALESCE($3, "paymentMethod"),
                 "paymentOrNumber" = COALESCE($4, "paymentOrNumber"),
                 "rejectionReason" = COALESCE($5, "rejectionReason"),
                 "staffNotes" = COALESCE($6, "staffNotes"),
                 "processedBy" = $7
             WHERE "requestId" = $8
             RETURNING *`,
            [
                status || null,
                paymentStatus || null,
                paymentMethod || null,
                paymentOrNumber || null,
                rejectionReason || null,
                staffNotes || null,
                req.user.id,
                req.params.id
            ]
        );
        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Request not found' });
        }
        return res.json(result.rows[0]);
    } catch (err) {
        console.error('Error updating document request:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

router.patch('/:id/verify-clearance', authenticateToken, requireRole('staff', 'admin'), async (req, res) => {
    try {
        if (!isValidUuid(req.params.id)) {
            return res.status(400).json({ error: 'Invalid document request ID' });
        }
        const verified = Boolean((req.body || {}).verified);
        const result = await db.query(
            `UPDATE "documentRequests"
             SET "purokClearanceVerified" = $1,
                 "purokClearanceVerifiedBy" = $2,
                 "purokClearanceVerifiedAt" = now()
             WHERE "requestId" = $3
             RETURNING *`,
            [verified, req.user.id, req.params.id]
        );
        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Request not found' });
        }
        return res.json(result.rows[0]);
    } catch (err) {
        console.error('Error verifying clearance:', err);
        return res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router;
module.exports.listDocumentTypes = listDocumentTypes;
