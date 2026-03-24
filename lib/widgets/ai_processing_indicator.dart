import 'package:flutter/material.dart';
import '../theme/theme.dart';

/// AIProcessingIndicator - AI 识别处理中指示器
///
/// 带脉冲动画效果的加载指示器
/// 显示 "AI识别中..." 文字
/// 包含进度点和微光效果
/// 支持深浅主题
class AIProcessingIndicator extends StatefulWidget {
  /// 提示文字，默认为 "AI识别中..."
  final String? message;

  /// 副标题说明
  final String? subtitle;

  const AIProcessingIndicator({
    super.key,
    this.message,
    this.subtitle,
  });

  @override
  State<AIProcessingIndicator> createState() => _AIProcessingIndicatorState();
}

class _AIProcessingIndicatorState extends State<AIProcessingIndicator>
    with TickerProviderStateMixin {
  late final AnimationController _pulseController;
  late final AnimationController _dotsController;
  late final Animation<double> _pulseAnimation;

  @override
  void initState() {
    super.initState();

    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    )..repeat(reverse: true);

    _dotsController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    )..repeat();

    _pulseAnimation = Tween<double>(
      begin: 0.8,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _pulseController,
      curve: Curves.easeInOut,
    ));
  }

  @override
  void dispose() {
    _pulseController.dispose();
    _dotsController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    return Container(
      padding: const EdgeInsets.all(AppSpacing.xxl),
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
        mainAxisSize: MainAxisSize.min,
        children: [
          // 脉冲动画圆圈
          AnimatedBuilder(
            animation: _pulseAnimation,
            builder: (context, child) {
              return Container(
                width: 72,
                height: 72,
                decoration: BoxDecoration(
                  color: colors.brandPrimary
                      .withOpacity(0.1 * _pulseAnimation.value),
                  shape: BoxShape.circle,
                ),
                child: Center(
                  child: ScaleTransition(
                    scale: _pulseAnimation,
                    child: Container(
                      width: 56,
                      height: 56,
                      decoration: BoxDecoration(
                        color: colors.brandPrimary.withOpacity(0.15),
                        shape: BoxShape.circle,
                      ),
                      child: Center(
                        child: SizedBox(
                          width: 28,
                          height: 28,
                          child: CircularProgressIndicator(
                            strokeWidth: 2.5,
                            valueColor: AlwaysStoppedAnimation<Color>(
                              colors.brandPrimary,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              );
            },
          ),
          const SizedBox(height: AppSpacing.xl),

          // 主标题带动态省略号
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                widget.message ?? 'AI识别中',
                style: textStyles.titleSmall.copyWith(
                  color: colors.textPrimary,
                ),
              ),
              _AnimatedDots(
                controller: _dotsController,
                color: colors.brandPrimary,
              ),
            ],
          ),

          const SizedBox(height: AppSpacing.sm),

          // 副标题
          if (widget.subtitle != null)
            Text(
              widget.subtitle!,
              style: textStyles.bodySmall.copyWith(
                color: colors.textTertiary,
              ),
              textAlign: TextAlign.center,
            )
          else
            Text(
              '正在分析发票信息，请稍候',
              style: textStyles.bodySmall.copyWith(
                color: colors.textTertiary,
              ),
              textAlign: TextAlign.center,
            ),
        ],
      ),
    );
  }
}

/// 动态省略号动画组件
class _AnimatedDots extends StatelessWidget {
  final AnimationController controller;
  final Color color;

  const _AnimatedDots({
    required this.controller,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: controller,
      builder: (context, child) {
        final progress = controller.value;
        final textStyles = AppTextStyles.of(context);

        String dots;
        if (progress < 0.33) {
          dots = '.';
        } else if (progress < 0.66) {
          dots = '..';
        } else {
          dots = '...';
        }

        return Text(
          dots,
          style: textStyles.titleSmall.copyWith(
            color: color,
          ),
        );
      },
    );
  }
}

/// 简化的 shimmer 效果包装器（可选使用）
class ShimmerLoading extends StatefulWidget {
  final Widget child;
  final Color baseColor;
  final Color highlightColor;

  const ShimmerLoading({
    super.key,
    required this.child,
    required this.baseColor,
    required this.highlightColor,
  });

  @override
  State<ShimmerLoading> createState() => _ShimmerLoadingState();
}

class _ShimmerLoadingState extends State<ShimmerLoading>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    )..repeat();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, child) {
        return ShaderMask(
          shaderCallback: (bounds) {
            return LinearGradient(
              colors: [
                widget.baseColor,
                widget.highlightColor,
                widget.baseColor,
              ],
              stops: const [0.0, 0.5, 1.0],
              begin: Alignment(-1.0 + _controller.value * 2, 0),
              end: Alignment(0.0 + _controller.value * 2, 0),
            ).createShader(bounds);
          },
          child: widget.child,
        );
      },
    );
  }
}
