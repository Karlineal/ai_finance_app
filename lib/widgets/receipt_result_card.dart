import 'package:flutter/material.dart';
import '../theme/theme.dart';
import 'amount_text.dart';

/// ReceiptResultCard - 发票识别结果卡片
///
/// 显示识别出的关键信息：
/// - 金额（大字突出显示）
/// - 商家名称
/// - 日期
/// - 类别
/// 每个字段带编辑按钮
/// 使用 Design System 一致的风格
class ReceiptResultCard extends StatelessWidget {
  /// 金额
  final double amount;

  /// 商家名称
  final String merchant;

  /// 日期字符串
  final String date;

  /// 类别名称
  final String category;

  /// 类别图标
  final IconData? categoryIcon;

  /// 编辑金额回调
  final VoidCallback? onEditAmount;

  /// 编辑商家回调
  final VoidCallback? onEditMerchant;

  /// 编辑日期回调
  final VoidCallback? onEditDate;

  /// 编辑类别回调
  final VoidCallback? onEditCategory;

  /// 是否可编辑
  final bool editable;

  const ReceiptResultCard({
    super.key,
    required this.amount,
    required this.merchant,
    required this.date,
    required this.category,
    this.categoryIcon,
    this.onEditAmount,
    this.onEditMerchant,
    this.onEditDate,
    this.onEditCategory,
    this.editable = true,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    return Container(
      padding: const EdgeInsets.all(AppSpacing.xl),
      decoration: BoxDecoration(
        color: colors.cardPrimary,
        borderRadius: BorderRadius.circular(AppRadius.lg),
        border: Theme.of(context).brightness == Brightness.dark
            ? Border.all(color: colors.border, width: 0.5)
            : null,
        boxShadow: Theme.of(context).brightness == Brightness.light
            ? AppShadows.lightCard
            : AppShadows.none,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 标题栏
          Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: colors.success.withOpacity(0.15),
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  Icons.check_circle_outline,
                  color: colors.success,
                  size: 22,
                ),
              ),
              const SizedBox(width: AppSpacing.md),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '识别成功',
                      style: textStyles.bodyLarge.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      '已自动提取发票信息',
                      style: textStyles.caption.copyWith(
                        color: colors.textTertiary,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),

          const SizedBox(height: AppSpacing.xl),

          // 金额区域（突出显示）
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(
              vertical: AppSpacing.lg,
              horizontal: AppSpacing.xl,
            ),
            decoration: BoxDecoration(
              color: colors.backgroundSecondary,
              borderRadius: BorderRadius.circular(AppRadius.md),
            ),
            child: Column(
              children: [
                Text(
                  '消费金额',
                  style: textStyles.caption.copyWith(
                    color: colors.textSecondary,
                  ),
                ),
                const SizedBox(height: AppSpacing.sm),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    AmountText.expense(
                      amount,
                      style: textStyles.amountLarge,
                      prefix: '¥',
                    ),
                    if (editable) ...[
                      const SizedBox(width: AppSpacing.sm),
                      _EditButton(
                        onTap: onEditAmount,
                        color: colors.textTertiary,
                      ),
                    ],
                  ],
                ),
              ],
            ),
          ),

          const SizedBox(height: AppSpacing.lg),

          // 详细信息列表
          Container(
            decoration: BoxDecoration(
              color: colors.backgroundSecondary,
              borderRadius: BorderRadius.circular(AppRadius.md),
            ),
            child: Column(
              children: [
                _InfoRow(
                  icon: Icons.store_outlined,
                  label: '商家',
                  value: merchant,
                  onEdit: editable ? onEditMerchant : null,
                ),
                Divider(
                  color: colors.divider,
                  height: 1,
                  indent: AppSpacing.xxl,
                  endIndent: AppSpacing.lg,
                ),
                _InfoRow(
                  icon: Icons.calendar_today_outlined,
                  label: '日期',
                  value: date,
                  onEdit: editable ? onEditDate : null,
                ),
                Divider(
                  color: colors.divider,
                  height: 1,
                  indent: AppSpacing.xxl,
                  endIndent: AppSpacing.lg,
                ),
                _InfoRow(
                  icon: categoryIcon ?? Icons.category_outlined,
                  label: '类别',
                  value: category,
                  onEdit: editable ? onEditCategory : null,
                  valueColor: colors.brandPrimary,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// 信息行组件
class _InfoRow extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;
  final VoidCallback? onEdit;
  final Color? valueColor;

  const _InfoRow({
    required this.icon,
    required this.label,
    required this.value,
    this.onEdit,
    this.valueColor,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.md,
      ),
      child: Row(
        children: [
          Icon(
            icon,
            color: colors.textTertiary,
            size: 20,
          ),
          const SizedBox(width: AppSpacing.md),
          SizedBox(
            width: 48,
            child: Text(
              label,
              style: textStyles.bodySmall.copyWith(
                color: colors.textSecondary,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: textStyles.bodyMedium.copyWith(
                fontWeight: FontWeight.w600,
                color: valueColor ?? colors.textPrimary,
              ),
              overflow: TextOverflow.ellipsis,
            ),
          ),
          if (onEdit != null)
            _EditButton(
              onTap: onEdit,
              color: colors.textTertiary,
            ),
        ],
      ),
    );
  }
}

/// 编辑按钮组件
class _EditButton extends StatelessWidget {
  final VoidCallback? onTap;
  final Color color;

  const _EditButton({
    this.onTap,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Container(
        padding: const EdgeInsets.all(AppSpacing.xs),
        decoration: BoxDecoration(
          color: color.withOpacity(0.1),
          borderRadius: BorderRadius.circular(AppRadius.sm),
        ),
        child: Icon(
          Icons.edit_outlined,
          color: color,
          size: 16,
        ),
      ),
    );
  }
}
