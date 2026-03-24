import 'package:flutter/material.dart';

/// AppShadows - 阴影系统
///
/// 浅色主题使用阴影，深色主题使用边框/微弱发光
class AppShadows {
  static List<BoxShadow> get lightCard => [
        const BoxShadow(
          color: Color(0x1A000000),
          blurRadius: 8,
          offset: Offset(0, 2),
        ),
      ];

  static List<BoxShadow> get lightElevated => [
        const BoxShadow(
          color: Color(0x26000000),
          blurRadius: 16,
          offset: Offset(0, 4),
        ),
      ];

  static List<BoxShadow> get none => [];
}
