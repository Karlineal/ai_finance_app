import 'package:flutter/material.dart';
import '../theme/theme.dart';
import '../widgets/widgets.dart';

/// 预算总览卡片组件
///
/// 展示月度总预算、已使用、剩余金额、使用比例
class BudgetSummaryCard extends StatelessWidget {
  final double totalBudget;
  final double usedAmount;
  final VoidCallback? onEditTap;

  const BudgetSummaryCard({
    super.key,
    required this.totalBudget,
    required this.usedAmount,
    this.onEditTap,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    final remaining = totalBudget - usedAmount;
    final percentage = totalBudget > 0 ? usedAmount / totalBudget : 0.0;
    final isOverBudget = usedAmount > totalBudget;

    return AppCard(
      margin: const EdgeInsets.all(AppSpacing.lg),
      padding: const EdgeInsets.all(AppSpacing.xl),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 标题行
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '本月预算',
                style: textStyles.titleSmall,
              ),
              GestureDetector(
                onTap: onEditTap,
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: AppSpacing.md,
                    vertical: AppSpacing.sm,
                  ),
                  decoration: BoxDecoration(
                    color: colors.backgroundSecondary,
                    borderRadius: BorderRadius.circular(AppRadius.md),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        Icons.edit_outlined,
                        size: 14,
                        color: colors.textSecondary,
                      ),
                      const SizedBox(width: AppSpacing.xs),
                      Text(
                        '编辑',
                        style: textStyles.caption.copyWith(
                          color: colors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),

          const SizedBox(height: AppSpacing.xl),

          // 金额行
          Row(
            children: [
              Expanded(
                child: _buildAmountColumn(
                  context,
                  label: '总预算',
                  amount: totalBudget,
                  color: colors.textPrimary,
                ),
              ),
              Container(
                width: 1,
                height: 40,
                color: colors.divider,
              ),
              Expanded(
                child: _buildAmountColumn(
                  context,
                  label: '已使用',
                  amount: usedAmount,
                  color: isOverBudget ? colors.expense : colors.textPrimary,
                ),
              ),
              Container(
                width: 1,
                height: 40,
                color: colors.divider,
              ),
              Expanded(
                child: _buildAmountColumn(
                  context,
                  label: isOverBudget ? '超支' : '剩余',
                  amount: remaining.abs(),
                  color: isOverBudget ? colors.expense : colors.income,
                ),
              ),
            ],
          ),

          const SizedBox(height: AppSpacing.xl),

          // 进度条
          ClipRRect(
            borderRadius: BorderRadius.circular(AppRadius.sm),
            child: LinearProgressIndicator(
              value: percentage.clamp(0.0, 1.0),
              backgroundColor: colors.backgroundSecondary,
              valueColor: AlwaysStoppedAnimation<Color>(
                isOverBudget ? colors.expense : colors.brandPrimary,
              ),
              minHeight: 8,
            ),
          ),

          const SizedBox(height: AppSpacing.md),

          // 百分比文字
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '已使用 ${(percentage * 100).toStringAsFixed(1)}%',
                style: textStyles.caption.copyWith(
                  color: colors.textSecondary,
                ),
              ),
              if (isOverBudget)
                Text(
                  '超支 ${(remaining.abs()).toStringAsFixed(0)}',
                  style: textStyles.caption.copyWith(
                    color: colors.expense,
                    fontWeight: FontWeight.w600,
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildAmountColumn(
    BuildContext context, {
    required String label,
    required double amount,
    required Color color,
  }) {
    final textStyles = AppTextStyles.of(context);

    return Column(
      children: [
        Text(
          label,
          style: textStyles.caption.copyWith(
            color: AppColors.of(context).textSecondary,
          ),
        ),
        const SizedBox(height: AppSpacing.xs),
        Text(
          '¥${amount.toStringAsFixed(0)}',
          style: textStyles.amountMedium.copyWith(
            color: color,
            fontSize: 20,
          ),
        ),
      ],
    );
  }
}
