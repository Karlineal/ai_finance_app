import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../theme/theme.dart';

/// 自定义数字键盘
///
/// 固定底部，Cookie记账风格
class CustomNumericKeyboard extends StatelessWidget {
  final Function(String) onKeyPressed;
  final VoidCallback onBackspace;
  final VoidCallback onSubmit;
  final VoidCallback? onDatePressed;
  final String submitText;

  const CustomNumericKeyboard({
    super.key,
    required this.onKeyPressed,
    required this.onBackspace,
    required this.onSubmit,
    this.onDatePressed,
    this.submitText = '记一笔',
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);

    return Container(
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: colors.backgroundPrimary,
        border: Border(
          top: BorderSide(color: colors.divider),
        ),
      ),
      child: SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // 第一行：7 8 9 日期
            _buildRow([
              _buildKey('7'),
              _buildKey('8'),
              _buildKey('9'),
              _buildActionKey(
                icon: Icons.calendar_today,
                label: '今天',
                onTap: onDatePressed ?? () {},
              ),
            ]),
            const SizedBox(height: AppSpacing.sm),
            // 第二行：4 5 6 +
            _buildRow([
              _buildKey('4'),
              _buildKey('5'),
              _buildKey('6'),
              _buildKey('+'),
            ]),
            const SizedBox(height: AppSpacing.sm),
            // 第三行：1 2 3 -
            _buildRow([
              _buildKey('1'),
              _buildKey('2'),
              _buildKey('3'),
              _buildKey('-'),
            ]),
            const SizedBox(height: AppSpacing.sm),
            // 第四行：. 0 ⌫ 确认
            _buildRow([
              _buildKey('.'),
              _buildKey('0'),
              _buildBackspaceKey(),
              _buildSubmitKey(),
            ]),
          ],
        ),
      ),
    );
  }

  Widget _buildRow(List<Widget> children) {
    return Row(
      children: children.map((child) => Expanded(child: child)).toList(),
    );
  }

  Widget _buildKey(String value) {
    return KeyboardKey(
      value: value,
      onTap: () {
        HapticFeedback.lightImpact();
        onKeyPressed(value);
      },
    );
  }

  Widget _buildBackspaceKey() {
    return KeyboardKey(
      value: '⌫',
      onTap: () {
        HapticFeedback.lightImpact();
        onBackspace();
      },
      isAction: true,
    );
  }

  Widget _buildActionKey({
    required IconData icon,
    required String label,
    required VoidCallback onTap,
  }) {
    return KeyboardKey(
      value: label,
      onTap: () {
        HapticFeedback.lightImpact();
        onTap();
      },
      isAction: true,
    );
  }

  Widget _buildSubmitKey() {
    return KeyboardKey(
      value: submitText,
      onTap: () {
        HapticFeedback.mediumImpact();
        onSubmit();
      },
      isSubmit: true,
    );
  }
}

/// 键盘按键
class KeyboardKey extends StatelessWidget {
  final String value;
  final VoidCallback onTap;
  final bool isAction;
  final bool isSubmit;

  const KeyboardKey({
    super.key,
    required this.value,
    required this.onTap,
    this.isAction = false,
    this.isSubmit = false,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 56,
        margin: const EdgeInsets.symmetric(horizontal: AppSpacing.xs),
        decoration: BoxDecoration(
          color: isSubmit
              ? colors.brandPrimary
              : isAction
                  ? colors.backgroundSecondary
                  : colors.cardPrimary,
          borderRadius: BorderRadius.circular(AppRadius.md),
          boxShadow: [
            if (!isSubmit)
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.05),
                blurRadius: 4,
                offset: const Offset(0, 2),
              ),
          ],
        ),
        child: Center(
          child: Text(
            value,
            style: isSubmit
                ? textStyles.bodyLarge.copyWith(
                    color: Colors.white,
                    fontWeight: FontWeight.w600,
                  )
                : isAction
                    ? textStyles.bodyMedium
                    : textStyles.bodyLarge.copyWith(
                        fontSize: 22,
                        fontWeight: FontWeight.w500,
                      ),
          ),
        ),
      ),
    );
  }
}
