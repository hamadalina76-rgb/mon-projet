import 'env_config.dart';

class ProdConfig extends EnvConfig {
  const ProdConfig()
      : super(
          apiBaseUrl: 'https://api.speedline.com',
          wsUrl: 'wss://api.speedline.com/ws',
          googleMapsApiKey: 'YOUR_PROD_API_KEY',
        );
}
