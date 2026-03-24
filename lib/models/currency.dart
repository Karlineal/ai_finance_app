import 'package:equatable/equatable.dart';

/// 货币模型
class Currency extends Equatable {
  final String code;
  final String name;
  final String symbol;
  final String flag;
  final double exchangeRate;

  const Currency({
    required this.code,
    required this.name,
    required this.symbol,
    required this.flag,
    required this.exchangeRate,
  });

  double toCNY(double amount) => amount / exchangeRate;
  double fromCNY(double cnyAmount) => cnyAmount * exchangeRate;

  Map<String, dynamic> toMap() => {
        'code': code,
        'name': name,
        'symbol': symbol,
        'flag': flag,
        'exchangeRate': exchangeRate,
      };

  factory Currency.fromMap(Map<String, dynamic> map) => Currency(
        code: map['code'] as String,
        name: map['name'] as String,
        symbol: map['symbol'] as String,
        flag: map['flag'] as String,
        exchangeRate: (map['exchangeRate'] as num).toDouble(),
      );

  @override
  List<Object?> get props => [code, name, symbol, flag, exchangeRate];
}

final List<Currency> supportedCurrencies = [
  const Currency(
      code: 'CNY', name: '人民币', symbol: 'CNY', flag: 'CN', exchangeRate: 1.0),
  const Currency(
      code: 'USD', name: '美元', symbol: 'USD', flag: 'US', exchangeRate: 0.14),
  const Currency(
      code: 'EUR', name: '欧元', symbol: 'EUR', flag: 'EU', exchangeRate: 0.13),
  const Currency(
      code: 'JPY', name: '日元', symbol: 'JPY', flag: 'JP', exchangeRate: 21.5),
  const Currency(
      code: 'GBP', name: '英镑', symbol: 'GBP', flag: 'GB', exchangeRate: 0.11),
  const Currency(
      code: 'HKD', name: '港币', symbol: 'HKD', flag: 'HK', exchangeRate: 1.09),
  const Currency(
      code: 'KRW', name: '韩元', symbol: 'KRW', flag: 'KR', exchangeRate: 186.0),
  const Currency(
      code: 'AUD', name: '澳元', symbol: 'AUD', flag: 'AU', exchangeRate: 0.21),
  const Currency(
      code: 'CAD', name: '加元', symbol: 'CAD', flag: 'CA', exchangeRate: 0.19),
  const Currency(
      code: 'SGD', name: '新加坡元', symbol: 'SGD', flag: 'SG', exchangeRate: 0.19),
  const Currency(
      code: 'THB', name: '泰铢', symbol: 'THB', flag: 'TH', exchangeRate: 5.0),
  const Currency(
      code: 'TWD', name: '新台币', symbol: 'TWD', flag: 'TW', exchangeRate: 4.5),
];

Currency getCurrencyByCode(String code) {
  return supportedCurrencies.firstWhere(
    (c) => c.code == code,
    orElse: () => supportedCurrencies.first,
  );
}
