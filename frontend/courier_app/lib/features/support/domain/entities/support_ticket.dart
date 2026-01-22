enum TicketStatus {
  open,
  inProgress,
  resolved,
  closed,
}

enum TicketPriority {
  low,
  medium,
  high,
  urgent,
}

class SupportTicket {
  final String id;
  final String subject;
  final String description;
  final TicketStatus status;
  final TicketPriority priority;
  final DateTime createdAt;
  final DateTime? resolvedAt;
  final List<TicketMessage> messages;

  SupportTicket({
    required this.id,
    required this.subject,
    required this.description,
    required this.status,
    required this.priority,
    required this.createdAt,
    this.resolvedAt,
    required this.messages,
  });
}

class TicketMessage {
  final String id;
  final String message;
  final bool isFromSupport;
  final DateTime timestamp;

  TicketMessage({
    required this.id,
    required this.message,
    required this.isFromSupport,
    required this.timestamp,
  });
}
