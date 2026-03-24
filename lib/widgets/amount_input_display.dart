import 'package:flutter/material.dart';
import '../theme/theme.dart';

/// 金额输入显示器
///
/// 显示当前输入的金额，支持支出/收入不同颜色
class AmountInputDisplay extends StatelessWidget {
  final String amount;
  final bool isExpense;
  final VoidCallback? onClear;

  const AmountInputDisplay({
    super.key,
    required this.amount,
    required this.isExpense,
    this.onClear,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.xl,
        vertical: AppSpacing.lg,
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          // 货币符号
          Text(
            '¥',
            style: textStyles.amountLarge.copyWith(
              color: isExpense ? colors.expense : colors.income,
              fontWeight: FontWeight.w500,
            ),
          ),
          const SizedBox(width: AppSpacing.sm),
          // 金额
          Expanded(
            child: Text(
              amount.isEmpty ? '0' : amount,
              style: textStyles.amountLarge.copyWith(
                color: isExpense ? colors.expense : colors.income,
                fontWeight: FontWeight.bold,
              ),
              textAlign: TextAlign.center,
            ),
          ),
          // 清除按钮
          if (amount.isNotEmpty)
            GestureDetector(
              onTap: onClear,
              child: Container(
                padding: const EdgeInsets.all(AppSpacing.sm),
                decoration: BoxDecoration(
                  color: colors.textTertiary.withValues(alpha: 0.1),
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  Icons.close,
                  size: 20,
                  color: colors.textSecondary,
                ),
              ),
            ),
        ],
      ),
    );
  }
}
