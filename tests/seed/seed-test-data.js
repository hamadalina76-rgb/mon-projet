/**
 * Database Seeder for SpeedLine Test Robot
 *
 * Seeds test accounts across all microservice databases.
 * Run this before executing tests against a fresh environment.
 *
 * Usage:
 *   cd tests/seed
 *   npm install
 *   node seed-test-data.js
 */

const { Client } = require('pg');
const bcrypt = require('bcryptjs');
const crypto = require('crypto');

// Load .env if available
try { require('dotenv').config({ path: '../playwright/.env' }); } catch (e) { /* no dotenv */ }

const DB_CONFIG = {
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '5432'),
  user: process.env.DB_USER || 'speedline',
  password: process.env.DB_PASSWORD || 'speedline',
};

// All test accounts use the same base password pattern
const PASSWORDS = {
  admin: 'TestAdmin123!',
  partner: 'TestPartner123!',
  customer: 'TestCustomer123!',
  courier: 'TestCourier123!',
};

const TEST_USERS = [
  {
    email: 'admin@speedline-test.com',
    password: PASSWORDS.admin,
    firstName: 'Test',
    lastName: 'Admin',
    role: 'SUPER_ADMIN',
    phone: '+21611111111',
  },
  {
    email: 'partner@speedline-test.com',
    password: PASSWORDS.partner,
    firstName: 'Test',
    lastName: 'Partner',
    role: 'PARTNER',
    phone: '+21622222222',
  },
  {
    email: 'customer@speedline-test.com',
    password: PASSWORDS.customer,
    firstName: 'Test',
    lastName: 'Customer',
    role: 'CUSTOMER',
    phone: '+21633333333',
  },
  {
    email: 'courier@speedline-test.com',
    password: PASSWORDS.courier,
    firstName: 'Test',
    lastName: 'Courier',
    role: 'COURIER',
    phone: '+21644444444',
  },
];

async function hashPassword(plaintext) {
  const salt = await bcrypt.genSalt(10);
  return bcrypt.hash(plaintext, salt);
}

async function seedAuthDB() {
  const client = new Client({
    ...DB_CONFIG,
    database: process.env.AUTH_DB_NAME || 'auth_db',
  });

  try {
    await client.connect();
    console.log('Connected to auth_db');

    for (const user of TEST_USERS) {
      const id = crypto.randomUUID();
      const passwordHash = await hashPassword(user.password);

      await client.query(`
        INSERT INTO users (email, password, first_name, last_name, role, phone_number, status, is_email_verified, created_at, updated_at)
        VALUES ($1, $2, $3, $4, $5, $6, 'ACTIVE', true, NOW(), NOW())
        ON CONFLICT (email) DO UPDATE SET
          password = $2,
          status = 'ACTIVE',
          is_email_verified = true,
          updated_at = NOW()
      `, [user.email, passwordHash, user.firstName, user.lastName, user.role, user.phone]);

      console.log(`  Seeded user: ${user.email} (${user.role})`);
    }
  } catch (err) {
    console.error('Error seeding auth_db:', err.message);
    console.error('  Hint: Make sure the auth_db database exists and is accessible');
  } finally {
    await client.end();
  }
}

async function seedPartnerDB() {
  const client = new Client({
    ...DB_CONFIG,
    database: process.env.PARTNER_DB_NAME || 'partner_db',
  });

  try {
    await client.connect();
    console.log('Connected to partner_db');

    // Check if partner already exists
    const existing = await client.query(
      "SELECT id FROM partners WHERE email = 'restaurant@speedline-test.com'"
    );

    let partnerId;
    if (existing.rows.length > 0) {
      partnerId = existing.rows[0].id;
      console.log('  Test partner already exists, updating...');
      await client.query(`
        UPDATE partners SET status = 'ACTIVE', updated_at = NOW()
        WHERE id = $1
      `, [partnerId]);
    } else {
      partnerId = crypto.randomUUID();
      await client.query(`
        INSERT INTO partners (id, name, type, status, email, phone, address, city, latitude, longitude, commission_rate, created_at, updated_at)
        VALUES ($1, 'Test Restaurant Nabeul', 'RESTAURANT', 'ACTIVE', 'restaurant@speedline-test.com', '+21612345678', '5 Avenue Habib Bourguiba', 'Nabeul', 36.4561, 10.7376, 15.0, NOW(), NOW())
      `, [partnerId]);
      console.log('  Seeded test partner');
    }

    // Seed test products
    const products = [
      { name: 'Test Pizza Margherita', description: 'Classic pizza with tomato and mozzarella', price: 12.50, prepTime: 15 },
      { name: 'Test Coca Cola 33cl', description: 'Classic soft drink', price: 3.00, prepTime: 1 },
      { name: 'Test Salade Caesar', description: 'Fresh caesar salad', price: 9.50, prepTime: 10 },
    ];

    for (const product of products) {
      const productId = crypto.randomUUID();
      await client.query(`
        INSERT INTO products (id, partner_id, name, description, price, status, available, preparation_time, created_at, updated_at)
        VALUES ($1, $2, $3, $4, $5, 'ACTIVE', true, $6, NOW(), NOW())
        ON CONFLICT DO NOTHING
      `, [productId, partnerId, product.name, product.description, product.price, product.prepTime]);
    }
    console.log(`  Seeded ${products.length} test products`);

  } catch (err) {
    console.error('Error seeding partner_db:', err.message);
  } finally {
    await client.end();
  }
}

