import 'package:flutter_riverpod/flutter_riverpod.dart';

class PartnersNotifier extends StateNotifier<List<dynamic>> {
  PartnersNotifier() : super([]);
}

final partnersProvider = StateNotifierProvider<PartnersNotifier, List<dynamic>>((ref) {
  return PartnersNotifier();
});
