import 'dart:ui';
import 'package:flutter/material.dart';

/// 高级液态玻璃卡片组件
///
/// 特性：
/// - 多层模糊效果（背景模糊 + 内容模糊）
/// - 光泽高光边缘
/// - 折射渐变边框
/// - 可选的动画流光效果
class LiquidGlassCard extends StatefulWidget {
  final Widget child;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? margin;
  final BorderRadius? borderRadius;
  final double blur;
  final Color? backgroundColor;
  final List<Color>? shimmerColors;
  final bool enableShimmer;
  final VoidCallback? onTap;
  final Border? border;
  final List<BoxShadow>? boxShadow;

  const LiquidGlassCard({
    super.key,
    required this.child,
    this.padding,
    this.margin,
    this.borderRadius,
    this.blur = 40.0,
    this.backgroundColor,
    this.shimmerColors,
    this.enableShimmer = false,
    this.onTap,
    this.border,
    this.boxShadow,
  });

  @override
  State<LiquidGlassCard> createState() => _LiquidGlassCardState();
}

class _LiquidGlassCardState extends State<LiquidGlassCard>
    with SingleTickerProviderStateMixin {
  late AnimationController _shimmerController;

  @override
  void initState() {
    super.initState();
    _shimmerController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 3000),
    );
    if (widget.enableShimmer) {
      _shimmerController.repeat();
    }
  }

  @override
  void dispose() {
    _shimmerController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final radius = widget.borderRadius ?? BorderRadius.circular(28);
    final shimmerColors = widget.shimmerColors ??
        [
          const Color(0xFF00D4AA).withValues(alpha: 0.3),
          const Color(0xFF7B61FF).withValues(alpha: 0.2),
        ];

    Widget content = ClipRRect(
      borderRadius: radius,
      child: Stack(
        children: [
          // 底层模糊 - 背景模糊
          BackdropFilter(
            filter: ImageFilter.blur(
              sigmaX: widget.blur,
              sigmaY: widget.blur,
              tileMode: TileMode.decal,
            ),
            child: Container(),
          ),

          // 玻璃底色层
          Container(
            decoration: BoxDecoration(
              color: widget.backgroundColor ??
                  Colors.white.withValues(alpha: 0.03),
              borderRadius: radius,
            ),
          ),

          // 光泽高光层 - 模拟液态表面反射
          Positioned(
            top: 0,
            left: 0,
            right: 0,
            child: Container(
              height: 60,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.only(
                  topLeft: radius.topLeft,
                  topRight: radius.topRight,
                ),
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [
                    Colors.white.withValues(alpha: 0.12),
                    Colors.white.withValues(alpha: 0.02),
                    Colors.transparent,
                  ],
                ),
              ),
            ),
          ),

          // 可选的流光动画效果
          if (widget.enableShimmer)
            AnimatedBuilder(
              animation: _shimmerController,
              builder: (context, child) {
                return Positioned.fill(
                  child: CustomPaint(
                    painter: _LiquidShimmerPainter(
                      progress: _shimmerController.value,
                      colors: shimmerColors,
                    ),
                  ),
                );
              },
            ),

          // 内容层
          Container(
            padding: widget.padding ?? const EdgeInsets.all(20),
            child: widget.child,
          ),

          // 边框 - 折射效果
          Positioned.fill(
            child: IgnorePointer(
              child: Container(
                decoration: BoxDecoration(
                  borderRadius: radius,
                  border: widget.border ??
                      Border.all(
                        color: Colors.white.withValues(alpha: 0.12),
                        width: 1,
                      ),
                ),
              ),
            ),
          ),
        ],
      ),
    );

    // 外层阴影和点击处理
    content = Container(
      margin: widget.margin ??
          const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(
        borderRadius: radius,
        boxShadow: widget.boxShadow ??
            [
              // 主阴影 - 柔和扩散
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.25),
                blurRadius: 40,
                offset: const Offset(0, 16),
                spreadRadius: -8,
              ),
              // 内发光 - 边缘提亮
              BoxShadow(
                color: Colors.white.withValues(alpha: 0.05),
                blurRadius: 20,
                offset: const Offset(0, -4),
                spreadRadius: 0,
              ),
            ],
      ),
      child: content,
    );

    if (widget.onTap != null) {
      return GestureDetector(
        onTap: widget.onTap,
        child: content,
      );
    }

    return content;
  }
}

/// 流光效果绘制器
class _LiquidShimmerPainter extends CustomPainter {
  final double progress;
  final List<Color> colors;

