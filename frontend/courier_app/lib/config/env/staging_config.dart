import 'env_config.dart';

class StagingConfig extends EnvConfig {
  const StagingConfig()
      : super(
          apiBaseUrl: 'https://staging-api.speedline.com',
          wsUrl: 'wss://staging-api.speedline.com/ws',
          googleMapsApiKey: 'YOUR_STAGING_API_KEY',
        );
}
