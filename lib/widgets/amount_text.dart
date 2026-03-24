import 'package:flutter/material.dart';
import '../theme/theme.dart';

/// AmountText - 金额显示组件
///
/// 自动根据收入/支出变色
/// 支持不同 size
class AmountText extends StatelessWidget {
  final double amount;
  final bool isExpense;
  final TextStyle? style;
  final String? prefix;
  final int decimalPlaces;

  const AmountText({
    super.key,
    required this.amount,
    required this.isExpense,
    this.style,
    this.prefix,
    this.decimalPlaces = 2,
  });

  /// 快捷构造器 - 支出
  factory AmountText.expense(
    double amount, {
    TextStyle? style,
    String? prefix,
    int decimalPlaces = 2,
  }) {
    return AmountText(
      amount: amount,
      isExpense: true,
      style: style,
      prefix: prefix,
      decimalPlaces: decimalPlaces,
    );
  }

  /// 快捷构造器 - 收入
  factory AmountText.income(
    double amount, {
    TextStyle? style,
    String? prefix,
    int decimalPlaces = 2,
  }) {
    return AmountText(
      amount: amount,
      isExpense: false,
      style: style,
      prefix: prefix,
      decimalPlaces: decimalPlaces,
    );
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    final color = isExpense ? colors.expense : colors.income;
    final sign = isExpense ? '-' : '+';
    final prefixStr = prefix ?? '';

    return Text(
      '$sign$prefixStr${amount.toStringAsFixed(decimalPlaces)}',
      style: style?.copyWith(color: color) ??
          textStyles.amountMedium.copyWith(color: color),
    );
  }
}
