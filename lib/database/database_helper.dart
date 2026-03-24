import 'package:path/path.dart';
import 'package:sqflite/sqflite.dart';
import '../models/transaction.dart' as app_models;
import '../models/subscription.dart';
import 'package:flutter/foundation.dart' show kIsWeb;

/// Web 平台内存数据库实现
class DatabaseHelper {
  static final DatabaseHelper instance = DatabaseHelper._init();
  static Database? _database;

  // Web 平台内存存储
  static final Map<String, Map<String, dynamic>> _webTransactions = {};
  static final Map<String, Map<String, dynamic>> _webCategories = {};
  static final Map<String, Map<String, dynamic>> _webSubscriptions = {};
  static bool _webInitialized = false;

  DatabaseHelper._init();

  Future<dynamic> get database async {
    if (kIsWeb) {
      await _initWebStorage();
      return null;
    }
    if (_database != null) return _database!;
    _database = await _initDB('ai_finance.db');
    return _database!;
  }

  Future<dynamic> _initDB(String filePath) async {
    // Web 平台使用内存存储
    if (kIsWeb) {
      await _initWebStorage();
      return null;
    }

    // 移动端使用 SQLite
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, filePath);

    return await openDatabase(
      path,
      version: 2,
      onCreate: _createDB,
      onUpgrade: _onUpgrade,
    );
  }

  Future<void> _initWebStorage() async {
    if (_webInitialized) return;

    // 初始化默认分类
    for (final category in app_models.defaultCategories) {
      _webCategories[category.id] = category.toMap();
    }
    _webInitialized = true;
  }

  Future _createDB(Database db, int version) async {
    await db.execute('''
      CREATE TABLE transactions (
        id TEXT PRIMARY KEY,
        date TEXT NOT NULL,
        payee TEXT NOT NULL,
        amount REAL NOT NULL,
        notes TEXT,
        category TEXT NOT NULL,
        type TEXT NOT NULL,
        source TEXT,
        createdAt TEXT NOT NULL,
        updatedAt TEXT NOT NULL,
        currency TEXT DEFAULT 'CNY'
      )
    ''');

    await db.execute('''
      CREATE TABLE categories (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        icon TEXT,
        color INTEGER NOT NULL,
        type TEXT NOT NULL,
        sortOrder INTEGER DEFAULT 0
      )
    ''');

    await _insertDefaultCategories(db);

    // 订阅/分期表
    await db.execute('''
      CREATE TABLE subscriptions (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        icon TEXT,
        amount REAL NOT NULL,
        type TEXT NOT NULL,
        frequency TEXT NOT NULL,
        frequencyValue INTEGER DEFAULT 1,
        startDate TEXT NOT NULL,
        endDate TEXT,
        category TEXT NOT NULL,
        notes TEXT,
        isActive INTEGER DEFAULT 1,
        createdAt TEXT NOT NULL,
        updatedAt TEXT NOT NULL
      )
    ''');
  }

  Future _onUpgrade(Database db, int oldVersion, int newVersion) async {
    if (oldVersion < 2) {
      // 添加货币字段到 transactions 表
      await db.execute(
          'ALTER TABLE transactions ADD COLUMN currency TEXT DEFAULT "CNY"');
    }
  }

  Future _insertDefaultCategories(dynamic db) async {
    if (kIsWeb) return;
    for (final category in app_models.defaultCategories) {
      await (db as Database).insert('categories', category.toMap());
    }
  }

  Future<List<app_models.Transaction>> getTransactions({
    DateTime? startDate,
    DateTime? endDate,
    String? category,
    app_models.TransactionType? type,
    int limit = 100,
    int offset = 0,
  }) async {
    // Web 平台
    if (kIsWeb) {
      var results = _webTransactions.values
          .map((m) => app_models.Transaction.fromMap(m))
          .toList();

      if (startDate != null) {
        results = results
            .where((t) =>
                t.date.isAfter(startDate) || t.date.isAtSameMomentAs(startDate))
            .toList();
      }
      if (endDate != null) {
        results = results
            .where((t) =>
                t.date.isBefore(endDate) || t.date.isAtSameMomentAs(endDate))
            .toList();
      }
      if (category != null) {
        results = results.where((t) => t.category == category).toList();
      }
      if (type != null) {
        results = results.where((t) => t.type == type).toList();
      }

      results.sort((a, b) => b.date.compareTo(a.date));
      return results.skip(offset).take(limit).toList();
    }

    // 移动端
    final db = await database;

    String whereClause = '1=1';
    List<Object?> whereArgs = [];

    if (startDate != null) {
      whereClause += ' AND date >= ?';
      whereArgs.add(startDate.toIso8601String());
    }

    if (endDate != null) {
      whereClause += ' AND date <= ?';
      whereArgs.add(endDate.toIso8601String());
    }

    if (category != null) {
      whereClause += ' AND category = ?';
      whereArgs.add(category);
    }

    if (type != null) {
      whereClause += ' AND type = ?';
      whereArgs.add(type.name);
    }

    final maps = await (db as Database).query(
      'transactions',
      where: whereClause,
      whereArgs: whereArgs,
      orderBy: 'date DESC',
      limit: limit,
      offset: offset,
    );

    return maps.map((map) => app_models.Transaction.fromMap(map)).toList();
  }

  Future<app_models.Transaction?> getTransaction(String id) async {
    if (kIsWeb) {
      final data = _webTransactions[id];
      return data != null ? app_models.Transaction.fromMap(data) : null;
    }

    final db = await database;
    final maps = await (db as Database).query(
      'transactions',
      where: 'id = ?',
      whereArgs: [id],
    );

    if (maps.isNotEmpty) {
      return app_models.Transaction.fromMap(maps.first);
    }
    return null;
  }

  Future<String> insertTransaction(app_models.Transaction transaction) async {
    if (kIsWeb) {
      _webTransactions[transaction.id] = transaction.toMap();
      return transaction.id;
    }

    final db = await database;
    await (db as Database).insert('transactions', transaction.toMap());
    return transaction.id;
  }

  Future<int> updateTransaction(app_models.Transaction transaction) async {
    if (kIsWeb) {
      _webTransactions[transaction.id] = transaction.toMap();
      return 1;
    }

    final db = await database;
    return await (db as Database).update(
      'transactions',
      transaction.toMap(),
      where: 'id = ?',
      whereArgs: [transaction.id],
    );
  }

  Future<int> deleteTransaction(String id) async {
    if (kIsWeb) {
      _webTransactions.remove(id);
      return 1;
    }

    final db = await database;
    return await (db as Database).delete(
      'transactions',
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  Future<List<app_models.Category>> getCategories(
      {app_models.TransactionType? type}) async {
    if (kIsWeb) {
      var results = _webCategories.values
          .map((m) => app_models.Category.fromMap(m))
          .toList();
      if (type != null) {
        results = results.where((c) => c.type == type).toList();
      }
      results.sort((a, b) => a.sortOrder.compareTo(b.sortOrder));
      return results;
    }

    final db = await database;

    String? where;
    List<Object?>? whereArgs;

    if (type != null) {
      where = 'type = ?';
      whereArgs = [type.name];
    }

    final maps = await (db as Database).query(
      'categories',
      where: where,
      whereArgs: whereArgs,
      orderBy: 'sortOrder ASC',
    );

    return maps.map((map) => app_models.Category.fromMap(map)).toList();
  }

  Future<Map<String, dynamic>> getMonthlyStats(int year, int month) async {
    final startDate = DateTime(year, month, 1);
    final endDate = DateTime(year, month + 1, 0);

    if (kIsWeb) {
      var transactions = _webTransactions.values
          .map((m) => app_models.Transaction.fromMap(m))
          .where((t) =>
              t.date.isAfter(startDate) &&
              t.date.isBefore(endDate.add(const Duration(days: 1))))
          .toList();

      final expenseTotal = transactions
          .where((t) => t.type == app_models.TransactionType.expense)
          .fold(0.0, (sum, t) => sum + t.amount.abs());
      final incomeTotal = transactions
          .where((t) => t.type == app_models.TransactionType.income)
          .fold(0.0, (sum, t) => sum + t.amount);

      return {
        'expense': expenseTotal,
        'income': incomeTotal,
        'balance': incomeTotal - expenseTotal,
      };
    }

    final db = await database;

    final expenseResult = await (db as Database).rawQuery('''
      SELECT COALESCE(SUM(amount), 0) as total
      FROM transactions
      WHERE type = 'expense'
      AND date >= ? AND date <= ?
    ''', [startDate.toIso8601String(), endDate.toIso8601String()]);

    final incomeResult = await (db).rawQuery('''
      SELECT COALESCE(SUM(amount), 0) as total
      FROM transactions
      WHERE type = 'income'
      AND date >= ? AND date <= ?
    ''', [startDate.toIso8601String(), endDate.toIso8601String()]);

    final expenseTotal = (expenseResult.first['total'] as num).toDouble();
    final incomeTotal = (incomeResult.first['total'] as num).toDouble();

    return {
      'expense': expenseTotal,
      'income': incomeTotal,
      'balance': incomeTotal - expenseTotal,
    };
  }

  Future<List<Map<String, dynamic>>> getCategoryStats(
    int year,
    int month,
    app_models.TransactionType type,
  ) async {
    final startDate = DateTime(year, month, 1);
    final endDate = DateTime(year, month + 1, 0);

    if (kIsWeb) {
      final transactions = _webTransactions.values
          .map((m) => app_models.Transaction.fromMap(m))
          .where((t) => t.type == type)
          .where((t) =>
              t.date.isAfter(startDate) &&
              t.date.isBefore(endDate.add(const Duration(days: 1))))
          .toList();

      final Map<String, Map<String, dynamic>> stats = {};
      for (final t in transactions) {
        if (!stats.containsKey(t.category)) {
          final cat = _webCategories[t.category];
          stats[t.category] = {
            'category': t.category,
            'categoryName': cat?['name'] ?? t.category,
            'categoryColor': cat?['color'] ?? 0xFF808080,
            'total': 0.0,
            'count': 0,
          };
        }
        stats[t.category]!['total'] =
            (stats[t.category]!['total'] as double) + t.amount.abs();
        stats[t.category]!['count'] = (stats[t.category]!['count'] as int) + 1;
      }

      final result = stats.values.toList();
      result.sort(
          (a, b) => (b['total'] as double).compareTo(a['total'] as double));
      return result;
    }

    final db = await database;

    final result = await (db as Database).rawQuery('''
      SELECT 
        t.category,
        c.name as categoryName,
        c.color as categoryColor,
        SUM(t.amount) as total,
        COUNT(*) as count
      FROM transactions t
      LEFT JOIN categories c ON t.category = c.id
      WHERE t.type = ?
      AND t.date >= ? AND t.date <= ?
      GROUP BY t.category
      ORDER BY total DESC
    ''', [type.name, startDate.toIso8601String(), endDate.toIso8601String()]);

    return result;
  }

  // ========== 订阅/分期管理 ==========

  Future<List<Subscription>> getSubscriptions({
    SubscriptionType? type,
    bool? isActive,
  }) async {
    // Web 平台
    if (kIsWeb) {
      var results =
          _webSubscriptions.values.map((m) => Subscription.fromMap(m)).toList();

      if (type != null) {
        results = results.where((s) => s.type == type).toList();
      }
      if (isActive != null) {
        results = results.where((s) => s.isActive == isActive).toList();
      }

      results.sort((a, b) => b.createdAt.compareTo(a.createdAt));
      return results;
    }

    // 移动端
    final db = await database;

    String whereClause = '1=1';
    List<Object?> whereArgs = [];

    if (type != null) {
      whereClause += ' AND type = ?';
      whereArgs.add(type.name);
    }
    if (isActive != null) {
      whereClause += ' AND isActive = ?';
      whereArgs.add(isActive ? 1 : 0);
    }

    final maps = await (db as Database).query(
      'subscriptions',
      where: whereClause,
      whereArgs: whereArgs,
      orderBy: 'createdAt DESC',
    );

    return maps.map((map) => Subscription.fromMap(map)).toList();
  }

  Future<Subscription?> getSubscription(String id) async {
    if (kIsWeb) {
      final data = _webSubscriptions[id];
      return data != null ? Subscription.fromMap(data) : null;
    }

    final db = await database;
    final maps = await (db as Database).query(
      'subscriptions',
      where: 'id = ?',
      whereArgs: [id],
    );

    if (maps.isNotEmpty) {
      return Subscription.fromMap(maps.first);
    }
    return null;
  }

  Future<String> insertSubscription(Subscription subscription) async {
    if (kIsWeb) {
      _webSubscriptions[subscription.id] = subscription.toMap();
      return subscription.id;
    }

    final db = await database;
    await (db as Database).insert('subscriptions', subscription.toMap());
    return subscription.id;
  }

  Future<int> updateSubscription(Subscription subscription) async {
    if (kIsWeb) {
      _webSubscriptions[subscription.id] = subscription.toMap();
      return 1;
    }

    final db = await database;
    return await (db as Database).update(
      'subscriptions',
      subscription.toMap(),
      where: 'id = ?',
      whereArgs: [subscription.id],
    );
  }

  Future<int> deleteSubscription(String id) async {
    if (kIsWeb) {
      _webSubscriptions.remove(id);
      return 1;
    }

    final db = await database;
    return await (db as Database).delete(
      'subscriptions',
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  // 获取本月即将扣费的订阅
  Future<List<Subscription>> getUpcomingSubscriptions() async {
    final now = DateTime.now();
    final endOfMonth = DateTime(now.year, now.month + 1, 0);

    final allSubscriptions = await getSubscriptions(isActive: true);

    return allSubscriptions.where((s) {
      final nextPayment = s.getNextPaymentDate();
      return nextPayment.isBefore(endOfMonth) ||
          nextPayment.isAtSameMomentAs(endOfMonth);
    }).toList();
  }

  // 计算本月订阅总支出
  Future<double> getMonthlySubscriptionTotal() async {
    final subscriptions = await getUpcomingSubscriptions();
    return subscriptions.fold<double>(0.0, (sum, s) => sum + s.amount);
  }

  Future close() async {
    if (kIsWeb) return;
    final db = await database;
    (db as Database).close();
  }
}
