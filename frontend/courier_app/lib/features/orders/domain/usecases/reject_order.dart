import '../repositories/orders_repository.dart';

class RejectOrder {
  final OrdersRepository repository;
  RejectOrder(this.repository);

  Future<void> call(int orderId, int courierId, String reason) {
    return repository.declineOffer(orderId, courierId, reason);
  }
}
