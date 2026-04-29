export const TEST_ACCOUNTS = {
  admin: {
    email: process.env.ADMIN_EMAIL || 'admin@speedline-test.com',
    password: process.env.ADMIN_PASSWORD || 'TestAdmin123!',
  },
  partner: {
    email: process.env.PARTNER_EMAIL || 'partner@speedline-test.com',
    password: process.env.PARTNER_PASSWORD || 'TestPartner123!',
  },
  customer: {
    email: process.env.CUSTOMER_EMAIL || 'customer@speedline-test.com',
    password: process.env.CUSTOMER_PASSWORD || 'TestCustomer123!',
  },
  courier: {
    email: process.env.COURIER_EMAIL || 'courier@speedline-test.com',
    password: process.env.COURIER_PASSWORD || 'TestCourier123!',
  },
};

export const TEST_ORDER = {
  items: [
    {
      productId: 'test-product-1',
      name: 'Test Pizza Margherita',
      quantity: 2,
      price: 12.5,
    },
    {
      productId: 'test-product-2',
      name: 'Test Coca Cola',
      quantity: 1,
      price: 3.0,
    },
  ],
  deliveryAddress: {
    street: '10 Rue Test',
    city: 'Nabeul',
    postalCode: '8000',
    latitude: 36.4513,
    longitude: 10.7357,
  },
  paymentMethod: 'CASH',
};

export const TEST_PARTNER = {
  name: 'Test Restaurant Nabeul',
  type: 'RESTAURANT',
  address: {
    street: '5 Avenue Habib Bourguiba',
    city: 'Nabeul',
    postalCode: '8000',
    latitude: 36.4561,
    longitude: 10.7376,
  },
  phone: '+21612345678',
  email: 'restaurant@speedline-test.com',
};

export const TEST_PRODUCT = {
  name: 'Test Pizza Margherita',
  description: 'Classic pizza with tomato and mozzarella',
  price: 12.5,
  category: 'Pizza',
  preparationTime: 15,
  available: true,
};

export const TEST_COURIER = {
  firstName: 'Test',
  lastName: 'Courier',
  phone: '+21698765432',
  vehicleType: 'MOTORCYCLE',
  latitude: 36.4530,
  longitude: 10.7360,
};
