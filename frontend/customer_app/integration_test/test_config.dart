/// Test configuration for SpeedLine Customer App integration tests
const testConfig = TestConfig(
  apiBaseUrl: 'http://10.0.2.2:8080', // Android emulator -> host machine
  customerEmail: 'customer@speedline-test.com',
  customerPassword: 'TestCustomer123!',
);

class TestConfig {
  final String apiBaseUrl;
  final String customerEmail;
  final String customerPassword;

  const TestConfig({
    required this.apiBaseUrl,
    required this.customerEmail,
    required this.customerPassword,
  });
}
