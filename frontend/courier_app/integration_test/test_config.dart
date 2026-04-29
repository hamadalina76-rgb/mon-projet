/// Test configuration for SpeedLine Courier App integration tests
const testConfig = TestConfig(
  apiBaseUrl: 'http://10.0.2.2:8080',
  courierEmail: 'courier@speedline-test.com',
  courierPassword: 'TestCourier123!',
);

class TestConfig {
  final String apiBaseUrl;
  final String courierEmail;
  final String courierPassword;

  const TestConfig({
    required this.apiBaseUrl,
    required this.courierEmail,
    required this.courierPassword,
  });
}
