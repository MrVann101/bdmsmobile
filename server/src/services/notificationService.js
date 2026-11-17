const db = require('../config/db');
const emailService = require('./emailService');

let ioInstance = null;

function setIo(io) {
    ioInstance = io;
}

function getIo() {
    return ioInstance;
}

function emitToUser(userId, eventName, payload) {
    if (ioInstance && userId) {
        ioInstance.to(`notifications:${userId}`).emit(eventName, payload);
    }
}

async function pushInApp({
    userId,
    title,
    message,
    type,
    relatedId = null,
    deliveryStatus = 'sent',
    client = db
}) {
    const result = await client.query(
        `INSERT INTO notifications ("userId", title, message, type, "relatedId", "deliveryStatus")
         VALUES ($1, $2, $3, $4, $5, $6)
         RETURNING *`,
        [userId, title, message, type, relatedId, deliveryStatus]
    );
    const notification = result.rows[0];
    emitToUser(userId, 'notification:new', notification);
    return notification;
}

async function sendNotificationEmail({
    notificationId,
    toEmail,
    subject,
    html,
    text,
    attachments = null
}) {
    const result = await emailService.sendEmail({
        to: toEmail,
        subject,
        html,
        text,
        attachments
    });

    const nextStatus = result.success ? 'sent' : 'failed';
    const failureReason = result.success ? null : result.error;

    const updated = await db.query(
        `UPDATE notifications
         SET "deliveryStatus" = $1,
             "deliveryAttempts" = "deliveryAttempts" + 1,
             "lastAttemptAt" = now(),
             "failureReason" = $2
         WHERE "notificationId" = $3
         RETURNING *`,
        [nextStatus, failureReason, notificationId]
    );

    return {
        success: result.success,
        error: result.error || null,
        notification: updated.rows[0] || null
    };
}

async function notifyUser({
    userId,
    email = null,
    title,
    message,
    type,
    relatedId = null,
    sendEmailFlag = true,
    subject = null,
    html = null,
    text = null,
    attachments = null,
    client = db
}) {
    const shouldEmail = Boolean(sendEmailFlag && email);
    const notification = await pushInApp({
        userId,
        title,
        message,
        type,
        relatedId,
        deliveryStatus: shouldEmail ? 'pending' : 'sent',
        client
    });

    if (shouldEmail && client === db) {
        return sendNotificationEmail({
            notificationId: notification.notificationId,
            toEmail: email,
            subject: subject || title,
            text: text || message,
            html: html || `<p>${message}</p>`,
            attachments
        });
    }

    return { success: true, notification };
}

/**
 * Broadcasts a notification to all active residents in a Purok (or barangay-wide)
 * using a FIFO queue to preserve ordered delivery.
 */
async function broadcastToPurok({
    purok = null,
    title,
    message,
    type = 'announcement',
    relatedId = null,
    sendEmailFlag = true
}) {
    let query = 'SELECT id, email FROM profiles WHERE role = $1 AND "isActive" = true';
    const params = ['resident'];

    if (Array.isArray(purok) && purok.length > 0) {
        query += ' AND purok = ANY($2)';
        params.push(purok);
    } else if (typeof purok === 'string' && purok.trim().length > 0) {
        query += ' AND purok = $2';
        params.push(purok.trim());
    }

    const recipientsRes = await db.query(query, params);
    const fifoQueue = [...recipientsRes.rows];
    const results = [];

    while (fifoQueue.length > 0) {
        const recipient = fifoQueue.shift();
        const outcome = await notifyUser({
            userId: recipient.id,
            email: recipient.email,
            title,
            message,
            type,
            relatedId,
            sendEmailFlag
        });
        results.push(outcome);
    }

    return results;
}

module.exports = {
    setIo,
    getIo,
    emitToUser,
    pushInApp,
    sendNotificationEmail,
    notifyUser,
    broadcastToPurok
};