  _LiquidShimmerPainter({
    required this.progress,
    required this.colors,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final shimmerWidth = size.width * 0.4;
    final shimmerOffset = (size.width + shimmerWidth) * progress - shimmerWidth;

    final gradient = LinearGradient(
      begin: Alignment.centerLeft,
      end: Alignment.centerRight,
      colors: [
        Colors.transparent,
        colors[0],
        colors.length > 1 ? colors[1] : colors[0],
        Colors.transparent,
      ],
      stops: const [0.0, 0.4, 0.6, 1.0],
    );

    final paint = Paint()
      ..shader = gradient.createShader(
        Rect.fromLTWH(shimmerOffset, 0, shimmerWidth, size.height),
      )
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 30);

    // 绘制斜向流光
    final path = Path()
      ..moveTo(shimmerOffset, 0)
      ..lineTo(shimmerOffset + shimmerWidth * 0.3, 0)
      ..lineTo(shimmerOffset + shimmerWidth, size.height)
      ..lineTo(shimmerOffset + shimmerWidth * 0.7, size.height)
      ..close();

    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(covariant _LiquidShimmerPainter oldDelegate) {
    return oldDelegate.progress != progress;
  }
}

/// 高级玻璃按钮 - 液态极光效果
class LiquidGlassButton extends StatefulWidget {
  final VoidCallback onPressed;
  final Widget child;
  final Color? color;
  final List<Color>? gradientColors;
  final double? width;
  final double? height;

  const LiquidGlassButton({
    super.key,
    required this.onPressed,
    required this.child,
    this.color,
    this.gradientColors,
    this.width,
    this.height,
  });

  @override
  State<LiquidGlassButton> createState() => _LiquidGlassButtonState();
}

class _LiquidGlassButtonState extends State<LiquidGlassButton> {
  bool _isPressed = false;

