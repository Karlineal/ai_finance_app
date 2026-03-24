import 'package:flutter/material.dart';
import '../theme/theme.dart';
import '../widgets/widgets.dart';
import 'subscription_screen.dart';
import 'savings_plan_screen.dart';
import 'currency_settings_screen.dart';

/// 个人中心页面
class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    return Scaffold(
      backgroundColor: colors.backgroundPrimary,
      body: SafeArea(
        child: CustomScrollView(
          slivers: [
            // 顶部标题
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.all(AppSpacing.lg),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      '我的',
                      style: textStyles.titleLarge,
                    ),
                    IconButton(
                      onPressed: () {},
                      icon: const Icon(Icons.settings_outlined),
                      color: colors.textSecondary,
                    ),
                  ],
                ),
              ),
            ),

            // 用户信息卡片
            const SliverToBoxAdapter(
              child: ProfileHeader(
                name: 'iCookie 用户',
                subtitle: '记录美好生活',
              ),
            ),

            const SliverToBoxAdapter(
              child: SizedBox(height: AppSpacing.xl),
            ),

            // 功能菜单
            SliverToBoxAdapter(
              child: _buildSection(
                title: '功能',
                children: [
                  ProfileMenuItem(
                    icon: Icons.subscriptions_outlined,
                    title: '订阅与分期',
                    subtitle: '管理你的订阅服务和分期付款',
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => const SubscriptionScreen(),
                        ),
                      );
                    },
                  ),
                  ProfileMenuItem(
                    icon: Icons.savings_outlined,
                    title: '存钱计划',
                    subtitle: '设定储蓄目标，积少成多',
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => const SavingsPlanScreen(),
                        ),
                      );
                    },
                  ),
                  ProfileMenuItem(
                    icon: Icons.category_outlined,
                    title: '分类管理',
                    subtitle: '自定义收支分类',
                    onTap: () {},
                  ),
                  ProfileMenuItem(
                    icon: Icons.backup_outlined,
                    title: '数据备份',
                    subtitle: '导出/导入记账数据',
                    onTap: () {},
                  ),
                ],
              ),
            ),

            // 设置菜单
            SliverToBoxAdapter(
              child: _buildSection(
                title: '设置',
                children: [
                  ProfileMenuItem(
                    icon: Icons.notifications_outlined,
                    title: '记账提醒',
                    subtitle: '每日提醒记账，养成好习惯',
                    onTap: () {},
                  ),
                  ProfileMenuItem(
                    icon: Icons.security_outlined,
                    title: '隐私保护',
                    subtitle: '密码锁、后台模糊',
                    onTap: () {},
                  ),
                  ProfileMenuItem(
                    icon: Icons.palette_outlined,
                    title: '主题设置',
                    subtitle: '切换浅色/深色模式',
                    onTap: () {},
                  ),
                  ProfileMenuItem(
                    icon: Icons.currency_exchange_outlined,
                    title: '货币设置',
                    subtitle: '切换默认货币、汇率转换',
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => const CurrencySettingsScreen(),
                        ),
                      );
                    },
                  ),
                ],
              ),
            ),

            // 版本信息
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.all(AppSpacing.lg),
                child: Center(
                  child: Text(
                    'iCookie v1.0.0',
                    style: textStyles.caption.copyWith(
                      color: colors.textTertiary,
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSection({
    required String title,
    required List<Widget> children,
  }) {
    return Builder(
      builder: (context) {
        final colors = AppColors.of(context);
        final textStyles = AppTextStyles.of(context);

        return Padding(
          padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: textStyles.bodyLarge.copyWith(
                  fontWeight: FontWeight.w600,
                  color: colors.textSecondary,
                ),
              ),
              const SizedBox(height: AppSpacing.md),
              ...children,
              const SizedBox(height: AppSpacing.xl),
            ],
          ),
        );
      },
    );
  }
}
