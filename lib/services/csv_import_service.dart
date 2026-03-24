import 'dart:convert';
import 'dart:io';
import 'package:csv/csv.dart';
import '../models/transaction.dart';
import 'ai_service.dart';

/// 导入来源
enum ImportSource { alipay, wechat }

/// CSV 导入服务 - 支持支付宝和微信账单导入
class CSVImportService {
  static final CSVImportService _instance = CSVImportService._internal();
  factory CSVImportService() => _instance;
  CSVImportService._internal();

  final AIService _aiService = AIService();

  /// 导入结果
  Future<ImportResult> importFromFile({
    required ImportSource source,
    required String filePath,
    bool autoCategorize = true,
  }) async {
    try {
      final file = File(filePath);
      if (!await file.exists()) {
        return ImportResult.error('文件不存在');
      }

      final bytes = await file.readAsBytes();
      return await importFromBytes(
        source: source,
        bytes: bytes,
        autoCategorize: autoCategorize,
      );
    } catch (e) {
      return ImportResult.error('读取文件失败: $e');
    }
  }

  /// 从字节导入
  Future<ImportResult> importFromBytes({
    required ImportSource source,
    required List<int> bytes,
    bool autoCategorize = true,
  }) async {
    try {
      List<Transaction> transactions = [];

      switch (source) {
        case ImportSource.alipay:
          transactions = await _parseAlipayCSV(bytes);
          break;
        case ImportSource.wechat:
          transactions = await _parseWechatCSV(bytes);
          break;
      }

      // 自动分类
      if (autoCategorize) {
        transactions = await _categorizeTransactions(transactions);
      }

      return ImportResult.success(
        transactions: transactions,
        imported: transactions.length,
      );
    } catch (e) {
      return ImportResult.error('解析失败: $e');
    }
  }

  /// 解析支付宝 CSV
  Future<List<Transaction>> _parseAlipayCSV(List<int> bytes) async {
    // 支付宝 CSV 通常是 UTF-8 编码
    String content;
    try {
      content = utf8.decode(bytes);
    } catch (e) {
      // 尝试 GBK
      content = latin1.decode(bytes);
    }

    // 跳过前4行（支付宝 CSV 有标题信息）
    final lines = content.split('\n');
    if (lines.length <= 4) {
      return [];
    }

    final csvContent = lines.skip(4).join('\n');
    final rows = const CsvToListConverter().convert(csvContent);

    final transactions = <Transaction>[];

    for (var i = 0; i < rows.length; i++) {
      final row = rows[i];
      if (row.isEmpty) continue;

      try {
        // 支付宝 CSV 列索引（根据实际格式调整）
        final transactionId = row.isNotEmpty ? row[0].toString() : '';
        final createTime = row.length > 2 ? row[2].toString() : '';
        final counterparty = row.length > 7 ? row[7].toString() : '';
        final productName = row.length > 8 ? row[8].toString() : '';
        final amountStr = row.length > 9 ? row[9].toString() : '0';
        final direction = row.length > 10 ? row[10].toString() : '';
        final status = row.length > 11 ? row[11].toString() : '';

        // 只导入交易成功的
        if (status != '交易成功') continue;

        final amount = double.tryParse(amountStr.replaceAll('¥', '').trim()) ?? 0;
        final isExpense = direction == '支出';

        final transaction = Transaction(
          id: 'alipay_$transactionId',
          date: _parseAlipayDate(createTime),
          payee: counterparty.isEmpty ? productName : counterparty,
          amount: isExpense ? -amount.abs() : amount.abs(),
          notes: productName,
          category: isExpense ? 'other_expense' : 'other_income',
          type: isExpense ? TransactionType.expense : TransactionType.income,
          source: 'alipay',
          createdAt: DateTime.now(),
          updatedAt: DateTime.now(),
        );

        transactions.add(transaction);
      } catch (e) {
        // 跳过解析失败的行
        continue;
      }
    }

    return transactions;
  }

