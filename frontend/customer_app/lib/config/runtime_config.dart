import 'dart:convert';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter/foundation.dart';

import 'package:flutter/services.dart' show rootBundle;

class RuntimeConfig {
  static Map<String, dynamic> _config = {};
  static String? _resolvedMode;
  static String? _resolvedApiBaseUrl;

  static const String _defaultPhysicalApiBaseUrl = 'http://192.168.1.171:8080';
  static const String _defaultEmulatorApiBaseUrl = 'http://10.0.0.2:8080';
  static const String _defaultCloudApiBaseUrl =
      'https://api-gateway-392205979525.europe-west1.run.app';

  static const String _defaultPhysicalWsUrl = 'ws://192.168.1.171:8080';
  static const String _defaultEmulatorWsUrl = 'ws://10.0.0.2:8080';
  static const String _defaultCloudWsUrl =
      'wss://api-gateway-392205979525.europe-west1.run.app';

  static Future<void> load() async {
    try {
      final raw = await rootBundle.loadString('assets/config/config.json');
      _config = jsonDecode(raw) as Map<String, dynamic>;
    } catch (_) {
      _config = {};
    }

    _resolvedMode = await _resolveMode();
    _resolvedApiBaseUrl = await _resolveApiBaseUrlForMode(_resolvedMode!);
    debugPrint(
      '[RuntimeConfig] mode=${_resolvedMode!.toUpperCase()} api=$_resolvedApiBaseUrl',
    );
  }

  static String get env => (_config['env'] as String?) ?? 'dev';

  /// Supported modes: auto, physical, emulator, cloud.
  static String get apiMode {
    final value = (_config['apiMode'] as String?)?.trim().toLowerCase();
    if (value == null || value.isEmpty) return 'auto';
    switch (value) {
      case 'auto':
      case 'physical':
      case 'emulator':
      case 'cloud':
        return value;
      default:
        return 'auto';
    }
  }

  static String get apiBaseUrl =>
      _resolvedApiBaseUrl ?? _defaultApiUrlForMode(_fallbackMode());

  static String get wsUrl {
    final configuredLegacy = (_config['wsUrl'] as String?)?.trim();
    if (configuredLegacy != null && configuredLegacy.isNotEmpty) {
      return configuredLegacy;
    }
    final mode = _resolvedMode ?? _fallbackMode();
    return switch (mode) {
      'physical' => _readString('wsUrlPhysical') ?? _defaultPhysicalWsUrl,
      'emulator' => _readString('wsUrlEmulator') ?? _defaultEmulatorWsUrl,
      _ => _readString('wsUrlCloud') ?? _defaultCloudWsUrl,
    };
  }

  static int get apiTimeoutMs {
    final value = _config['apiTimeoutMs'];
    if (value is int) return value;
    if (value is String) {
      final parsed = int.tryParse(value);
      if (parsed != null && parsed > 0) return parsed;
    }
    return 30000;
  }

  static String _fallbackMode() {
    if (apiMode != 'auto') return apiMode;
    // When runtime detection cannot run (e.g., no connected device context),
    // default to cloud to keep APK builds usable.
    return 'cloud';
  }

  static Future<String> _resolveMode() async {
    if (apiMode != 'auto') return apiMode;

    if (kReleaseMode || _isProdEnv || kIsWeb) {
      return 'cloud';
    }

    if (defaultTargetPlatform == TargetPlatform.android) {
      final detected = await _detectAndroidMode();
      if (detected != null) return detected;
      return 'cloud';
    }

    return 'cloud';
  }

  static Future<String?> _detectAndroidMode() async {
    try {
      final androidInfo = await DeviceInfoPlugin().androidInfo;
      return androidInfo.isPhysicalDevice ? 'physical' : 'emulator';
    } catch (_) {
      return null;
    }
  }

  static Future<String> _resolveApiBaseUrlForMode(String mode) async {
    return _defaultApiUrlForMode(mode);
  }

  static bool get _isProdEnv {
    final value = env.trim().toLowerCase();
    return value == 'prod' || value == 'production';
  }

  static String _defaultApiUrlForMode(String mode) {
    switch (mode) {
      case 'physical':
        return _readString('apiBaseUrlPhysical') ??
            _readString('apiBaseUrl') ??
            _defaultPhysicalApiBaseUrl;
      case 'emulator':
        return _readString('apiBaseUrlEmulator') ??
            _readString('apiBaseUrl') ??
            _defaultEmulatorApiBaseUrl;
      case 'cloud':
        return _readString('apiBaseUrlCloud') ??
            _readString('apiBaseUrl') ??
            _defaultCloudApiBaseUrl;
      default:
        return _readString('apiBaseUrl') ?? _defaultPhysicalApiBaseUrl;
    }
  }

  static String? _readString(String key) {
    final value = _config[key];
    if (value is! String) return null;
    final trimmed = value.trim();
    return trimmed.isEmpty ? null : trimmed;
  }

}
