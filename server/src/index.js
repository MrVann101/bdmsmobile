const express = require('express');
const cors = require('cors');
const http = require('http');
const jwt = require('jsonwebtoken');
const { Server } = require('socket.io');
require('dotenv').config();

const db = require('./config/db');
const { getJwtSecret } = require('./middleware/auth');
const { isValidUuid } = require('./utils/validators');
const { handleUploadError } = require('./middleware/uploadMiddleware');
const notificationService = require('./services/notificationService');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
    cors: {
        origin: '*',
        methods: ['GET', 'POST', 'PATCH', 'PUT', 'DELETE']
    }
});

notificationService.setIo(io);

app.use(cors());
app.use(express.json());

// Health check (verifies both HTTP server and PostgreSQL connectivity)
app.get('/api/health', async (_req, res) => {
    try {
        await db.query('SELECT 1');
        return res.json({ status: 'healthy', database: 'connected', timestamp: new Date() });
    } catch (err) {
        console.error('Health check DB error:', err);
        return res.status(503).json({ status: 'unhealthy', database: 'disconnected', timestamp: new Date() });
    }
});

// Routes
const documentsRouter = require('./routes/documents');
const { authenticateToken } = require('./middleware/auth');

app.use('/api/auth', require('./routes/auth'));
app.use('/api/profiles', require('./routes/profiles'));
app.use('/api/puroks', require('./routes/puroks'));
app.get('/api/document-types', authenticateToken, documentsRouter.listDocumentTypes);
app.use('/api/document-requests', documentsRouter);
app.use('/api/complaints', require('./routes/complaints'));
app.use('/api/announcements', require('./routes/announcements'));
app.use('/api/emergency-reports', require('./routes/emergencies'));
app.use('/api/notifications', require('./routes/notifications'));
app.use('/api/admin', require('./routes/admin'));
app.use('/api/uploads', require('./routes/uploads'));

// Upload / generic error handler
app.use(handleUploadError);

// Socket.io connection handling with JWT + active profile verification
io.use(async (socket, next) => {
    const token =
        (socket.handshake.auth && socket.handshake.auth.token) ||
        (socket.handshake.query && socket.handshake.query.token);

    if (!token) {
        return next(new Error('Authentication token required'));
    }

    try {
        const decoded = jwt.verify(token, getJwtSecret());
        if (!decoded || !isValidUuid(decoded.id)) {
            return next(new Error('Invalid token claims'));
        }
        const profileRes = await db.query(
            'SELECT id, role, purok, "isActive" FROM profiles WHERE id = $1',
            [decoded.id]
        );
        if (profileRes.rows.length === 0 || !profileRes.rows[0].isActive) {
            return next(new Error('Account inactive or missing'));
        }
        socket.user = profileRes.rows[0];
        return next();
    } catch (err) {
        return next(new Error('Invalid or expired token'));
    }
});

io.on('connection', (socket) => {
    if (socket.user && socket.user.id) {
        socket.join(`notifications:${socket.user.id}`);
    }

    socket.on('disconnect', () => {
        // Room membership is cleaned up automatically by Socket.io
    });
});

const PORT = process.env.PORT || 5000;
if (require.main === module) {
    server.listen(PORT, () => {
        console.log(`BDMS Backend Server running on port ${PORT}`);
    });
}

module.exports = { app, server, io };
