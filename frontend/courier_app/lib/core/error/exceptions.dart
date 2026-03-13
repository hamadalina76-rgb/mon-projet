class ServerException implements Exception {
  final String message;
  
  ServerException(this.message);
}

class NetworkException implements Exception {
  final String message;
  
  NetworkException(this.message);
}

class CacheException implements Exception {
  final String message;
  
  CacheException(this.message);
}

/// Thrown when the API returns 400 with validation errors (e.g. invalid IBAN).
/// Use [userFriendlyMessage] to show a clear message to the user.
class ApiValidationException implements Exception {
  final int statusCode;
  final String message;
  final Map<String, dynamic> details;

  ApiValidationException({
    required this.statusCode,
    required this.message,
    this.details = const {},
  });

  /// Message suitable for the user (no status codes, no technical jargon).
  String get userFriendlyMessage => _toUserFriendlyMessage();

  String _toUserFriendlyMessage() {
    if (details.isEmpty) {
      return _friendlyFallback(message);
    }
    final labels = <String, String>{
      'bankIban': 'IBAN du compte',
      'bank_account_holder': 'Titulaire du compte',
      'vehicleNumber': 'Numéro d\'immatriculation',
      'vehicleModel': 'Modèle du véhicule',
      'vehicleColor': 'Couleur du véhicule',
      'identityNumber': 'Numéro d\'identité',
      'drivingLicenseNumber': 'Numéro du permis',
      'drivingLicenseExpiry': 'Date d\'expiration du permis',
    };
    final messages = <String>[];
    for (final e in details.entries) {
      final fieldLabel = labels[e.key] ?? e.key;
      final value = e.value?.toString() ?? '';
      if (value.toLowerCase().contains('iban')) {
        messages.add(
          '$fieldLabel : Le format de l\'IBAN est invalide. Utilisez un IBAN tunisien (ex. TN59 suivi de 22 chiffres, sans espace).',
        );
      } else {
        messages.add('$fieldLabel : $value');
      }
    }
    return messages.join('\n');
  }

  static String _friendlyFallback(String msg) {
    if (msg.toLowerCase().contains('validation') || msg.toLowerCase().contains('erreur')) {
      return 'Vérifiez les informations saisies et corrigez les champs indiqués.';
    }
    return msg;
  }

  @override
  String toString() => message;
}
