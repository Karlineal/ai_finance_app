import 'package:flutter/material.dart';
import '../theme/theme.dart';

/// AppProgressBar - 统一进度条组件
///
/// 支持不同高度、颜色
class AppProgressBar extends StatelessWidget {
  final double progress; // 0.0 - 1.0
  final double height;
  final Color? backgroundColor;
  final Color? foregroundColor;
  final double radius;

  const AppProgressBar({
    super.key,
    required this.progress,
    this.height = 4,
    this.backgroundColor,
    this.foregroundColor,
    this.radius = 2,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);

    return ClipRRect(
      borderRadius: BorderRadius.circular(radius),
      child: Container(
        height: height,
        decoration: BoxDecoration(
          color: backgroundColor ?? colors.backgroundTertiary,
          borderRadius: BorderRadius.circular(radius),
        ),
        child: FractionallySizedBox(
          alignment: Alignment.centerLeft,
          widthFactor: progress.clamp(0.0, 1.0),
          child: Container(
            decoration: BoxDecoration(
              color: foregroundColor ?? colors.brandPrimary,
              borderRadius: BorderRadius.circular(radius),
            ),
          ),
        ),
      ),
    );
  }
}
