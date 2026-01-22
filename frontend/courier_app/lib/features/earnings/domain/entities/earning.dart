class Earning {
  final String id;
  final String deliveryId;
  final double baseAmount;
  final double bonus;
  final double tip;
  final double total;
  final DateTime earnedAt;
  final String status;

  Earning({
    required this.id,
    required this.deliveryId,
    required this.baseAmount,
    required this.bonus,
    required this.tip,
    required this.total,
    required this.earnedAt,
    required this.status,
  });
}

class EarningsSummary {
  final double today;
  final double thisWeek;
  final double thisMonth;
  final int deliveriesToday;
  final int deliveriesThisWeek;
  final int deliveriesThisMonth;

  EarningsSummary({
    required this.today,
    required this.thisWeek,
    required this.thisMonth,
    required this.deliveriesToday,
    required this.deliveriesThisWeek,
    required this.deliveriesThisMonth,
  });
}
