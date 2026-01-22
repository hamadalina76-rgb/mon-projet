import 'package:flutter/material.dart';

class SocialLoginButton extends StatelessWidget {
  final String provider;

  const SocialLoginButton({super.key, required this.provider});

  @override
  Widget build(BuildContext context) {
    return ElevatedButton(
      onPressed: () {},
      child: Text('Continue with \$provider'),
    );
  }
}
