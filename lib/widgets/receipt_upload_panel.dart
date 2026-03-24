import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../theme/theme.dart';

/// ReceiptUploadPanel - 发票上传操作面板
///
/// 提供相机拍摄和相册选择两种上传方式
/// 图标+文字样式，居中布局
/// 使用 AppCard 作为容器，带微妙阴影
class ReceiptUploadPanel extends StatelessWidget {
  /// 点击相机按钮回调
  final VoidCallback? onCameraTap;

  /// 点击相册按钮回调
  final VoidCallback? onGalleryTap;

  const ReceiptUploadPanel({
    super.key,
    this.onCameraTap,
    this.onGalleryTap,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    return Container(
      padding: const EdgeInsets.all(AppSpacing.lg),
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
      child: Row(
        children: [
          Expanded(
            child: _UploadButton(
              icon: Icons.camera_alt_outlined,
              label: '拍照',
              backgroundColor: colors.brandPrimary.withOpacity(0.1),
              iconColor: colors.brandPrimary,
              textStyle: textStyles.bodyMedium.copyWith(
                color: colors.brandPrimary,
                fontWeight: FontWeight.w600,
              ),
              onTap: () {
                HapticFeedback.mediumImpact();
                onCameraTap?.call();
              },
            ),
          ),
          const SizedBox(width: AppSpacing.lg),
          Expanded(
            child: _UploadButton(
              icon: Icons.photo_library_outlined,
              label: '相册',
              backgroundColor: colors.brandSecondary.withOpacity(0.1),
              iconColor: colors.brandSecondary,
              textStyle: textStyles.bodyMedium.copyWith(
                color: colors.brandSecondary,
                fontWeight: FontWeight.w600,
              ),
              onTap: () {
                HapticFeedback.mediumImpact();
                onGalleryTap?.call();
              },
            ),
          ),
        ],
      ),
    );
  }
}

/// 内部使用的上传按钮组件
class _UploadButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color backgroundColor;
  final Color iconColor;
  final TextStyle textStyle;
  final VoidCallback? onTap;

  const _UploadButton({
    required this.icon,
    required this.label,
    required this.backgroundColor,
    required this.iconColor,
    required this.textStyle,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);

    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Container(
        padding: const EdgeInsets.symmetric(
          vertical: AppSpacing.lg,
          horizontal: AppSpacing.md,
        ),
        decoration: BoxDecoration(
          color: backgroundColor,
          borderRadius: BorderRadius.circular(AppRadius.md),
          border: Border.all(
            color: iconColor.withOpacity(0.2),
            width: 1,
          ),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              icon,
              color: iconColor,
              size: 28,
            ),
            const SizedBox(height: AppSpacing.sm),
            Text(
              label,
              style: textStyle,
            ),
          ],
        ),
      ),
    );
  }
}
