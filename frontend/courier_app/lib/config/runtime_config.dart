import 'dart:convert';

import 'package:flutter/services.dart' show rootBundle;

class RuntimeConfig {
  static Map<String, dynamic> _config = {};

  static Future<void> load() async {
    try {
      final raw = await rootBundle.loadString('assets/config/config.json');
      _config = jsonDecode(raw) as Map<String, dynamic>;
    } catch (_) {
      _config = {};
    }
  }

  static String get env => (_config['env'] as String?) ?? 'dev';
  static String get apiBaseUrl =>
      (_config['apiBaseUrl'] as String?) ?? 'https://api-gateway-392205979525.europe-west1.run.app';
  static String get wsUrl =>
      (_config['wsUrl'] as String?) ?? 'wss://api-gateway-392205979525.europe-west1.run.app';
  static int get apiTimeoutMs => (_config['apiTimeoutMs'] as int?) ?? 30000;
}
