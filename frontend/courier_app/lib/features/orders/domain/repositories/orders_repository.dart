abstract class OrdersRepository {
  Future<void> acceptOffer(int orderId, int courierId);
  Future<void> declineOffer(int orderId, int courierId, String reason);
}
