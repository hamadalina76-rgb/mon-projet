import 'package:flutter/foundation.dart';

import '../../config/runtime_config.dart';

String resolveMediaUrl(String? rawUrl, {String? fallbackBaseUrl}) {
  if (rawUrl == null) return '';
  final trimmed = rawUrl.trim();
  if (trimmed.isEmpty) return '';

  final configuredBase = (fallbackBaseUrl != null && fallbackBaseUrl.trim().isNotEmpty)
      ? fallbackBaseUrl.trim()
      : RuntimeConfig.apiBaseUrl.trim();

  final runtimeBase = configuredBase.isEmpty
      ? 'http://localhost:8080'
      : configuredBase;

  final baseUri = _normalizeBaseUri(runtimeBase);
  if (baseUri == null) return trimmed;

  if (trimmed.startsWith('/uploads/')) {
    return _join(baseUri, trimmed);
  }

  if (trimmed.startsWith('uploads/')) {
    return _join(baseUri, '/$trimmed');
  }

  final uploadsIndex = trimmed.toLowerCase().indexOf('/uploads/');
  if (uploadsIndex >= 0) {
    final uploadsPath = trimmed.substring(uploadsIndex);
    final parsed = Uri.tryParse(trimmed);
    if (parsed != null && _isLocalHost(parsed.host)) {
      return _join(baseUri, uploadsPath);
    }
  }

  return trimmed;
}

Uri? _normalizeBaseUri(String base) {
  final parsed = Uri.tryParse(base);
  if (parsed == null || parsed.host.isEmpty) return null;

  if (!kIsWeb && defaultTargetPlatform == TargetPlatform.android && _isLocalHost(parsed.host)) {
    final port = parsed.hasPort ? ':${parsed.port}' : '';
    return Uri.parse('${parsed.scheme}://10.0.2.2$port');
  }

  return parsed;
}

bool _isLocalHost(String host) {
  final value = host.toLowerCase();
  return value == 'localhost' || value == '127.0.0.1' || value == '0.0.0.0';
}

String _join(Uri baseUri, String path) {
  final base = '${baseUri.scheme}://${baseUri.host}${baseUri.hasPort ? ':${baseUri.port}' : ''}';
  return '$base$path';
}
