import 'env_config.dart';

class DevConfig extends EnvConfig {
  const DevConfig()
      : super(
          apiBaseUrl: 'http://localhost:8080',
          wsUrl: 'ws://localhost:8080/ws',
          googleMapsApiKey: 'YOUR_DEV_API_KEY',
        );
}
