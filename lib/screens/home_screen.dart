import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:intl/intl.dart';
import '../bloc/transaction_bloc.dart';
import '../models/transaction.dart';
import '../theme/theme.dart';
import '../widgets/widgets.dart';
import 'settings_screen.dart';

/// 首页 - 明细页面（Design System 版本）
///
/// 使用新的 Design Tokens 和公共组件
/// 支持 Light/Dark 主题
class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    return Scaffold(
      backgroundColor: colors.backgroundPrimary,
      body: BlocBuilder<TransactionBloc, TransactionState>(
        builder: (context, state) {
          if (state is TransactionLoading) {
            return const Center(child: CircularProgressIndicator());
          }

          final transactions =
              state is TransactionLoaded ? state.transactions : <Transaction>[];

          final monthlyExpense = _calculateMonthlyExpense(transactions);
          final monthlyIncome = _calculateMonthlyIncome(transactions);

          return SafeArea(
            child: Column(
              children: [
                _buildHeader(context, textStyles),
                _buildSummaryCard(context, monthlyExpense, monthlyIncome),
                Expanded(
                  child: _buildTransactionList(context, transactions),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildHeader(BuildContext context, AppTextStyles textStyles) {
    final colors = AppColors.of(context);

    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.md,
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Row(
            children: [
              Text(
                '7月',
                style: textStyles.titleSmall.copyWith(
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(width: AppSpacing.sm),
              Text(
                '7月1日 - 7月31日',
                style: textStyles.bodySmall,
              ),
            ],
          ),
          Row(
            children: [
              IconButton(
                icon: Icon(Icons.search, color: colors.textSecondary),
                onPressed: () {},
              ),
              IconButton(
                icon:
                    Icon(Icons.settings_outlined, color: colors.textSecondary),
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => const SettingsScreen(),
                    ),
                  );
                },
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSummaryCard(
    BuildContext context,
    double expense,
    double income,
  ) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    return AppCard(
      margin: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      padding: const EdgeInsets.all(AppSpacing.lg),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      width: 8,
                      height: 8,
                      decoration: BoxDecoration(
                        color: colors.expense,
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: AppSpacing.xs),
                    Text(
                      '支出',
                      style: textStyles.caption,
                    ),
                  ],
                ),
                const SizedBox(height: AppSpacing.sm),
                AmountText.expense(
                  expense,
                  style: textStyles.amountMedium,
                ),
              ],
            ),
          ),
          Container(
            width: 1,
            height: 40,
            color: colors.divider,
          ),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.only(left: AppSpacing.lg),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Container(
                        width: 8,
                        height: 8,
                        decoration: BoxDecoration(
                          color: colors.income,
                          shape: BoxShape.circle,
                        ),
                      ),
                      const SizedBox(width: AppSpacing.xs),
                      Text(
                        '收入',
                        style: textStyles.caption,
                      ),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.sm),
                  AmountText.income(
                    income,
                    style: textStyles.amountMedium,
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTransactionList(
    BuildContext context,
    List<Transaction> transactions,
  ) {
    if (transactions.isEmpty) {
      return const EmptyStateView(
        icon: Icons.receipt_long_outlined,
        title: '暂无交易记录',
        subtitle: '点击下方按钮记一笔',
      );
    }

    final grouped = _groupByDate(transactions);

    return ListView.builder(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      itemCount: grouped.length,
      itemBuilder: (context, index) {
        final date = grouped.keys.elementAt(index);
        final items = grouped[date]!;
        final dayExpense = items
            .where((t) => t.type == TransactionType.expense)
            .fold(0.0, (sum, t) => sum + t.amount);
        final dayIncome = items
            .where((t) => t.type == TransactionType.income)
            .fold(0.0, (sum, t) => sum + t.amount);

        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildDateHeader(context, date, dayExpense, dayIncome),
            AppCard(
              padding: const EdgeInsets.all(AppSpacing.md),
              margin: const EdgeInsets.only(bottom: AppSpacing.md),
              child: Column(
                children: items
                    .map((t) => TransactionTile(
                          category: t.category,
                          subTitle: t.notes,
                          amount: t.amount,
                          isExpense: t.type == TransactionType.expense,
                          date: t.date,
                          emoji: _getCategoryEmoji(t.category),
                          iconColor: _getCategoryColor(t.category),
                        ))
                    .toList(),
              ),
            ),
          ],
        );
      },
    );
  }

  Widget _buildDateHeader(
    BuildContext context,
    DateTime date,
    double expense,
    double income,
  ) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    return Padding(
      padding: const EdgeInsets.only(
        top: AppSpacing.lg,
        bottom: AppSpacing.sm,
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            _formatDate(date),
            style: textStyles.bodySmall.copyWith(
              fontWeight: FontWeight.w500,
            ),
          ),
          Row(
            children: [
              if (expense > 0)
                Text(
                  '支 ¥${expense.toStringAsFixed(0)}  ',
                  style: textStyles.caption.copyWith(color: colors.expense),
                ),
              if (income > 0)
                Text(
                  '收 ¥${income.toStringAsFixed(0)}',
                  style: textStyles.caption.copyWith(color: colors.income),
                ),
            ],
          ),
        ],
      ),
    );
  }

  // 辅助方法
  double _calculateMonthlyExpense(List<Transaction> transactions) {
    final now = DateTime.now();
    return transactions
        .where((t) =>
            t.type == TransactionType.expense &&
            t.date.year == now.year &&
            t.date.month == now.month)
        .fold(0.0, (sum, t) => sum + t.amount);
  }

  double _calculateMonthlyIncome(List<Transaction> transactions) {
    final now = DateTime.now();
    return transactions
        .where((t) =>
            t.type == TransactionType.income &&
            t.date.year == now.year &&
            t.date.month == now.month)
        .fold(0.0, (sum, t) => sum + t.amount);
  }

  Map<DateTime, List<Transaction>> _groupByDate(
      List<Transaction> transactions) {
    final grouped = <DateTime, List<Transaction>>{};
    for (final t in transactions) {
      final date = DateTime(t.date.year, t.date.month, t.date.day);
      grouped.putIfAbsent(date, () => []).add(t);
    }
    return Map.fromEntries(
      grouped.entries.toList()..sort((a, b) => b.key.compareTo(a.key)),
    );
  }

  String _formatDate(DateTime date) {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final yesterday = today.subtract(const Duration(days: 1));

    if (date == today) return '今天 ${DateFormat('MM月dd日').format(date)}';
    if (date == yesterday) return '昨天 ${DateFormat('MM月dd日').format(date)}';

    final weekdays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
    final weekday = weekdays[date.weekday - 1];
    return '$weekday ${DateFormat('MM月dd日').format(date)}';
  }

  Color _getCategoryColor(String category) {
    final colors = {
      '餐饮': const Color(0xFFE07A5F),
      '交通': const Color(0xFF3D5A80),
      '购物': const Color(0xFF98C1D9),
      '娱乐': const Color(0xFFE0B1CB),
      '生活': const Color(0xFF7CB69D),
      '医疗': const Color(0xFFEE6C4D),
      '教育': const Color(0xFF293241),
      '工资': const Color(0xFF7CB69D),
      '理财': const Color(0xFFD4A373),
    };
    return colors[category] ?? const Color(0xFFD4A373);
  }

  String _getCategoryEmoji(String category) {
    final emojis = {
      '餐饮': '🍔',
      '交通': '🚗',
      '购物': '🛍️',
      '娱乐': '🎬',
      '生活': '🏠',
      '医疗': '🏥',
      '教育': '📚',
      '工资': '💰',
      '理财': '📈',
    };
    return emojis[category] ?? '📋';
  }
}
