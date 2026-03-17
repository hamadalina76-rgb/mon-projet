abstract class TrackingTransport {
  Future<void> connect({required String path, required String jwt});
  bool send(Map<String, dynamic> payload);
  Future<void> disconnect();
  bool get isConnected;
  Stream<bool> get connectionChanges;
}
