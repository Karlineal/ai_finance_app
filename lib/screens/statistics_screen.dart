import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../bloc/transaction_bloc.dart';
import '../models/transaction.dart';
import '../theme/theme.dart';
import '../widgets/widgets.dart';
import '../constants/category_meta.dart';

/// 统计页面 - Design System 版本
///
/// 甜甜圈图 + 中心总计 + 分类占比
class StatisticsScreen extends StatefulWidget {
  const StatisticsScreen({super.key});

  @override
  State<StatisticsScreen> createState() => _StatisticsScreenState();
}

class _StatisticsScreenState extends State<StatisticsScreen> {
  bool _isExpense = true;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);

    return Scaffold(
      backgroundColor: colors.backgroundPrimary,
      body: BlocBuilder<TransactionBloc, TransactionState>(
        builder: (context, state) {
          if (state is! TransactionLoaded) {
            return const Center(child: CircularProgressIndicator());
          }

          final stats = _getCategoryStats(state.transactions, _isExpense);
          final total = stats.fold<double>(
            0,
            (sum, item) => sum + (item['amount'] as double),
          );

          return SafeArea(
            child: Column(
              children: [
                _buildHeader(),
                _buildTypeSwitch(),
                Expanded(
                  child: stats.isEmpty
                      ? _buildEmptyState()
                      : SingleChildScrollView(
                          padding:
                              const EdgeInsets.only(bottom: AppSpacing.xxl),
                          child: Column(
                            children: [
                              _buildDonutChart(stats, total),
                              const SizedBox(height: AppSpacing.lg),
                              StatisticsCategoryList(
                                data: stats,
                                total: total,
                              ),
                            ],
                          ),
                        ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.all(AppSpacing.lg),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            '7月',
            style: AppTextStyles.of(context).titleMedium.copyWith(
                  fontWeight: FontWeight.bold,
                ),
          ),
          Text(
            '7月1日 - 7月31日',
            style: AppTextStyles.of(context).bodySmall,
          ),
        ],
      ),
    );
  }

  Widget _buildTypeSwitch() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
      child: TransactionTypeSwitch(
        isExpense: _isExpense,
        onChanged: (value) => setState(() => _isExpense = value),
      ),
    );
  }

  Widget _buildDonutChart(List<Map<String, dynamic>> stats, double total) {
    return AppCard(
      margin: const EdgeInsets.all(AppSpacing.lg),
      padding: const EdgeInsets.all(AppSpacing.lg),
      child: StatisticsDonutChart(
        data: stats,
        total: total,
        title: _isExpense ? '总支出' : '总收入',
        subtitle: '本月',
      ),
    );
  }

  Widget _buildEmptyState() {
    return Expanded(
      child: EmptyStateView(
        icon: Icons.pie_chart_outline,
        title: '暂无数据',
        subtitle: '本月还没有${_isExpense ? '支出' : '收入'}记录',
      ),
    );
  }

  List<Map<String, dynamic>> _getCategoryStats(
    List<dynamic> transactions,
    bool isExpense,
  ) {
    final Map<String, Map<String, dynamic>> stats = {};

    for (final t in transactions) {
      final amount = t.amount as double;
      final isTExpense = amount < 0;

      if (isTExpense != isExpense) continue;

      final category = t.category as String;
      final meta = getCategoryByName(category) ??
          const CategoryMeta(
            id: 'unknown',
            name: '未知',
            emoji: '📦',
            color: Color(0xFF95A5A6),
            type: TransactionType.expense,
          );

      if (!stats.containsKey(category)) {
        stats[category] = {
          'categoryName': category,
          'categoryColor': meta.color,
          'amount': 0.0,
        };
      }
      stats[category]!['amount'] =
          (stats[category]!['amount'] as double) + amount.abs();
    }

    final result = stats.values.toList();
    result.sort(
        (a, b) => (b['amount'] as double).compareTo(a['amount'] as double));
    return result;
  }
}
