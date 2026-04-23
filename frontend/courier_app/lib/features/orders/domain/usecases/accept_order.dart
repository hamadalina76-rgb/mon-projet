import '../repositories/orders_repository.dart';

class AcceptOrder {
  final OrdersRepository repository;
  AcceptOrder(this.repository);

  Future<void> call(int orderId, int courierId) {
    return repository.acceptOffer(orderId, courierId);
  }
}
