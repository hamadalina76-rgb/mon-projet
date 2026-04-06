import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../config/dependency_injection/injection.dart';
import '../../../config/routes/route_names.dart';
import '../../auth/presentation/providers/auth_state.dart';
import '../cart_providers.dart';

class CartFab extends ConsumerStatefulWidget {
  const CartFab({super.key});

  @override
  ConsumerState<CartFab> createState() => _CartFabState();
}

class _CartFabState extends ConsumerState<CartFab> with WidgetsBindingObserver {
  bool _initialLoginSyncDone = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_initialLoginSyncDone) return;
      _initialLoginSyncDone = true;
      final authState = ref.read(authNotifierProvider);
      final isAuthenticated = authState.maybeWhen(
        authenticated: (_) => true,
        orElse: () => false,
      );
      if (isAuthenticated) {
        ref.read(cartNotifierProvider.notifier).syncFromServerOnLogin();
      }
    });
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      ref.read(cartNotifierProvider.notifier).retryPendingSyncIfNeeded();
    }
  }

  @override
  Widget build(BuildContext context) {
    ref.listen<AuthState>(authNotifierProvider, (previous, next) {
      final wasAuthenticated =
          previous?.maybeWhen(
            authenticated: (_) => true,
            orElse: () => false,
          ) ??
          false;

      final isAuthenticated = next.maybeWhen(
        authenticated: (_) => true,
        orElse: () => false,
      );

      if (!wasAuthenticated && isAuthenticated) {
        ref.read(cartNotifierProvider.notifier).syncFromServerOnLogin();
      }
    });

    final itemCount = ref.watch(cartItemCountProvider);

    return FloatingActionButton(
      heroTag: 'global-cart-fab',
      onPressed: () => context.push(RouteNames.cart),
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          const Icon(Icons.shopping_bag_outlined),
          Positioned(
            right: -10,
            top: -10,
            child: AnimatedSwitcher(
              duration: const Duration(milliseconds: 220),
              switchInCurve: Curves.easeOutBack,
              switchOutCurve: Curves.easeIn,
              transitionBuilder: (child, animation) {
                return ScaleTransition(scale: animation, child: child);
              },
              child: itemCount <= 0
                  ? const SizedBox(key: ValueKey<String>('empty-badge'))
                  : Container(
                      key: ValueKey<int>(itemCount),
                      padding: const EdgeInsets.symmetric(
                        horizontal: 6,
                        vertical: 3,
                      ),
                      decoration: const BoxDecoration(
                        color: Colors.red,
                        shape: BoxShape.rectangle,
                        borderRadius: BorderRadius.all(Radius.circular(12)),
                      ),
                      constraints: const BoxConstraints(minWidth: 22),
                      child: Text(
                        '$itemCount',
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.w700,
                          fontSize: 12,
                        ),
                      ),
                    ),
            ),
          ),
        ],
      ),
    );
  }
}
