import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../models/currency.dart';
import '../services/currency_service.dart';
import '../theme/theme.dart';
import '../widgets/widgets.dart';

/// 货币设置页面
class CurrencySettingsScreen extends StatefulWidget {
  const CurrencySettingsScreen({super.key});

  @override
  State<CurrencySettingsScreen> createState() => _CurrencySettingsScreenState();
}

class _CurrencySettingsScreenState extends State<CurrencySettingsScreen> {
  String _defaultCurrencyCode = 'CNY';
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    final currency = await CurrencyService().getDefaultCurrency();
    setState(() {
      _defaultCurrencyCode = currency.code;
      _isLoading = false;
    });
  }

  Future<void> _setDefaultCurrency(String code) async {
    await CurrencyService().setDefaultCurrency(code);
    setState(() => _defaultCurrencyCode = code);
    HapticFeedback.mediumImpact();
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('默认货币已更新')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    return Scaffold(
      backgroundColor: colors.backgroundPrimary,
      appBar: AppBar(
        backgroundColor: colors.backgroundPrimary,
        elevation: 0,
        title: Text(
          '货币设置',
          style: textStyles.titleSmall.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        centerTitle: true,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : ListView.builder(
              padding: const EdgeInsets.all(AppSpacing.lg),
              itemCount: supportedCurrencies.length,
              itemBuilder: (context, index) {
                final currency = supportedCurrencies[index];
                final isSelected = currency.code == _defaultCurrencyCode;
                return CurrencyItem(
                  currency: currency,
                  isSelected: isSelected,
                  onTap: () => _setDefaultCurrency(currency.code),
                );
              },
            ),
    );
  }
}