  @override
  Widget build(BuildContext context) {
    final gradientColors = widget.gradientColors ??
        [
          const Color(0xFF00D4AA),
          const Color(0xFF7B61FF),
        ];

    return GestureDetector(
      onTapDown: (_) => setState(() => _isPressed = true),
      onTapUp: (_) {
        setState(() => _isPressed = false);
        widget.onPressed();
      },
      onTapCancel: () => setState(() => _isPressed = false),
      child: AnimatedScale(
        duration: const Duration(milliseconds: 150),
        curve: Curves.easeOutCubic,
        scale: _isPressed ? 0.97 : 1.0,
        child: Container(
          width: widget.width,
          height: widget.height ?? 56,
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(20),
            child: Stack(
              children: [
                // 背景模糊
                BackdropFilter(
                  filter: ImageFilter.blur(sigmaX: 20, sigmaY: 20),
                  child: Container(),
                ),

                // 渐变背景
                Container(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                      colors: gradientColors
                          .map((c) => c.withValues(alpha: 0.85))
                          .toList(),
                    ),
                  ),
                ),

                // 光泽层
                Positioned(
                  top: 0,
                  left: 0,
                  right: 0,
                  height: 30,
                  child: Container(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                        colors: [
                          Colors.white.withValues(alpha: 0.25),
                          Colors.transparent,
                        ],
                      ),
                    ),
                  ),
                ),

                // 内容
                Center(child: widget.child),

                // 边框
                Positioned.fill(
                  child: Container(
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(
                        color: Colors.white.withValues(alpha: 0.3),
                        width: 1,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// 高级图标按钮 - 液态玻璃圆形
class LiquidGlassIconButton extends StatelessWidget {
  final IconData icon;
  final VoidCallback onTap;
  final String? tooltip;
  final double size;
  final Color? iconColor;

  const LiquidGlassIconButton({
    super.key,
    required this.icon,
    required this.onTap,
    this.tooltip,
    this.size = 44,
    this.iconColor,
  });

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(16),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 20, sigmaY: 20),
        child: Container(
          width: size,
          height: size,
          decoration: BoxDecoration(
            color: Colors.white.withValues(alpha: 0.06),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(
              color: Colors.white.withValues(alpha: 0.15),
              width: 1,
            ),
            // 内发光
            boxShadow: [
              BoxShadow(
                color: Colors.white.withValues(alpha: 0.05),
                blurRadius: 10,
                offset: const Offset(0, -2),
              ),
            ],
          ),
          child: Material(
            color: Colors.transparent,
            child: InkWell(
              onTap: onTap,
              borderRadius: BorderRadius.circular(16),
              child: Icon(
                icon,
                color: iconColor ?? Colors.white.withValues(alpha: 0.9),
                size: size * 0.45,
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// 渐变背景容器 - 深邃极光
class AuroraBackground extends StatelessWidget {
  final Widget child;

  const AuroraBackground({
    super.key,
    required this.child,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            Color(0xFF0A0E1A), // 深邃午夜
            Color(0xFF0D1220),
            Color(0xFF111628),
            Color(0xFF0A0E1A),
          ],
          stops: [0.0, 0.3, 0.7, 1.0],
        ),
      ),
      child: Stack(
        children: [
          // 背景极光效果 - 左上
          Positioned(
            top: -100,
            left: -100,
            width: 400,
            height: 400,
            child: Container(
              decoration: BoxDecoration(
                gradient: RadialGradient(
                  center: Alignment.center,
                  radius: 0.5,
                  colors: [
                    const Color(0xFF7B61FF).withValues(alpha: 0.15),
                    Colors.transparent,
                  ],
                ),
              ),
            ),
          ),

          // 背景极光效果 - 右下
          Positioned(
            bottom: -150,
            right: -100,
            width: 500,
            height: 500,
            child: Container(
              decoration: BoxDecoration(
                gradient: RadialGradient(
                  center: Alignment.center,
                  radius: 0.5,
                  colors: [
                    const Color(0xFF00D4AA).withValues(alpha: 0.1),
                    Colors.transparent,
                  ],
                ),
              ),
            ),
          ),

          // 内容
          child,
        ],
      ),
    );
  }
}

/// 高级玻璃卡片 - 兼容旧版接口
class GlassCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? margin;
  final BorderRadius? borderRadius;
  final double blur;
  final Color? backgroundColor;
  final List<BoxShadow>? boxShadow;
  final VoidCallback? onTap;
  final Border? border;

  const GlassCard({
    super.key,
    required this.child,
    this.padding,
    this.margin,
    this.borderRadius,
    this.blur = 20.0,
    this.backgroundColor,
    this.boxShadow,
    this.onTap,
    this.border,
  });

  @override
  Widget build(BuildContext context) {
    return LiquidGlassCard(
      padding: padding,
      margin: margin,
      borderRadius: borderRadius,
      blur: blur,
      backgroundColor: backgroundColor,
      boxShadow: boxShadow,
      onTap: onTap,
      border: border,
      child: child,
    );
  }
}

/// 玻璃拟态按钮 - 兼容旧版
class GlassButton extends StatelessWidget {
  final VoidCallback onPressed;
  final Widget child;
  final Color? color;
  final double? width;
  final double? height;

  const GlassButton({
    super.key,
    required this.onPressed,
    required this.child,
    this.color,
    this.width,
    this.height,
  });

  @override
  Widget build(BuildContext context) {
    final gradientColors =
        color != null ? [color!, color!.withValues(alpha: 0.8)] : null;

    return LiquidGlassButton(
      onPressed: onPressed,
      gradientColors: gradientColors,
      width: width,
      height: height,
      child: child,
    );
  }
}

/// 玻璃拟态圆形头像 - 兼容旧版
class GlassAvatar extends StatelessWidget {
  final IconData icon;
  final Color color;
  final double size;

  const GlassAvatar({
    super.key,
    required this.icon,
    required this.color,
    this.size = 48,
  });

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(size / 2),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 15, sigmaY: 15),
        child: Container(
          width: size,
          height: size,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: color.withValues(alpha: 0.15),
            border: Border.all(
              color: Colors.white.withValues(alpha: 0.2),
              width: 1,
            ),
          ),
          child: Icon(
            icon,
            color: color,
            size: size * 0.4,
          ),
        ),
      ),
    );
  }
}

/// 渐变背景容器 - 兼容旧版
class GradientBackground extends StatelessWidget {
  final Widget child;
  final List<Color>? colors;

  const GradientBackground({
    super.key,
    required this.child,
    this.colors,
  });

  @override
  Widget build(BuildContext context) {
    return AuroraBackground(child: child);
  }
}

/// 霓虹发光文字
class NeonText extends StatelessWidget {
  final String text;
  final double fontSize;
  final FontWeight fontWeight;
  final Color color;

  const NeonText({
    super.key,
    required this.text,
    this.fontSize = 24,
    this.fontWeight = FontWeight.bold,
    this.color = Colors.white,
  });

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      style: TextStyle(
        fontSize: fontSize,
        fontWeight: fontWeight,
        color: color,
        shadows: [
          Shadow(
            color: color.withValues(alpha: 0.5),
            blurRadius: 20,
          ),
          Shadow(
            color: color.withValues(alpha: 0.3),
            blurRadius: 40,
          ),
        ],
      ),
    );
  }
}
