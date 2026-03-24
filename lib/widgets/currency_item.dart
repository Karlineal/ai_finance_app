import 'package:flutter/material.dart';
import '../models/currency.dart';
import '../theme/theme.dart';
import 'app_card.dart';
import 'tappable.dart';

/// 货币列表项组件 - 带点击反馈
class CurrencyItem extends StatelessWidget {
  final Currency currency;
  final bool isSelected;
  final VoidCallback? onTap;

  const CurrencyItem({
    super.key,
    required this.currency,
    this.isSelected = false,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    return Tappable(
      onTap: onTap,
      child: AppCard(
        margin: const EdgeInsets.only(bottom: AppSpacing.sm),
        padding: const EdgeInsets.all(AppSpacing.md),
        backgroundColor: isSelected
            ? colors.brandPrimary.withValues(alpha: 0.1)
            : colors.cardPrimary,
        showBorder: isSelected,
        child: Row(
          children: [
            Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                color: isSelected
                    ? colors.brandPrimary.withValues(alpha: 0.2)
                    : colors.backgroundSecondary,
                borderRadius: BorderRadius.circular(AppRadius.md),
              ),
              child: Center(
                child: Text(
                  currency.flag,
                  style: const TextStyle(fontSize: 24),
                ),
              ),
            ),
            const SizedBox(width: AppSpacing.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    currency.name,
                    style: textStyles.bodyLarge.copyWith(
                      fontWeight: FontWeight.w600,
                      color:
                          isSelected ? colors.brandPrimary : colors.textPrimary,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  Text(
                    '${currency.code} · ${currency.symbol}',
                    style: textStyles.bodySmall.copyWith(
                      color: colors.textSecondary.withValues(alpha: 0.7),
                    ),
                  ),
                ],
              ),
            ),
            if (isSelected)
              Container(
                width: 28,
                height: 28,
                decoration: BoxDecoration(
                  color: colors.brandPrimary,
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.check, color: Colors.white, size: 18),
              ),
          ],
        ),
      ),
    );
  }
}
