import 'package:flutter/material.dart';

/// 页面转场动画
class AppPageTransitions {
  /// 标准页面进入动画 - 从右向左滑入
  static Route<T> slideFromRight<T>({
    required Widget page,
    RouteSettings? settings,
  }) {
    return PageRouteBuilder(
      settings: settings,
      pageBuilder: (context, animation, secondaryAnimation) => page,
      transitionsBuilder: (context, animation, secondaryAnimation, child) {
        const begin = Offset(1.0, 0.0);
        const end = Offset.zero;
        const curve = Curves.easeOutCubic;

        var tween = Tween(begin: begin, end: end).chain(
          CurveTween(curve: curve),
        );

        return SlideTransition(
          position: animation.drive(tween),
          child: child,
        );
      },
      transitionDuration: const Duration(milliseconds: 250),
    );
  }

  /// 底部弹窗动画 - 从下向上滑入
  static Route<T> slideFromBottom<T>({
    required Widget page,
    RouteSettings? settings,
  }) {
    return PageRouteBuilder(
      settings: settings,
      pageBuilder: (context, animation, secondaryAnimation) => page,
      transitionsBuilder: (context, animation, secondaryAnimation, child) {
        const begin = Offset(0.0, 1.0);
        const end = Offset.zero;
        const curve = Curves.easeOutCubic;

        var tween = Tween(begin: begin, end: end).chain(
          CurveTween(curve: curve),
        );

        return SlideTransition(
          position: animation.drive(tween),
          child: child,
        );
      },
      transitionDuration: const Duration(milliseconds: 300),
    );
  }

  /// 淡入淡出动画
  static Route<T> fade<T>({
    required Widget page,
    RouteSettings? settings,
  }) {
    return PageRouteBuilder(
      settings: settings,
      pageBuilder: (context, animation, secondaryAnimation) => page,
      transitionsBuilder: (context, animation, secondaryAnimation, child) {
        return FadeTransition(
          opacity: animation,
          child: child,
        );
      },
      transitionDuration: const Duration(milliseconds: 200),
    );
  }

  /// 缩放进入动画
  static Route<T> scale<T>({
    required Widget page,
    RouteSettings? settings,
  }) {
    return PageRouteBuilder(
      settings: settings,
      pageBuilder: (context, animation, secondaryAnimation) => page,
      transitionsBuilder: (context, animation, secondaryAnimation, child) {
        var tween = Tween(begin: 0.95, end: 1.0).chain(
          CurveTween(curve: Curves.easeOutCubic),
        );

        return FadeTransition(
          opacity: animation,
          child: ScaleTransition(
            scale: animation.drive(tween),
            child: child,
          ),
        );
      },
      transitionDuration: const Duration(milliseconds: 200),
    );
  }
}

/// 便捷导航扩展
extension AppNavigation on BuildContext {
  /// 带滑动动画的页面跳转
  Future<T?> pushWithSlide<T>(Widget page) {
    return Navigator.of(this).push(
      AppPageTransitions.slideFromRight(page: page),
    );
  }

  /// 带底部弹窗动画的页面跳转
  Future<T?> pushFromBottom<T>(Widget page) {
    return Navigator.of(this).push(
      AppPageTransitions.slideFromBottom(page: page),
    );
  }

  /// 带淡入动画的页面跳转
  Future<T?> pushWithFade<T>(Widget page) {
    return Navigator.of(this).push(
      AppPageTransitions.fade(page: page),
    );
  }
}
