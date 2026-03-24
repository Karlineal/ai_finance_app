import 'package:equatable/equatable.dart';

class Transaction extends Equatable {
  final String id;
  final DateTime date;
  final String payee;
  final double amount;
  final String? notes;
  final String category;
  final TransactionType type;
  final String? source;
  final DateTime createdAt;
  final DateTime updatedAt;
  final String currency; // 货币代码，如 CNY, USD

  const Transaction({
    required this.id,
    required this.date,
    required this.payee,
    required this.amount,
    this.notes,
    required this.category,
    required this.type,
    this.source,
    required this.createdAt,
    required this.updatedAt,
    this.currency = 'CNY',
  });

  bool get isExpense => type == TransactionType.expense;
  bool get isIncome => type == TransactionType.income;

  Transaction copyWith({
    String? id,
    DateTime? date,
    String? payee,
    double? amount,
    String? notes,
    String? category,
    TransactionType? type,
    String? source,
    DateTime? createdAt,
    DateTime? updatedAt,
    String? currency,
  }) {
    return Transaction(
      id: id ?? this.id,
      date: date ?? this.date,
      payee: payee ?? this.payee,
      amount: amount ?? this.amount,
      notes: notes ?? this.notes,
      category: category ?? this.category,
      type: type ?? this.type,
      source: source ?? this.source,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
      currency: currency ?? this.currency,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'date': date.toIso8601String(),
      'payee': payee,
      'amount': amount,
      'notes': notes,
      'category': category,
      'type': type.name,
      'source': source,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt.toIso8601String(),
      'currency': currency,
    };
  }

  factory Transaction.fromMap(Map<String, dynamic> map) {
    return Transaction(
      id: map['id'] as String,
      date: DateTime.parse(map['date'] as String),
      payee: map['payee'] as String,
      amount: (map['amount'] as num).toDouble(),
      notes: map['notes'] as String?,
      category: map['category'] as String,
      type: TransactionType.values.byName(map['type'] as String),
      source: map['source'] as String?,
      createdAt: DateTime.parse(map['createdAt'] as String),
      updatedAt: DateTime.parse(map['updatedAt'] as String),
      currency: map['currency'] as String? ?? 'CNY',
    );
  }

  @override
  List<Object?> get props => [
        id,
        date,
        payee,
        amount,
        notes,
        category,
        type,
        source,
        createdAt,
        updatedAt,
        currency,
      ];
}

enum TransactionType { expense, income }

class Category extends Equatable {
  final String id;
  final String name;
  final String? icon;
  final int color;
  final TransactionType type;
  final int sortOrder;

  const Category({
    required this.id,
    required this.name,
    this.icon,
    required this.color,
    required this.type,
    this.sortOrder = 0,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'icon': icon,
      'color': color,
      'type': type.name,
      'sortOrder': sortOrder,
    };
  }

  factory Category.fromMap(Map<String, dynamic> map) {
    return Category(
      id: map['id'] as String,
      name: map['name'] as String,
      icon: map['icon'] as String?,
      color: map['color'] as int,
      type: TransactionType.values.byName(map['type'] as String),
      sortOrder: map['sortOrder'] as int? ?? 0,
    );
  }

  @override
  List<Object?> get props => [id, name, icon, color, type, sortOrder];
}

final List<Category> defaultCategories = [
  const Category(
      id: 'food', name: '餐饮', color: 0xFFFF6B6B, type: TransactionType.expense),
  const Category(
      id: 'transport',
      name: '交通',
      color: 0xFF4ECDC4,
      type: TransactionType.expense),
  const Category(
      id: 'shopping',
      name: '购物',
      color: 0xFFFFE66D,
      type: TransactionType.expense),
  const Category(
      id: 'entertainment',
      name: '娱乐',
      color: 0xFF9B59B6,
      type: TransactionType.expense),
  const Category(
      id: 'utilities',
      name: '生活缴费',
      color: 0xFF3498DB,
      type: TransactionType.expense),
  const Category(
      id: 'housing',
      name: '居住',
      color: 0xFFE67E22,
      type: TransactionType.expense),
  const Category(
      id: 'medical',
      name: '医疗',
      color: 0xFFE74C3C,
      type: TransactionType.expense),
  const Category(
      id: 'education',
      name: '教育',
      color: 0xFF2ECC71,
      type: TransactionType.expense),
  const Category(
      id: 'salary',
      name: '工资',
      color: 0xFF27AE60,
      type: TransactionType.income),
  const Category(
      id: 'investment',
      name: '理财收益',
      color: 0xFFF39C12,
      type: TransactionType.income),
  const Category(
      id: 'other_expense',
      name: '其他支出',
      color: 0xFF95A5A6,
      type: TransactionType.expense),
  const Category(
      id: 'other_income',
      name: '其他收入',
      color: 0xFF95A5A6,
      type: TransactionType.income),
];