  /// 解析微信 CSV
  Future<List<Transaction>> _parseWechatCSV(List<int> bytes) async {
    // 微信 CSV 使用 GBK 编码
    String content;
    try {
      // 尝试 UTF-8
      content = utf8.decode(bytes);
    } catch (e) {
      // 回退到 GBK (使用 latin1 作为近似)
      content = latin1.decode(bytes);
    }

    // 跳过前16行（微信 CSV 有标题和说明）
    final lines = content.split('\n');
    if (lines.length <= 16) {
      return [];
    }

    final csvContent = lines.skip(16).join('\n');
    final rows = const CsvToListConverter().convert(csvContent);

    final transactions = <Transaction>[];

    for (var i = 0; i < rows.length; i++) {
      final row = rows[i];
      if (row.isEmpty) continue;

      try {
        // 微信 CSV 列索引（根据实际格式调整）
        final createTime = row.isNotEmpty ? row[0].toString() : '';
        final counterparty = row.length > 2 ? row[2].toString() : '';
        final productName = row.length > 3 ? row[3].toString() : '';
        final direction = row.length > 4 ? row[4].toString() : '';
        final amountStr = row.length > 5 ? row[5].toString() : '0';
        final status = row.length > 7 ? row[7].toString() : '';
        final transactionId = row.length > 8 ? row[8].toString() : '';

        // 只导入支付成功的
        if (status != '支付成功' && status != '已存入零钱') continue;

        final amount = double.tryParse(
          amountStr.replaceAll('¥', '').replaceAll(',', '').trim()
        ) ?? 0;
        final isExpense = direction == '支出';

        final transaction = Transaction(
          id: 'wechat_$transactionId',
          date: _parseWechatDate(createTime),
          payee: counterparty.isEmpty ? productName : counterparty,
          amount: isExpense ? -amount.abs() : amount.abs(),
          notes: productName,
          category: isExpense ? 'other_expense' : 'other_income',
          type: isExpense ? TransactionType.expense : TransactionType.income,
          source: 'wechat',
          createdAt: DateTime.now(),
          updatedAt: DateTime.now(),
        );

        transactions.add(transaction);
      } catch (e) {
        // 跳过解析失败的行
        continue;
      }
    }

    return transactions;
  }

  /// 自动分类交易
  Future<List<Transaction>> _categorizeTransactions(List<Transaction> transactions) async {
    final categorized = <Transaction>[];

    for (final transaction in transactions) {
      try {
        final result = await _aiService.categorizeTransaction(
          payee: transaction.payee,
          amount: transaction.amount.abs(),
          notes: transaction.notes,
          isExpense: transaction.isExpense,
        );

        categorized.add(transaction.copyWith(
          category: result.categoryId,
        ));
      } catch (e) {
        // 分类失败，使用默认分类
        categorized.add(transaction);
      }
    }

    return categorized;
  }

  /// 解析支付宝日期格式
  DateTime _parseAlipayDate(String dateStr) {
    try {
      // 格式: 2024-01-15 14:30:25
      final normalized = dateStr.replaceAll('/', '-');
      return DateTime.parse(normalized);
    } catch (e) {
      return DateTime.now();
    }
  }

  /// 解析微信日期格式
  DateTime _parseWechatDate(String dateStr) {
    try {
      // 格式: 2024-01-15 14:30:25
      final normalized = dateStr.replaceAll('/', '-');
      return DateTime.parse(normalized);
    } catch (e) {
      return DateTime.now();
    }
  }

  /// 检测导入源（根据文件内容自动识别）
  Future<ImportSource?> detectSource(String filePath) async {
    try {
      final file = File(filePath);
      final content = await file.readAsString();

      if (content.contains('支付宝') || content.contains('Alipay')) {
        return ImportSource.alipay;
      } else if (content.contains('微信') || content.contains('WeChat')) {
        return ImportSource.wechat;
      }

      return null;
    } catch (e) {
      return null;
    }
  }

  /// 获取导入源名称
  String getSourceName(ImportSource source) {
    switch (source) {
      case ImportSource.alipay:
        return '支付宝';
      case ImportSource.wechat:
        return '微信支付';
    }
  }
}

/// 导入结果
class ImportResult {
  final bool success;
  final int imported;
  final int skipped;
  final List<Transaction> transactions;
  final String? error;

  const ImportResult({
    required this.success,
    required this.imported,
    this.skipped = 0,
    this.transactions = const [],
    this.error,
  });

  factory ImportResult.success({
    required List<Transaction> transactions,
    required int imported,
    int skipped = 0,
  }) {
    return ImportResult(
      success: true,
      imported: imported,
      skipped: skipped,
      transactions: transactions,
    );
  }

  factory ImportResult.error(String error) {
    return ImportResult(
      success: false,
      imported: 0,
      error: error,
    );
  }
}
