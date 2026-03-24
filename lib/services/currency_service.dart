import 'package:shared_preferences/shared_preferences.dart';
import '../models/currency.dart';

/// 货币设置服务
class CurrencyService {
  static const String _defaultCurrencyKey = 'default_currency';
  static const String _enableMultiCurrencyKey = 'enable_multi_currency';

  static final CurrencyService _instance = CurrencyService._internal();
  factory CurrencyService() => _instance;
  CurrencyService._internal();

  /// 获取默认货币
  Future<Currency> getDefaultCurrency() async {
    final prefs = await SharedPreferences.getInstance();
    final code = prefs.getString(_defaultCurrencyKey) ?? 'CNY';
    return getCurrencyByCode(code);
  }

  /// 设置默认货币
  Future<void> setDefaultCurrency(String code) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_defaultCurrencyKey, code);
  }

  /// 是否启用多币种
  Future<bool> isMultiCurrencyEnabled() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_enableMultiCurrencyKey) ?? false;
  }

  /// 设置多币种启用状态
  Future<void> setMultiCurrencyEnabled(bool enabled) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_enableMultiCurrencyKey, enabled);
  }

  /// 转换金额
  double convert(double amount, String fromCode, String toCode) {
    if (fromCode == toCode) return amount;

    final fromCurrency = getCurrencyByCode(fromCode);
    final toCurrency = getCurrencyByCode(toCode);

    // 先转为CNY，再转为目标货币
    final amountInCNY = amount / fromCurrency.exchangeRate;
    return amountInCNY * toCurrency.exchangeRate;
  }

  /// 格式化金额显示
  String formatAmount(double amount, String currencyCode) {
    final currency = getCurrencyByCode(currencyCode);
    return '${currency.symbol} ${amount.toStringAsFixed(2)}';
  }
}
