import 'package:flutter/material.dart';
import '../theme/theme.dart';
import 'amount_text.dart';

/// TransactionTile - 交易列表项组件
///
/// 统一交易项样式：左侧图标 - 中间分类/备注 - 右侧金额/时间
class TransactionTile extends StatelessWidget {
  final String category;
  final String? subTitle;
  final double amount;
  final bool isExpense;
  final DateTime? date;
  final String? emoji;
  final IconData? icon;
  final Color? iconColor;
  final VoidCallback? onTap;

  const TransactionTile({
    super.key,
    required this.category,
    this.subTitle,
    required this.amount,
    required this.isExpense,
    this.date,
    this.emoji,
    this.icon,
    this.iconColor,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.md,
        ),
        child: Row(
          children: [
            // 左侧图标
            _buildIcon(colors),
            const SizedBox(width: AppSpacing.lg),
            // 中间内容
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    category,
                    style: textStyles.bodyLarge.copyWith(
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  if (subTitle != null) ...[
                    const SizedBox(height: AppSpacing.xs),
                    Text(
                      subTitle!,
                      style: textStyles.caption,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ],
              ),
            ),
            // 右侧金额和时间
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                AmountText(
                  amount: amount,
                  isExpense: isExpense,
                  style: textStyles.amountSmall,
                ),
                if (date != null) ...[
                  const SizedBox(height: AppSpacing.xs),
                  Text(
                    _formatTime(date!),
                    style: textStyles.caption,
                  ),
                ],
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildIcon(AppColors colors) {
    final bgColor =
        iconColor?.withValues(alpha: 0.15) ?? colors.backgroundTertiary;
    final fgColor = iconColor ?? colors.brandPrimary;

    return Container(
      width: 44,
      height: 44,
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(AppRadius.md),
      ),
      child: Center(
        child: emoji != null
            ? Text(emoji!, style: const TextStyle(fontSize: 22))
            : Icon(icon ?? Icons.category, color: fgColor, size: 22),
      ),
    );
  }

  String _formatTime(DateTime date) {
    return '${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}';
  }
}
