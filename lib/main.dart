import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:provider/provider.dart';
import 'bloc/transaction_bloc.dart';
import 'theme/theme.dart';
import 'screens/main_navigation_screen.dart';

// 全局导航键
final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // 初始化主题
  final themeProvider = ThemeProvider();
  await themeProvider.setThemeMode(ThemeMode.system);

  runApp(
    MultiBlocProvider(
      providers: [
        BlocProvider(
          create: (_) => TransactionBloc()..add(const LoadTransactions()),
        ),
      ],
      child: ChangeNotifierProvider.value(
        value: themeProvider,
        child: const MyApp(),
      ),
    ),
  );
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<ThemeProvider>(
      builder: (context, themeProvider, child) {
        // 根据当前主题设置系统UI样式
        final isDark = themeProvider.getBrightness(
              MediaQuery.platformBrightnessOf(context),
            ) ==
            Brightness.dark;

        SystemChrome.setSystemUIOverlayStyle(
          SystemUiOverlayStyle(
            statusBarColor: Colors.transparent,
            statusBarIconBrightness:
                isDark ? Brightness.light : Brightness.dark,
            systemNavigationBarColor:
                isDark ? const Color(0xFF1C1C1E) : const Color(0xFFF8F5F0),
            systemNavigationBarIconBrightness:
                isDark ? Brightness.light : Brightness.dark,
          ),
        );

        return MaterialApp(
          navigatorKey: navigatorKey,
          title: 'iCookie',
          debugShowCheckedModeBanner: false,
          theme: AppTheme.light,
          darkTheme: AppTheme.dark,
          themeMode: themeProvider.themeMode,
          home: const MainNavigationScreen(),
        );
      },
    );
  }
}

/// 兼容旧代码的 AppColors 全局访问
///
/// 将逐步废弃，请使用 AppColors.of(context)
@Deprecated('Use AppColors.of(context) instead')
class AppColors {
  static Color get primary => const Color(0xFFD4A373);
  static Color get secondary => const Color(0xFF7CB69D);
  static Color get expense => const Color(0xFFE07A5F);
  static Color get background => const Color(0xFFF8F5F0);
  static Color get card => Colors.white;
  static Color get surface => const Color(0xFFF5F0E8);
  static Color get textPrimary => const Color(0xFF2D3436);
  static Color get textSecondary => const Color(0xFF636E72);
  static Color get textTertiary => const Color(0xFFB2BEC3);
  static Color get divider => const Color(0xFFDFE6E9);
  static Color get shadow => const Color(0x1A000000);
}
