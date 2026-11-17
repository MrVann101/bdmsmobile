const { Resend } = require('resend');
require('dotenv').config();

let resendClient = null;

function getResendClient() {
    const apiKey = process.env.RESEND_API_KEY;
    if (!apiKey || !apiKey.trim()) {
        return null;
    }
    if (!resendClient) {
        resendClient = new Resend(apiKey.trim());
    }
    return resendClient;
}

async function sendEmail({ to, subject, html, text, attachments }) {
    const from = process.env.RESEND_FROM_EMAIL;
    const client = getResendClient();

    if (!client || !from || !from.trim()) {
        return {
            success: false,
            error: 'RESEND_API_KEY or RESEND_FROM_EMAIL is not configured in server/.env'
        };
    }

    if (!to) {
        return {
            success: false,
            error: 'Recipient email address is required'
        };
    }

    try {
        const payload = {
            from: from.trim(),
            to: Array.isArray(to) ? to : [to],
            subject,
            text: text || undefined,
            html: html || undefined,
            attachments: attachments || undefined
        };

        const response = await client.emails.send(payload);
        if (response && response.error) {
            return {
                success: false,
                error: response.error.message || 'Resend API returned an error'
            };
        }

        return {
            success: true,
            id: response && response.data ? response.data.id : null
        };
    } catch (err) {
        return {
            success: false,
            error: err && err.message ? err.message : 'Unexpected Resend send failure'
        };
    }
}

module.exports = {
    sendEmail
};
