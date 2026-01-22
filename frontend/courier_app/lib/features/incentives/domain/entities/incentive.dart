enum IncentiveType {
  dailyQuest,
  weeklyChallenge,
  peakHoursBonus,
  deliveryStreak,
  ratingBonus,
}

class Incentive {
  final String id;
  final IncentiveType type;
  final String title;
  final String description;
  final double reward;
  final int currentProgress;
  final int targetProgress;
  final DateTime startDate;
  final DateTime endDate;
  final bool isCompleted;
  final bool isClaimed;

  Incentive({
    required this.id,
    required this.type,
    required this.title,
    required this.description,
    required this.reward,
    required this.currentProgress,
    required this.targetProgress,
    required this.startDate,
    required this.endDate,
    required this.isCompleted,
    required this.isClaimed,
  });

  double get progressPercentage => (currentProgress / targetProgress * 100).clamp(0, 100);
}
