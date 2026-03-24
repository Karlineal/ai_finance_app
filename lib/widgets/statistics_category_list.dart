import 'package:flutter/material.dart';
import '../theme/theme.dart';
import 'amount_text.dart';
import 'app_progress_bar.dart';

/// 统计分类列表项组件
class StatisticsCategoryItem extends StatelessWidget {
  final String categoryName;
  final int categoryColor;
  final double amount;
  final double percentage;
  final IconData? icon;

  const StatisticsCategoryItem({
    super.key,
    required this.categoryName,
    required this.categoryColor,
    required this.amount,
    required this.percentage,
    this.icon,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);
    final color = Color(categoryColor);

    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.md,
      ),
      child: Row(
        children: [
          // 分类图标
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.15),
              borderRadius: BorderRadius.circular(AppRadius.md),
            ),
            child: icon != null
                ? Icon(icon, color: color, size: 20)
                : Center(
                    child: Text(
                      categoryName.substring(0, 1),
                      style: TextStyle(
                        color: color,
                        fontWeight: FontWeight.bold,
                        fontSize: 16,
                      ),
                    ),
                  ),
          ),
          const SizedBox(width: AppSpacing.md),

          // 分类信息和进度
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      categoryName,
                      style: textStyles.bodyMedium.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    Text(
                      '${percentage.toStringAsFixed(1)}%',
                      style: textStyles.caption.copyWith(
                        color: colors.textSecondary,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: AppSpacing.sm),
                AppProgressBar(
                  progress: percentage / 100,
                  height: 6,
                  backgroundColor: colors.divider,
                  foregroundColor: color,
                ),
                const SizedBox(height: AppSpacing.xs),
                AmountText.expense(
                  amount,
                  style: textStyles.bodySmall,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// 统计分类列表组件
class StatisticsCategoryList extends StatelessWidget {
  final List<Map<String, dynamic>> data;
  final double total;

  const StatisticsCategoryList({
    super.key,
    required this.data,
    required this.total,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);

    if (data.isEmpty) {
      return const SizedBox.shrink();
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 标题
        Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.lg,
            vertical: AppSpacing.md,
          ),
          child: Text(
            '分类明细',
            style: AppTextStyles.of(context).titleSmall.copyWith(
                  fontWeight: FontWeight.w600,
                ),
          ),
        ),

        // 列表
        Container(
          margin: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
          decoration: BoxDecoration(
            color: colors.cardPrimary,
            borderRadius: BorderRadius.circular(AppRadius.lg),
          ),
          child: Column(
            children: data.asMap().entries.map((entry) {
              final index = entry.key;
              final item = entry.value;
              final percentage = total > 0
                  ? ((item['total'] as double).abs() / total * 100)
                  : 0.0;

              return Column(
                children: [
                  StatisticsCategoryItem(
                    categoryName: item['categoryName'] as String,
                    categoryColor: item['categoryColor'] as int,
                    amount: (item['total'] as double).abs(),
                    percentage: percentage,
                  ),
                  if (index < data.length - 1)
                    Divider(
                      height: 1,
                      indent: AppSpacing.lg + 40 + AppSpacing.md,
                      color: colors.divider,
                    ),
                ],
              );
            }).toList(),
          ),
        ),
      ],
    );
  }
}
