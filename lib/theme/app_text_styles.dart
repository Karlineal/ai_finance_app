import 'package:flutter/material.dart';
import 'app_colors.dart';

/// AppTextStyles - 字体层级系统
///
/// 禁止在页面中直接写 TextStyle
/// 统一使用 AppTextStyles.of(context).titleLarge 等
@immutable
class AppTextStyles {
  final TextStyle titleLarge;
  final TextStyle titleMedium;
  final TextStyle titleSmall;

  final TextStyle bodyLarge;
  final TextStyle bodyMedium;
  final TextStyle bodySmall;

  final TextStyle caption;
  final TextStyle label;

  final TextStyle amountLarge;
  final TextStyle amountMedium;
  final TextStyle amountSmall;

  const AppTextStyles({
    required this.titleLarge,
    required this.titleMedium,
    required this.titleSmall,
    required this.bodyLarge,
    required this.bodyMedium,
    required this.bodySmall,
    required this.caption,
    required this.label,
    required this.amountLarge,
    required this.amountMedium,
    required this.amountSmall,
  });

  static AppTextStyles of(BuildContext context) {
    final colors = AppColors.of(context);

    return AppTextStyles(
      titleLarge: TextStyle(
        fontSize: 24,
        fontWeight: FontWeight.bold,
        color: colors.textPrimary,
        letterSpacing: -0.5,
      ),
      titleMedium: TextStyle(
        fontSize: 20,
        fontWeight: FontWeight.w600,
        color: colors.textPrimary,
      ),
      titleSmall: TextStyle(
        fontSize: 18,
        fontWeight: FontWeight.w600,
        color: colors.textPrimary,
      ),
      bodyLarge: TextStyle(
        fontSize: 16,
        fontWeight: FontWeight.w500,
        color: colors.textPrimary,
      ),
      bodyMedium: TextStyle(
        fontSize: 14,
        fontWeight: FontWeight.normal,
        color: colors.textPrimary,
      ),
      bodySmall: TextStyle(
        fontSize: 13,
        fontWeight: FontWeight.normal,
        color: colors.textSecondary,
      ),
      caption: TextStyle(
        fontSize: 12,
        fontWeight: FontWeight.normal,
        color: colors.textTertiary,
      ),
      label: TextStyle(
        fontSize: 11,
        fontWeight: FontWeight.w500,
        color: colors.textSecondary,
      ),
      amountLarge: TextStyle(
        fontSize: 32,
        fontWeight: FontWeight.bold,
        color: colors.textPrimary,
        letterSpacing: -1,
      ),
      amountMedium: TextStyle(
        fontSize: 20,
        fontWeight: FontWeight.w600,
        color: colors.textPrimary,
      ),
      amountSmall: TextStyle(
        fontSize: 16,
        fontWeight: FontWeight.w600,
        color: colors.textPrimary,
      ),
    );
  }
}