async function seedUserDB() {
  const client = new Client({
    ...DB_CONFIG,
    database: process.env.USER_DB_NAME || 'user_db',
  });

  try {
    await client.connect();
    console.log('Connected to user_db');

    // Seed customer
    const customerId = crypto.randomUUID();
    await client.query(`
      INSERT INTO customers (id, email, first_name, last_name, phone, status, created_at, updated_at)
      VALUES ($1, 'customer@speedline-test.com', 'Test', 'Customer', '+21633333333', 'ACTIVE', NOW(), NOW())
      ON CONFLICT (email) DO UPDATE SET status = 'ACTIVE', updated_at = NOW()
    `, [customerId]);
    console.log('  Seeded test customer');

    // Seed courier
    const courierId = crypto.randomUUID();
    await client.query(`
      INSERT INTO couriers (id, email, first_name, last_name, phone, vehicle_type, status, available, documents_verified, created_at, updated_at)
      VALUES ($1, 'courier@speedline-test.com', 'Test', 'Courier', '+21644444444', 'MOTORCYCLE', 'ACTIVE', true, true, NOW(), NOW())
      ON CONFLICT (email) DO UPDATE SET status = 'ACTIVE', available = true, documents_verified = true, updated_at = NOW()
    `, [courierId]);
    console.log('  Seeded test courier');

    // Seed admin
    const adminId = crypto.randomUUID();
    await client.query(`
      INSERT INTO admins (id, email, first_name, last_name, phone, status, created_at, updated_at)
      VALUES ($1, 'admin@speedline-test.com', 'Test', 'Admin', '+21611111111', 'ACTIVE', NOW(), NOW())
      ON CONFLICT (email) DO UPDATE SET status = 'ACTIVE', updated_at = NOW()
    `, [adminId]);
    console.log('  Seeded test admin');

  } catch (err) {
    console.error('Error seeding user_db:', err.message);
  } finally {
    await client.end();
  }
}

async function seedLocationDB() {
  const client = new Client({
    ...DB_CONFIG,
    database: process.env.LOCATION_DB_NAME || 'location_db',
  });

  try {
    await client.connect();
    console.log('Connected to location_db');

    const zoneId = crypto.randomUUID();
    await client.query(`
      INSERT INTO adm_zones (id, name, city, status, radius_km, center_lat, center_lng, created_at, updated_at)
      VALUES ($1, 'Zone Nabeul Centre', 'Nabeul', 'ACTIVE', 10.0, 36.4513, 10.7357, NOW(), NOW())
      ON CONFLICT DO NOTHING
    `, [zoneId]);
    console.log('  Seeded test zone');

  } catch (err) {
    console.error('Error seeding location_db:', err.message);
    console.error('  (This is OK if location_db uses a different schema)');
  } finally {
    await client.end();
  }
}

async function main() {
  console.log('=== SpeedLine Test Data Seeder ===\n');
  console.log(`Database host: ${DB_CONFIG.host}:${DB_CONFIG.port}`);
  console.log(`Database user: ${DB_CONFIG.user}\n`);

  await seedAuthDB();
  await seedPartnerDB();
  await seedUserDB();
  await seedLocationDB();

  console.log('\n=== Seeding complete ===');
  console.log('\nTest accounts:');
  console.log(`  Admin:    admin@speedline-test.com / ${PASSWORDS.admin}`);
  console.log(`  Partner:  partner@speedline-test.com / ${PASSWORDS.partner}`);
  console.log(`  Customer: customer@speedline-test.com / ${PASSWORDS.customer}`);
  console.log(`  Courier:  courier@speedline-test.com / ${PASSWORDS.courier}`);
}

main().catch(console.error);
