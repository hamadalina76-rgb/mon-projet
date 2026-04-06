import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../config/dependency_injection/injection.dart';
import 'data/repositories/cart_repository.dart';
import 'domain/cart_notifier.dart';

export 'data/models/cart_item_model.dart';
export 'data/repositories/cart_repository.dart';
export 'domain/cart_notifier.dart';

final cartRepositoryProvider = Provider<CartRepository>((ref) {
  final dio = ref.watch(apiClientProvider).dio;
  return CartRepository(dio: dio);
});

final cartNotifierProvider = StateNotifierProvider<CartNotifier, CartState>((
  ref,
) {
  return CartNotifier(repository: ref.watch(cartRepositoryProvider));
});

final cartItemCountProvider = Provider<int>((ref) {
  return ref.watch(cartNotifierProvider.select((state) => state.itemCount));
});
