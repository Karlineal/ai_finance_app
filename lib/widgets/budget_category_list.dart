import 'package:flutter/material.dart';
import '../theme/theme.dart';
import '../widgets/widgets.dart';
import '../constants/category_meta.dart';
import '../models/transaction.dart';

/// 分类预算项组件
class BudgetCategoryItem extends StatelessWidget {
  final String categoryId;
  final String categoryName;
  final double budgetAmount;
  final double usedAmount;
  final VoidCallback? onTap;

  const BudgetCategoryItem({
    super.key,
    required this.categoryId,
    required this.categoryName,
    required this.budgetAmount,
    required this.usedAmount,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    final percentage = budgetAmount > 0 ? usedAmount / budgetAmount : 0.0;
    final isOverBudget = usedAmount > budgetAmount;
    final remaining = budgetAmount - usedAmount;

    // 获取分类元数据
    final meta = getCategoryById(categoryId) ??
        const CategoryMeta(
          id: 'unknown',
          name: '未知',
          emoji: '📦',
          color: Color(0xFF95A5A6),
          type: TransactionType.expense,
        );

    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 第一行：图标、名称、金额
            Row(
              children: [
                // 图标
                Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    color: meta.color.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(AppRadius.md),
                  ),
                  child: Center(
                    child: Text(
                      meta.emoji,
                      style: const TextStyle(fontSize: 20),
                    ),
                  ),
                ),
                const SizedBox(width: AppSpacing.md),

                // 名称和金额
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        categoryName,
                        style: textStyles.bodyMedium.copyWith(
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(height: AppSpacing.xs),
                      Row(
                        children: [
                          Text(
                            '已用 ¥${usedAmount.toStringAsFixed(0)}',
                            style: textStyles.caption.copyWith(
                              color: isOverBudget
                                  ? colors.expense
                                  : colors.textSecondary,
                            ),
                          ),
                          Text(
                            ' / 预算 ¥${budgetAmount.toStringAsFixed(0)}',
                            style: textStyles.caption.copyWith(
                              color: colors.textTertiary,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),

                // 剩余/超支金额
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text(
                      isOverBudget ? '超支' : '剩余',
                      style: textStyles.caption.copyWith(
                        color: isOverBudget ? colors.expense : colors.income,
                      ),
                    ),
                    const SizedBox(height: AppSpacing.xs),
                    Text(
                      '¥${remaining.abs().toStringAsFixed(0)}',
                      style: textStyles.bodyMedium.copyWith(
                        color: isOverBudget ? colors.expense : colors.income,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
              ],
            ),

            const SizedBox(height: AppSpacing.md),

            // 进度条
            AppProgressBar(
              progress: percentage.clamp(0.0, 1.0),
              height: 6,
              backgroundColor: colors.backgroundSecondary,
              foregroundColor:
                  _getProgressColor(colors, percentage, isOverBudget),
            ),
          ],
        ),
      ),
    );
  }

  Color _getProgressColor(
      AppColors colors, double percentage, bool isOverBudget) {
    if (isOverBudget) return colors.expense;
    if (percentage >= 0.9) return colors.warning;
    if (percentage >= 0.7) return colors.brandPrimary;
    return colors.income;
  }
}

/// 分类预算列表组件
class BudgetCategoryList extends StatelessWidget {
  final List<Map<String, dynamic>> categories;
  final Map<String, double> categoryBudgets;
  final Map<String, double> categoryExpenses;
  final Function(String categoryId, String categoryName, double currentBudget)?
      onEditBudget;

  const BudgetCategoryList({
    super.key,
    required this.categories,
    required this.categoryBudgets,
    required this.categoryExpenses,
    this.onEditBudget,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    if (categories.isEmpty) {
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
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '分类预算',
                style: textStyles.titleSmall.copyWith(
                  fontWeight: FontWeight.w600,
                ),
              ),
              Text(
                '${categories.length} 个分类',
                style: textStyles.caption.copyWith(
                  color: colors.textSecondary,
                ),
              ),
            ],
          ),
        ),

        // 列表
        AppCard(
          margin: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
          padding: EdgeInsets.zero,
          child: Column(
            children: categories.asMap().entries.map((entry) {
              final index = entry.key;
              final category = entry.value;
              final id = category['id'] as String;
              final name = category['name'] as String;
              final budget = categoryBudgets[id] ?? 0.0;
              final expense = categoryExpenses[id] ?? 0.0;

              return Column(
                children: [
                  BudgetCategoryItem(
                    categoryId: id,
                    categoryName: name,
                    budgetAmount: budget,
                    usedAmount: expense,
                    onTap: () => onEditBudget?.call(id, name, budget),
                  ),
                  if (index < categories.length - 1)
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
