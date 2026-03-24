import 'package:flutter/material.dart';
import '../theme/theme.dart';
import '../models/subscription.dart';

/// InstallmentItemCard - 分期项目卡片
///
/// 显示分期的图标、名称、总金额、还款进度和剩余期数
/// 支持左滑删除
class InstallmentItemCard extends StatelessWidget {
  final Subscription subscription;
  final VoidCallback? onTap;
  final VoidCallback? onDelete;
  final VoidCallback? onEdit;

  const InstallmentItemCard({
    super.key,
    required this.subscription,
    this.onTap,
    this.onDelete,
    this.onEdit,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);
    final isDark = Theme.of(context).brightness == Brightness.dark;

    final totalPeriods = subscription.getTotalInstallments() ?? 0;
    final paidPeriods = subscription.getPaidInstallments() ?? 0;
    final remainingPeriods = subscription.getRemainingInstallments() ?? 0;
    final progress = totalPeriods > 0 ? paidPeriods / totalPeriods : 0.0;
    final isCompleted = paidPeriods >= totalPeriods;

    return Dismissible(
      key: Key('installment_${subscription.id}'),
      direction: DismissDirection.endToStart,
      background: _buildDismissBackground(colors),
      onDismissed: (_) => onDelete?.call(),
      child: GestureDetector(
        onTap: onTap,
        child: Container(
          margin: const EdgeInsets.only(bottom: AppSpacing.lg),
          padding: const EdgeInsets.all(AppSpacing.lg),
          decoration: BoxDecoration(
            color: colors.cardPrimary,
            borderRadius: BorderRadius.circular(AppRadius.lg),
            border:
                isDark ? Border.all(color: colors.border, width: 0.5) : null,
            boxShadow: isDark ? null : AppShadows.lightCard,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 第一行：图标、名称、状态标签
              Row(
                children: [
                  _buildIcon(context, colors),
                  const SizedBox(width: AppSpacing.lg),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          subscription.name,
                          style: textStyles.bodyLarge.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: AppSpacing.xs),
                        Text(
                          '¥${subscription.amount.toStringAsFixed(0)}',
                          style: textStyles.amountSmall.copyWith(
                            color: colors.expense,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                  ),
                  _buildStatusBadge(context, colors, textStyles, isCompleted,
                      remainingPeriods),
                ],
              ),
              const SizedBox(height: AppSpacing.lg),
              // 进度条区域
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        // 进度条
                        ClipRRect(
                          borderRadius: BorderRadius.circular(AppRadius.sm),
                          child: LinearProgressIndicator(
                            value: progress.clamp(0.0, 1.0),
                            backgroundColor: colors.backgroundTertiary,
                            valueColor: AlwaysStoppedAnimation<Color>(
                              isCompleted
                                  ? colors.success
                                  : colors.brandPrimary,
                            ),
                            minHeight: 8,
                          ),
                        ),
                        const SizedBox(height: AppSpacing.sm),
                        // 期数信息
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              '已还 $paidPeriods 期',
                              style: textStyles.caption.copyWith(
                                color: colors.textSecondary,
                              ),
                            ),
                            Text(
                              '共 $totalPeriods 期',
                              style: textStyles.caption.copyWith(
                                color: colors.textSecondary,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: AppSpacing.lg),
                  // 进度百分比
                  Text(
                    '${(progress * 100).toStringAsFixed(0)}%',
                    style: textStyles.bodyMedium.copyWith(
                      color: isCompleted ? colors.success : colors.brandPrimary,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildDismissBackground(AppColors colors) {
    return Container(
      margin: const EdgeInsets.only(bottom: AppSpacing.lg),
      decoration: BoxDecoration(
        color: colors.expense,
        borderRadius: BorderRadius.circular(AppRadius.lg),
      ),
      alignment: Alignment.centerRight,
      padding: const EdgeInsets.only(right: AppSpacing.xl),
      child: const Icon(
        Icons.delete_outline,
        color: Colors.white,
        size: 24,
      ),
    );
  }

  Widget _buildIcon(BuildContext context, AppColors colors) {
    final iconData = _getIconData(subscription.icon);
    final iconColor = _getIconColor();

    return Container(
      width: 48,
      height: 48,
      decoration: BoxDecoration(
        color: Color(iconColor).withOpacity(0.15),
        borderRadius: BorderRadius.circular(AppRadius.md),
      ),
      child: Icon(
        iconData,
        color: Color(iconColor),
        size: 24,
      ),
    );
  }

  Widget _buildStatusBadge(
    BuildContext context,
    AppColors colors,
    AppTextStyles textStyles,
    bool isCompleted,
    int remainingPeriods,
  ) {
    if (isCompleted) {
      return Container(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.sm,
          vertical: AppSpacing.xs,
        ),
        decoration: BoxDecoration(
          color: colors.success.withOpacity(0.15),
          borderRadius: BorderRadius.circular(AppRadius.sm),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.check_circle_outline,
              size: 12,
              color: colors.success,
            ),
            const SizedBox(width: AppSpacing.xs),
            Text(
              '已还清',
              style: textStyles.caption.copyWith(
                color: colors.success,
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
        ),
      );
    }

    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.sm,
        vertical: AppSpacing.xs,
      ),
      decoration: BoxDecoration(
        color: colors.warning.withOpacity(0.15),
        borderRadius: BorderRadius.circular(AppRadius.sm),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            Icons.hourglass_empty_outlined,
            size: 12,
            color: colors.warning,
          ),
          const SizedBox(width: AppSpacing.xs),
          Text(
            '剩余 $remainingPeriods 期',
            style: textStyles.caption.copyWith(
              color: colors.warning,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }

  IconData _getIconData(String? iconName) {
    switch (iconName) {
      case 'movie':
        return Icons.movie_outlined;
      case 'music_note':
        return Icons.music_note_outlined;
      case 'cloud':
        return Icons.cloud_outlined;
      case 'apps':
        return Icons.apps_outlined;
      case 'sports_esports':
        return Icons.sports_esports_outlined;
      case 'fitness_center':
        return Icons.fitness_center_outlined;
      case 'newspaper':
        return Icons.newspaper_outlined;
      case 'shopping_bag':
        return Icons.shopping_bag_outlined;
      case 'delivery_dining':
        return Icons.delivery_dining_outlined;
      case 'directions_car':
        return Icons.directions_car_outlined;
      case 'school':
        return Icons.school_outlined;
      case 'more_horiz':
        return Icons.more_horiz_outlined;
      default:
        return Icons.payment_outlined;
    }
  }

  int _getIconColor() {
    final template = subscriptionIconTemplates.firstWhere(
      (t) => t.icon == subscription.icon,
      orElse: () => const SubscriptionIconTemplate(
        name: '其他',
        icon: 'more_horiz',
        color: 0xFF95A5A6,
      ),
    );
    return template.color;
  }
}
