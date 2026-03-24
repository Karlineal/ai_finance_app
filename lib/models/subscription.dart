import 'package:equatable/equatable.dart';

/// 订阅/分期模型
class Subscription extends Equatable {
  final String id;
  final String name;
  final String? icon;
  final double amount;
  final SubscriptionType type;
  final SubscriptionFrequency frequency;
  final int frequencyValue; // 每N个周期
  final DateTime startDate;
  final DateTime? endDate;
  final String category;
  final String? notes;
  final bool isActive;
  final DateTime createdAt;
  final DateTime updatedAt;

  const Subscription({
    required this.id,
    required this.name,
    this.icon,
    required this.amount,
    required this.type,
    required this.frequency,
    this.frequencyValue = 1,
    required this.startDate,
    this.endDate,
    required this.category,
    this.notes,
    this.isActive = true,
    required this.createdAt,
    required this.updatedAt,
  });

  /// 获取下次扣费日期
  DateTime getNextPaymentDate() {
    final now = DateTime.now();
    var nextDate = startDate;

    while (nextDate.isBefore(now) || nextDate.isAtSameMomentAs(now)) {
      nextDate = _addPeriod(nextDate);
    }

    return nextDate;
  }

  /// 计算是否已经到期
  bool get isExpired => endDate != null && DateTime.now().isAfter(endDate!);

  /// 获取剩余天数
  int getRemainingDays() {
    final nextPayment = getNextPaymentDate();
    return nextPayment.difference(DateTime.now()).inDays;
  }

  /// 获取总期数(用于分期)
  int? getTotalInstallments() {
    if (type != SubscriptionType.installment || endDate == null) return null;
    var count = 0;
    var date = startDate;
    while (date.isBefore(endDate!) || date.isAtSameMomentAs(endDate!)) {
      count++;
      date = _addPeriod(date);
    }
    return count;
  }

  /// 获取已还期数
  int? getPaidInstallments() {
    if (type != SubscriptionType.installment) return null;
    final now = DateTime.now();
    var count = 0;
    var date = startDate;
    while (date.isBefore(now)) {
      count++;
      date = _addPeriod(date);
    }
    return count;
  }

  /// 获取剩余期数
  int? getRemainingInstallments() {
    final total = getTotalInstallments();
    final paid = getPaidInstallments();
    if (total == null || paid == null) return null;
    return total - paid;
  }

  DateTime _addPeriod(DateTime date) {
    switch (frequency) {
      case SubscriptionFrequency.daily:
        return date.add(Duration(days: frequencyValue));
      case SubscriptionFrequency.weekly:
        return date.add(Duration(days: 7 * frequencyValue));
      case SubscriptionFrequency.monthly:
        return DateTime(date.year, date.month + frequencyValue, date.day);
      case SubscriptionFrequency.yearly:
        return DateTime(date.year + frequencyValue, date.month, date.day);
    }
  }

  Subscription copyWith({
    String? id,
    String? name,
    String? icon,
    double? amount,
    SubscriptionType? type,
    SubscriptionFrequency? frequency,
    int? frequencyValue,
    DateTime? startDate,
    DateTime? endDate,
    String? category,
    String? notes,
    bool? isActive,
    DateTime? createdAt,
    DateTime? updatedAt,
  }) {
    return Subscription(
      id: id ?? this.id,
      name: name ?? this.name,
      icon: icon ?? this.icon,
      amount: amount ?? this.amount,
      type: type ?? this.type,
      frequency: frequency ?? this.frequency,
      frequencyValue: frequencyValue ?? this.frequencyValue,
      startDate: startDate ?? this.startDate,
      endDate: endDate ?? this.endDate,
      category: category ?? this.category,
      notes: notes ?? this.notes,
      isActive: isActive ?? this.isActive,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'icon': icon,
      'amount': amount,
      'type': type.name,
      'frequency': frequency.name,
      'frequencyValue': frequencyValue,
      'startDate': startDate.toIso8601String(),
      'endDate': endDate?.toIso8601String(),
      'category': category,
      'notes': notes,
      'isActive': isActive ? 1 : 0,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt.toIso8601String(),
    };
  }

  factory Subscription.fromMap(Map<String, dynamic> map) {
    return Subscription(
      id: map['id'] as String,
      name: map['name'] as String,
      icon: map['icon'] as String?,
      amount: (map['amount'] as num).toDouble(),
      type: SubscriptionType.values.byName(map['type'] as String),
      frequency:
          SubscriptionFrequency.values.byName(map['frequency'] as String),
      frequencyValue: map['frequencyValue'] as int? ?? 1,
      startDate: DateTime.parse(map['startDate'] as String),
      endDate: map['endDate'] != null
          ? DateTime.parse(map['endDate'] as String)
          : null,
      category: map['category'] as String,
      notes: map['notes'] as String?,
      isActive: (map['isActive'] as int? ?? 1) == 1,
      createdAt: DateTime.parse(map['createdAt'] as String),
      updatedAt: DateTime.parse(map['updatedAt'] as String),
    );
  }

  @override
  List<Object?> get props => [
        id,
        name,
        icon,
        amount,
        type,
        frequency,
        frequencyValue,
        startDate,
        endDate,
        category,
        notes,
        isActive,
        createdAt,
        updatedAt,
      ];
}

/// 订阅类型
enum SubscriptionType {
  subscription, // 订阅(周期性扣费)
  installment, // 分期(有结束日期)
}

/// 订阅频率
enum SubscriptionFrequency {
  daily, // 日
  weekly, // 周
  monthly, // 月
  yearly, // 年
}

/// 订阅图标模板
final List<SubscriptionIconTemplate> subscriptionIconTemplates = [
  const SubscriptionIconTemplate(name: '视频', icon: 'movie', color: 0xFFE74C3C),
  const SubscriptionIconTemplate(
      name: '音乐', icon: 'music_note', color: 0xFF3498DB),
  const SubscriptionIconTemplate(name: '云存储', icon: 'cloud', color: 0xFF2ECC71),
  const SubscriptionIconTemplate(name: '软件', icon: 'apps', color: 0xFF9B59B6),
  const SubscriptionIconTemplate(
      name: '游戏', icon: 'sports_esports', color: 0xFFE67E22),
  const SubscriptionIconTemplate(
      name: '健身', icon: 'fitness_center', color: 0xFF1ABC9C),
  const SubscriptionIconTemplate(
      name: '新闻', icon: 'newspaper', color: 0xFF34495E),
  const SubscriptionIconTemplate(
      name: '购物', icon: 'shopping_bag', color: 0xFFF39C12),
  const SubscriptionIconTemplate(
      name: '外卖', icon: 'delivery_dining', color: 0xFFFF6B6B),
  const SubscriptionIconTemplate(
      name: '交通', icon: 'directions_car', color: 0xFF3498DB),
  const SubscriptionIconTemplate(name: '学习', icon: 'school', color: 0xFF27AE60),
  const SubscriptionIconTemplate(
      name: '其他', icon: 'more_horiz', color: 0xFF95A5A6),
];

class SubscriptionIconTemplate extends Equatable {
  final String name;
  final String icon;
  final int color;

  const SubscriptionIconTemplate({
    required this.name,
    required this.icon,
    required this.color,
  });

  @override
  List<Object?> get props => [name, icon, color];
}
