import 'package:flutter/material.dart';
import '../theme/theme.dart';
import '../models/subscription.dart';

/// SubscriptionItemCard - 订阅项目卡片
///
/// 显示订阅的图标、名称、分类、金额和下次扣费日期
/// 支持左滑删除
class SubscriptionItemCard extends StatelessWidget {
  final Subscription subscription;
  final VoidCallback? onTap;
  final VoidCallback? onDelete;
  final VoidCallback? onEdit;

  const SubscriptionItemCard({
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

    final nextPaymentDate = subscription.getNextPaymentDate();
    final remainingDays = subscription.getRemainingDays();

    return Dismissible(
      key: Key('subscription_${subscription.id}'),
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
          child: Row(
            children: [
              // 图标区域
              _buildIcon(context, colors),
              const SizedBox(width: AppSpacing.lg),
              // 内容区域
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // 名称和金额
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Expanded(
                          child: Text(
                            subscription.name,
                            style: textStyles.bodyLarge.copyWith(
                              fontWeight: FontWeight.w600,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        Text(
                          '-¥${subscription.amount.toStringAsFixed(0)}',
                          style: textStyles.amountSmall.copyWith(
                            color: colors.expense,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: AppSpacing.xs),
                    // 分类和下次扣费
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: AppSpacing.sm,
                            vertical: AppSpacing.xs,
                          ),
                          decoration: BoxDecoration(
                            color: colors.backgroundTertiary,
                            borderRadius: BorderRadius.circular(AppRadius.sm),
                          ),
                          child: Text(
                            subscription.category,
                            style: textStyles.caption.copyWith(
                              color: colors.textSecondary,
                            ),
                          ),
                        ),
                        Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(
                              Icons.calendar_today_outlined,
                              size: 12,
                              color: remainingDays <= 3
                                  ? colors.warning
                                  : colors.textTertiary,
                            ),
                            const SizedBox(width: AppSpacing.xs),
                            Text(
                              _getNextPaymentText(remainingDays),
                              style: textStyles.caption.copyWith(
                                color: remainingDays <= 3
                                    ? colors.warning
                                    : colors.textSecondary,
                                fontWeight: remainingDays <= 3
                                    ? FontWeight.w500
                                    : FontWeight.normal,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ],
                ),
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
        return Icons.subscriptions_outlined;
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

  String _getNextPaymentText(int days) {
    if (days == 0) return '今天';
    if (days == 1) return '明天';
    if (days < 7) return '$days天后';
    if (days < 30) return '${(days / 7).floor()}周后';
    return '${(days / 30).floor()}个月后';
  }
}
