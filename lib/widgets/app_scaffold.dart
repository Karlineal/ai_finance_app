import 'package:flutter/material.dart';
import '../theme/theme.dart';

/// AppScaffold - 统一页面 Scaffold
///
/// 统一背景色、安全区处理、页面 padding
class AppScaffold extends StatelessWidget {
  final String? title;
  final Widget body;
  final List<Widget>? actions;
  final Widget? floatingActionButton;
  final Widget? bottomNavigationBar;
  final bool extendBody;
  final PreferredSizeWidget? appBar;
  final bool showAppBar;

  const AppScaffold({
    super.key,
    this.title,
    required this.body,
    this.actions,
    this.floatingActionButton,
    this.bottomNavigationBar,
    this.extendBody = false,
    this.appBar,
    this.showAppBar = true,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);

    return Scaffold(
      backgroundColor: colors.backgroundPrimary,
      extendBody: extendBody,
      appBar: showAppBar
          ? (appBar ??
              (title != null
                  ? AppBar(
                      title: Text(title!),
                      actions: actions,
                    )
                  : null))
          : null,
      body: SafeArea(
        bottom: bottomNavigationBar == null,
        child: body,
      ),
      floatingActionButton: floatingActionButton,
      bottomNavigationBar: bottomNavigationBar,
    );
  }
}
