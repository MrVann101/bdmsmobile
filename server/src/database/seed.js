const db = require('../config/db');
const bcrypt = require('bcrypt');

async function seed() {
    try {
        // Seed Puroks
        const puroks = [
            ['Purok 1', 'Centro commercial and plaza area'],
            ['Purok 2', 'Riverside residential zone'],
            ['Purok 3', 'Silangan interior and school zone'],
            ['Purok 4', 'Bukid agricultural and upland area'],
            ['Purok 5', 'Tabing-Dagat coastal community']
        ];

        for (const p of puroks) {
            await db.query(
                `INSERT INTO puroks (name, description) VALUES ($1, $2) ON CONFLICT (name) DO NOTHING`,
                p
            );
        }

        const salt = await bcrypt.genSalt(10);

        // Seed Admin Account
        const adminHash = await bcrypt.hash('Admin@123', salt);
        await db.query(
            `INSERT INTO profiles (username, "fullName", email, "passwordHash", "mobileNumber", purok, role)
             VALUES ($1, $2, $3, $4, $5, $6, $7)
             ON CONFLICT (email) DO NOTHING`,
            ['admin_user', 'Barangay Admin', 'admin@barangay.gov.ph', adminHash, '+639170000000', 'Purok 1', 'admin']
        );

        // Seed Staff Account
        const staffHash = await bcrypt.hash('Staff@123', salt);
        await db.query(
            `INSERT INTO profiles (username, "fullName", email, "passwordHash", "mobileNumber", purok, role)
             VALUES ($1, $2, $3, $4, $5, $6, $7)
             ON CONFLICT (email) DO NOTHING`,
            ['staff_maria', 'Desk Clerk Maria', 'staff@barangay.gov.ph', staffHash, '+639171111111', 'Purok 1', 'staff']
        );

        // Seed Resident Account
        const residentHash = await bcrypt.hash('Resident@123', salt);
        await db.query(
            `INSERT INTO profiles (username, "fullName", email, "passwordHash", "mobileNumber", purok, role)
             VALUES ($1, $2, $3, $4, $5, $6, $7)
             ON CONFLICT (email) DO NOTHING`,
            ['juan_cruz', 'Juan dela Cruz', 'juan.delacruz@email.com', residentHash, '+639171234567', 'Purok 3', 'resident']
        );

        console.log('Database seeding completed successfully.');
        process.exit(0);
    } catch (err) {
        console.error('Seeding failed:', err);
        process.exit(1);
    }
}

seed();
