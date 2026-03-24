import 'package:flutter/material.dart';
import '../theme/theme.dart';

/// AppCard - 统一卡片组件
///
/// 自动适配深浅主题
/// 浅色：阴影 | 深色：边框
class AppCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? margin;
  final double? radius;
  final Color? backgroundColor;
  final VoidCallback? onTap;
  final bool showBorder;

  const AppCard({
    super.key,
    required this.child,
    this.padding,
    this.margin,
    this.radius,
    this.backgroundColor,
    this.onTap,
    this.showBorder = true,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final isDark = Theme.of(context).brightness == Brightness.dark;

    Widget card = Container(
      margin: margin,
      padding: padding ?? const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: backgroundColor ?? colors.cardPrimary,
        borderRadius: BorderRadius.circular(radius ?? AppRadius.lg),
        border: showBorder && isDark
            ? Border.all(color: colors.border, width: 0.5)
            : null,
        boxShadow: !isDark ? AppShadows.lightCard : AppShadows.none,
      ),
      child: child,
    );

    if (onTap != null) {
      card = GestureDetector(
        onTap: onTap,
        behavior: HitTestBehavior.opaque,
        child: card,
      );
    }

    return card;
  }
}
