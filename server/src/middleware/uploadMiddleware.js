const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const multer = require('multer');

const MAX_UPLOAD_BYTES = 5 * 1024 * 1024;
const ALLOWED_MIME_TYPES = new Set(['image/jpeg', 'image/jpg', 'image/png', 'image/webp']);
const ALLOWED_EXTENSIONS = new Set(['.jpg', '.jpeg', '.png', '.webp']);
const UPLOADS_ROOT = path.resolve(__dirname, '../../uploads');

function createUploadMiddleware(subDir) {
    const targetDir = path.join(UPLOADS_ROOT, subDir);
    fs.mkdirSync(targetDir, { recursive: true });

    const storage = multer.diskStorage({
        destination: (_req, _file, cb) => {
            fs.mkdirSync(targetDir, { recursive: true });
            cb(null, targetDir);
        },
        filename: (_req, file, cb) => {
            const rawExt = path.extname(file.originalname || '').toLowerCase();
            const safeExt = ALLOWED_EXTENSIONS.has(rawExt) ? rawExt : '.jpg';
            cb(null, `${Date.now()}-${crypto.randomUUID()}${safeExt}`);
        }
    });

    const fileFilter = (_req, file, cb) => {
        const mime = (file.mimetype || '').toLowerCase();
        const ext = path.extname(file.originalname || '').toLowerCase();
        if (!ALLOWED_MIME_TYPES.has(mime) || (ext && !ALLOWED_EXTENSIONS.has(ext))) {
            const err = new Error('Invalid file type. Only JPEG, PNG, and WebP images are allowed.');
            err.statusCode = 400;
            return cb(err);
        }
        return cb(null, true);
    };

    return multer({
        storage,
        fileFilter,
        limits: { fileSize: MAX_UPLOAD_BYTES }
    });
}

function getUploadRelativePath(subDir, filename) {
    return `uploads/${subDir}/${path.basename(filename)}`;
}

function handleUploadError(err, _req, res, next) {
    if (!err) return next();
    if (err instanceof multer.MulterError) {
        if (err.code === 'LIMIT_FILE_SIZE') {
            return res.status(400).json({ error: 'Uploaded file exceeds the 5 MB size limit.' });
        }
        return res.status(400).json({ error: `Upload error: ${err.message}` });
    }
    if (err.statusCode === 400) {
        return res.status(400).json({ error: err.message });
    }
    return next(err);
}

module.exports = {
    MAX_UPLOAD_BYTES,
    UPLOADS_ROOT,
    createUploadMiddleware,
    getUploadRelativePath,
    handleUploadError,
    uploadAvatar: createUploadMiddleware('avatars').single('avatar'),
    uploadClearance: createUploadMiddleware('clearances').single('purokClearance'),
    uploadComplaintPhotos: createUploadMiddleware('complaints').array('photos', 3),
    uploadEmergencyPhoto: createUploadMiddleware('emergencies').single('photo'),
    uploadAnnouncementAttachment: createUploadMiddleware('announcements').single('attachment'),
    uploadTemplateAssets: createUploadMiddleware('templates').fields([
        { name: 'seal', maxCount: 1 },
        { name: 'signature', maxCount: 1 }
    ])
};
