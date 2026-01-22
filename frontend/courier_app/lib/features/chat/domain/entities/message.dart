enum MessageType {
  text,
  audio,
  image,
}

enum MessageSender {
  courier,
  customer,
  support,
}

class Message {
  final String id;
  final String conversationId;
  final MessageType type;
  final MessageSender sender;
  final String content;
  final DateTime timestamp;
  final bool isRead;

  Message({
    required this.id,
    required this.conversationId,
    required this.type,
    required this.sender,
    required this.content,
    required this.timestamp,
    required this.isRead,
  });
}

class Conversation {
  final String id;
  final String orderId;
  final String customerName;
  final String? customerPhoto;
  final Message? lastMessage;
  final int unreadCount;

  Conversation({
    required this.id,
    required this.orderId,
    required this.customerName,
    this.customerPhoto,
    this.lastMessage,
    required this.unreadCount,
  });
}
