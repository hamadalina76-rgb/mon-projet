/// Request pour la connexion sociale (Google/Facebook)
class SocialLoginRequest {
  final String accessToken;
  final SocialProvider provider;
  final String role;
  final String? deviceInfo;

  SocialLoginRequest({
    required this.accessToken,
    required this.provider,
    this.role = 'CUSTOMER',
    this.deviceInfo,
  });

  Map<String, dynamic> toJson() => {
    'accessToken': accessToken,
    'provider': provider.name,
    'role': role,
    if (deviceInfo != null) 'deviceInfo': deviceInfo,
  };
}

/// Fournisseurs de connexion sociale supportés
enum SocialProvider {
  GOOGLE,
  FACEBOOK;
  
  String get name {
    switch (this) {
      case SocialProvider.GOOGLE:
        return 'GOOGLE';
      case SocialProvider.FACEBOOK:
        return 'FACEBOOK';
    }
  }
}
