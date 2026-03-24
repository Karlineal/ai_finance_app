import 'package:flutter/material.dart';
import '../models/transaction.dart';

/// 分类元数据
///
/// 集中管理所有分类的：
/// - 显示名称
/// - Emoji 图标
/// - 颜色
/// - 类型（支出/收入）
class CategoryMeta {
  final String id;
  final String name;
  final String emoji;
  final Color color;
  final TransactionType type;

  const CategoryMeta({
    required this.id,
    required this.name,
    required this.emoji,
    required this.color,
    required this.type,
  });
}

/// 支出分类
final List<CategoryMeta> expenseCategories = [
  const CategoryMeta(
    id: 'food',
    name: '餐饮',
    emoji: '🍔',
    color: Color(0xFFE07A5F),
    type: TransactionType.expense,
  ),
  const CategoryMeta(
    id: 'transport',
    name: '交通',
    emoji: '🚗',
    color: Color(0xFF3D5A80),
    type: TransactionType.expense,
  ),
  const CategoryMeta(
    id: 'shopping',
    name: '购物',
    emoji: '🛍️',
    color: Color(0xFF98C1D9),
    type: TransactionType.expense,
  ),
  const CategoryMeta(
    id: 'entertainment',
    name: '娱乐',
    emoji: '🎬',
    color: Color(0xFFE0B1CB),
    type: TransactionType.expense,
  ),
  const CategoryMeta(
    id: 'life',
    name: '生活',
    emoji: '🏠',
    color: Color(0xFF7CB69D),
    type: TransactionType.expense,
  ),
  const CategoryMeta(
    id: 'medical',
    name: '医疗',
    emoji: '🏥',
    color: Color(0xFFEE6C4D),
    type: TransactionType.expense,
  ),
  const CategoryMeta(
    id: 'education',
    name: '教育',
    emoji: '📚',
    color: Color(0xFF293241),
    type: TransactionType.expense,
  ),
  const CategoryMeta(
    id: 'digital',
    name: '数码',
    emoji: '💻',
    color: Color(0xFF6C757D),
    type: TransactionType.expense,
  ),
  const CategoryMeta(
    id: 'pet',
    name: '宠物',
    emoji: '🐱',
    color: Color(0xFFFFB4A2),
    type: TransactionType.expense,
  ),
  const CategoryMeta(
    id: 'travel',
    name: '旅行',
    emoji: '✈️',
    color: Color(0xFF48CAE4),
    type: TransactionType.expense,
  ),
  const CategoryMeta(
    id: 'beauty',
    name: '美妆',
    emoji: '💄',
    color: Color(0xFFFF6B9D),
    type: TransactionType.expense,
  ),
  const CategoryMeta(
    id: 'other_expense',
    name: '其他',
    emoji: '📋',
    color: Color(0xFFADB5BD),
    type: TransactionType.expense,
  ),
];

/// 收入分类
final List<CategoryMeta> incomeCategories = [
  const CategoryMeta(
    id: 'salary',
    name: '工资',
    emoji: '💰',
    color: Color(0xFF7CB69D),
    type: TransactionType.income,
  ),
  const CategoryMeta(
    id: 'investment',
    name: '理财',
    emoji: '📈',
    color: Color(0xFFD4A373),
    type: TransactionType.income,
  ),
  const CategoryMeta(
    id: 'bonus',
    name: '奖金',
    emoji: '🎁',
    color: Color(0xFFE9C46A),
    type: TransactionType.income,
  ),
  const CategoryMeta(
    id: 'parttime',
    name: '兼职',
    emoji: '💼',
    color: Color(0xFF2A9D8F),
    type: TransactionType.income,
  ),
  const CategoryMeta(
    id: 'other_income',
    name: '其他',
    emoji: '📋',
    color: Color(0xFFADB5BD),
    type: TransactionType.income,
  ),
];

/// 获取所有分类
List<CategoryMeta> getAllCategories() {
  return [...expenseCategories, ...incomeCategories];
}

/// 根据类型获取分类
List<CategoryMeta> getCategoriesByType(TransactionType type) {
  return type == TransactionType.expense ? expenseCategories : incomeCategories;
}

/// 根据ID获取分类
CategoryMeta? getCategoryById(String id) {
  try {
    return getAllCategories().firstWhere((c) => c.id == id);
  } catch (_) {
    return null;
  }
}

/// 根据名称获取分类
CategoryMeta? getCategoryByName(String name) {
  try {
    return getAllCategories().firstWhere((c) => c.name == name);
  } catch (_) {
    return null;
  }
}

/// 获取分类颜色
Color getCategoryColor(String categoryName) {
  final category = getCategoryByName(categoryName);
  return category?.color ?? const Color(0xFFD4A373);
}

/// 获取分类Emoji
String getCategoryEmoji(String categoryName) {
  final category = getCategoryByName(categoryName);
  return category?.emoji ?? '📋';
}

/// 获取分类类型
TransactionType? getCategoryType(String categoryName) {
  final category = getCategoryByName(categoryName);
  return category?.type;
}
