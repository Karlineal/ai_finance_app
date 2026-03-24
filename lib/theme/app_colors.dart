import 'package:flutter/material.dart';

/// AppColors - 语义化颜色系统
///
/// 禁止在页面中直接使用 Color(0xFFxxxxxx)
/// 所有颜色必须通过 AppColors.of(context) 获取
@immutable
class AppColors {
  // Background
  final Color backgroundPrimary;
  final Color backgroundSecondary;
  final Color backgroundTertiary;

  // Card
  final Color cardPrimary;
  final Color cardSecondary;
  final Color cardElevated;

  // Text
  final Color textPrimary;
  final Color textSecondary;
  final Color textTertiary;
  final Color textInverse;

  // Brand
  final Color brandPrimary;
  final Color brandSecondary;

  // Functional
  final Color expense;
  final Color income;
  final Color success;
  final Color warning;
  final Color error;

  // Divider & Border
  final Color divider;
  final Color border;

  // Overlay
  final Color overlay;
  final Color scrim;

  const AppColors({
    required this.backgroundPrimary,
    required this.backgroundSecondary,
    required this.backgroundTertiary,
    required this.cardPrimary,
    required this.cardSecondary,
    required this.cardElevated,
    required this.textPrimary,
    required this.textSecondary,
    required this.textTertiary,
    required this.textInverse,
    required this.brandPrimary,
    required this.brandSecondary,
    required this.expense,
    required this.income,
    required this.success,
    required this.warning,
    required this.error,
    required this.divider,
    required this.border,
    required this.overlay,
    required this.scrim,
  });

  /// Light Theme Colors
  static const AppColors light = AppColors(
    backgroundPrimary: Color(0xFFF8F5F0),
    backgroundSecondary: Color(0xFFF5F0E8),
    backgroundTertiary: Color(0xFFEDE8E0),
    cardPrimary: Colors.white,
    cardSecondary: Color(0xFFFAFAFA),
    cardElevated: Colors.white,
    textPrimary: Color(0xFF1A1A1A),
    textSecondary: Color(0xFF636E72),
    textTertiary: Color(0xFFB2BEC3),
    textInverse: Colors.white,
    brandPrimary: Color(0xFFD4A373),
    brandSecondary: Color(0xFFE6B88A),
    expense: Color(0xFFE07A5F),
    income: Color(0xFF7CB69D),
    success: Color(0xFF7CB69D),
    warning: Color(0xFFF4D03F),
    error: Color(0xFFE07A5F),
    divider: Color(0xFFEEEEEE),
    border: Color(0xFFE0E0E0),
    overlay: Color(0x1F000000),
    scrim: Color(0x99000000),
  );

  /// Dark Theme Colors (Cookie Style)
  static const AppColors dark = AppColors(
    backgroundPrimary: Color(0xFF1C1C1E),
    backgroundSecondary: Color(0xFF2C2C2E),
    backgroundTertiary: Color(0xFF3A3A3C),
    cardPrimary: Color(0xFF2C2C2E),
    cardSecondary: Color(0xFF3A3A3C),
    cardElevated: Color(0xFF444446),
    textPrimary: Colors.white,
    textSecondary: Color(0xFF8E8E93),
    textTertiary: Color(0xFF636366),
    textInverse: Color(0xFF1C1C1E),
    brandPrimary: Color(0xFFD4A373),
    brandSecondary: Color(0xFFE6B88A),
    expense: Color(0xFFFF6B6B),
    income: Color(0xFF7CB69D),
    success: Color(0xFF7CB69D),
    warning: Color(0xFFFFD93D),
    error: Color(0xFFFF6B6B),
    divider: Color(0xFF3A3A3C),
    border: Color(0xFF3A3A3C),
    overlay: Color(0x1FFFFFFF),
    scrim: Color(0x99000000),
  );

  static AppColors of(BuildContext context) {
    final brightness = Theme.of(context).brightness;
    return brightness == Brightness.dark ? dark : light;
  }
}
