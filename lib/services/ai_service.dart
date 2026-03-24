import 'dart:convert';
import 'package:dio/dio.dart';

/// AI 服务类 - 提供 OCR 发票识别和智能分类功能
class AIService {
  static final AIService _instance = AIService._internal();
  factory AIService() => _instance;
  AIService._internal();

  final Dio _dio = Dio(BaseOptions(
    connectTimeout: const Duration(seconds: 30),
    receiveTimeout: const Duration(seconds: 30),
  ));

  // Kimi Coding API 配置 - 官方推荐配置
  static const String _kimiBaseUrl = 'https://api.kimi.com/coding/v1';
  static const String _kimiModel = 'kimi-for-coding';
  static const int _maxOutputTokens = 32768;
  
  // 硬编码 API Key（实验课临时使用）
  static const String _defaultKimiApiKey = 'sk-kimi-jpFn3oIT6H6wHyTATFIWtkO3CcOP5D9bzKeolIjPAKUz68qPGA7yhAzPKuSwFQJI';

  // OpenAI API 配置（备选）
  static const String _openaiBaseUrl = 'https://api.openai.com/v1';
  static const String _openaiModel = 'gpt-3.5-turbo';

  String _kimiApiKey = _defaultKimiApiKey;
  String? _openaiApiKey;

  /// 初始化 API Keys
  void initialize({String? kimiApiKey, String? openaiApiKey}) {
    if (kimiApiKey != null && kimiApiKey.isNotEmpty) {
      _kimiApiKey = kimiApiKey;
    }
    _openaiApiKey = openaiApiKey;
  }

  /// 使用 Kimi Coding K2.5 识别发票
  /// 免费且中文发票识别能力强
  Future<ReceiptInfo> recognizeReceiptWithKimi(String base64Image) async {
    // Web端演示模式：模拟延迟后返回Mock结果（用于CORS限制下的演示）
    if (_kimiApiKey == 'mock' || _kimiApiKey == _defaultKimiApiKey) {
      await Future.delayed(const Duration(seconds: 2)); // 模拟网络延迟
      return _mockReceiptInfo();
    }

    try {
      final response = await _dio.post(
        '$_kimiBaseUrl/chat/completions',
        options: Options(
          headers: {
            'Authorization': 'Bearer $_kimiApiKey',
            'Content-Type': 'application/json',
          },
        ),
        data: {
          'model': _kimiModel,
          'messages': [
            {
              'role': 'system',
              'content': '你是一个专业的发票识别助手，擅长从各类发票图片中提取结构化信息。'
            },
            {
              'role': 'user',
              'content': [
                {
                  'type': 'text',
                  'text': '''请识别这张发票图片，提取以下信息并以JSON格式返回：
{
  "merchant": "商家名称（发票抬头）",
  "amount": 金额数字（不含税的总金额，纯数字）,
  "date": "YYYY-MM-DD",
  "category": "消费类别（餐饮/交通/购物/生活缴费/医疗/教育/娱乐/其他）",
  "items": ["商品或服务明细1", "商品或服务明细2"],
  "invoiceNumber": "发票号码（如有）",
  "taxAmount": 税额数字（如有）
}

注意：
1. 金额只需返回数字，不要包含"¥"或"元"
2. 日期统一转换为 YYYY-MM-DD 格式
3. 如果无法识别某项，使用 null 或 "未知"
4. 只返回 JSON，不要有其他内容'''
                },
                {
                  'type': 'image_url',
                  'image_url': {
                    'url': 'data:image/jpeg;base64,$base64Image'
                  }
                }
              ]
            }
          ],
          'temperature': 0.1,
          'max_tokens': _maxOutputTokens,
          'stream': false
        },
      );

      final content = response.data['choices'][0]['message']['content'] as String;
      return _parseReceiptInfo(content);
    } catch (e) {
      throw Exception('发票识别失败: $e');
    }
  }

  /// Mock发票数据（演示模式）
  ReceiptInfo _mockReceiptInfo() {
    return const ReceiptInfo(
      merchant: '昆医附一院',
      amount: 156.80,
      date: '2025-11-21',
      category: 'medical',
      items: ['门诊诊疗费', '药品费'],
      invoiceNumber: '02251',
      taxAmount: null,
    );
  }

  /// 使用 OpenAI GPT-4o-mini 识别发票（备选方案）
  Future<ReceiptInfo> recognizeReceiptWithOpenAI(String base64Image) async {
    if (_openaiApiKey == null) {
      throw Exception('OpenAI API Key 未设置');
    }

    try {
      final response = await _dio.post(
        '$_openaiBaseUrl/chat/completions',
        options: Options(
          headers: {
            'Authorization': 'Bearer $_openaiApiKey',
            'Content-Type': 'application/json',
          },
        ),
        data: {
          'model': 'gpt-4o-mini',
          'messages': [
            {
              'role': 'user',
              'content': [
                {
                  'type': 'text',
                  'text': '''请识别这张发票，提取以下信息并以JSON格式返回：
{
  "merchant": "商家名称",
  "amount": 金额数字,
  "date": "YYYY-MM-DD",
  "category": "消费类别（餐饮/交通/购物/生活缴费/其他）",
  "items": ["商品1", "商品2"],
  "invoiceNumber": "发票号码（如有）"
}'''
                },
                {
                  'type': 'image_url',
                  'image_url': {
                    'url': 'data:image/jpeg;base64,$base64Image'
                  }
                }
              ]
            }
          ],
          'max_tokens': 500
        },
      );

      final content = response.data['choices'][0]['message']['content'] as String;
      return _parseReceiptInfo(content);
    } catch (e) {
      throw Exception('发票识别失败: $e');
    }
  }

  /// 智能分类交易
  /// 优先使用规则匹配，其次调用 AI
  Future<CategorizationResult> categorizeTransaction({
    required String payee,
    required double amount,
    String? notes,
    required bool isExpense,
  }) async {
    // 1. 先尝试规则匹配
    final ruleResult = _matchByRule(payee: payee, notes: notes);
    if (ruleResult != null) {
      return CategorizationResult(
        categoryId: ruleResult,
        categoryName: _getCategoryName(ruleResult),
        confidence: 0.95,
        reasoning: '规则匹配',
      );
    }

    // 2. 规则未匹配，调用 AI
    if (_openaiApiKey != null) {
      return await _categorizeWithOpenAI(
        payee: payee,
        amount: amount,
        notes: notes,
        isExpense: isExpense,
      );
    }

    // 3. 返回默认分类
    return CategorizationResult(
      categoryId: isExpense ? 'other_expense' : 'other_income',
      categoryName: isExpense ? '其他支出' : '其他收入',
      confidence: 0,
      reasoning: '未匹配到规则且 AI 未配置',
    );
  }

  /// 使用 OpenAI 进行分类
  Future<CategorizationResult> _categorizeWithOpenAI({
    required String payee,
    required double amount,
    String? notes,
    required bool isExpense,
  }) async {
    final categories = isExpense
        ? ['food', 'transport', 'shopping', 'entertainment', 'utilities', 'housing', 'medical', 'education', 'other_expense']
        : ['salary', 'investment', 'other_income'];

    try {
      final response = await _dio.post(
        '$_openaiBaseUrl/chat/completions',
        options: Options(
          headers: {
            'Authorization': 'Bearer $_openaiApiKey',
            'Content-Type': 'application/json',
          },
        ),
        data: {
          'model': _openaiModel,
          'messages': [
            {
              'role': 'system',
              'content': '你是一个精准的财务分类助手，擅长根据交易描述判断消费类别。只返回JSON格式结果。'
            },
            {
              'role': 'user',
              'content': '''请将以下交易分类到最合适的类别。

交易信息：
- 商家: $payee
- 金额: $amount 元
- 备注: ${notes ?? '无'}
- 类型: ${isExpense ? '支出' : '收入'}

可选类别：${categories.join(', ')}

请以JSON格式返回：
{
  "categoryId": "类别ID",
  "confidence": 0.95,
  "reasoning": "分类理由"
}'''
            }
          ],
          'temperature': 0.3,
          'max_tokens': 150
        },
      );

      final content = response.data['choices'][0]['message']['content'] as String;
      final result = _parseCategoryResult(content);
      return result.copyWith(
        categoryName: _getCategoryName(result.categoryId),
      );
    } catch (e) {
      return CategorizationResult(
        categoryId: isExpense ? 'other_expense' : 'other_income',
        categoryName: isExpense ? '其他支出' : '其他收入',
        confidence: 0,
        reasoning: 'AI 分类失败: $e',
      );
    }
  }

  /// 规则匹配
  String? _matchByRule({required String payee, String? notes}) {
    final text = '$payee ${notes ?? ''}'.toLowerCase();

    final rules = [
      (pattern: RegExp(r'美团|饿了么|外卖|餐厅|快餐|肯德基|麦当劳|星巴克|火锅|烧烤'), category: 'food'),
      (pattern: RegExp(r'滴滴|地铁|公交|打车|加油|高德|百度地图|火车票|机票|高铁'), category: 'transport'),
      (pattern: RegExp(r'淘宝|京东|天猫|拼多多|亚马逊|苏宁|国美|商场|超市|便利店'), category: 'shopping'),
      (pattern: RegExp(r'电费|水费|燃气|宽带|话费|移动|联通|电信|有线电视'), category: 'utilities'),
      (pattern: RegExp(r'房租|房贷|物业|房租'), category: 'housing'),
      (pattern: RegExp(r'医院|药店|体检|诊所|医药'), category: 'medical'),
      (pattern: RegExp(r'课程|培训|书籍|教育|学费|学习'), category: 'education'),
      (pattern: RegExp(r'电影|游戏|会员|视频|音乐|娱乐|ktv'), category: 'entertainment'),
      (pattern: RegExp(r'工资|薪资|奖金|收入|月薪'), category: 'salary'),
      (pattern: RegExp(r'红包|转账'), category: 'other_income'),
    ];

    for (final rule in rules) {
      if (rule.pattern.hasMatch(text)) {
        return rule.category;
      }
    }

    return null;
  }

  /// 获取分类名称
  String _getCategoryName(String categoryId) {
    final names = {
      'food': '餐饮',
      'transport': '交通',
      'shopping': '购物',
      'entertainment': '娱乐',
      'utilities': '生活缴费',
      'housing': '居住',
      'medical': '医疗',
      'education': '教育',
      'salary': '工资',
      'investment': '理财收益',
      'other_expense': '其他支出',
      'other_income': '其他收入',
    };
    return names[categoryId] ?? '其他';
  }

  /// 解析发票识别结果
  ReceiptInfo _parseReceiptInfo(String content) {
    try {
      // 提取 JSON 部分
      final jsonMatch = RegExp(r'```json\n?([\s\S]*?)\n?```').firstMatch(content) ??
                        RegExp(r'\{[\s\S]*\}').firstMatch(content);
      
      final jsonStr = jsonMatch != null 
          ? (jsonMatch.group(1) ?? jsonMatch.group(0)!)
          : content;
      
      final result = jsonDecode(jsonStr);

      return ReceiptInfo(
        merchant: result['merchant']?.toString() ?? '未知商家',
        amount: double.tryParse(result['amount'].toString()) ?? 0.0,
        date: _normalizeDate(result['date']?.toString()),
        category: _mapToStandardCategory(result['category']?.toString()),
        items: (result['items'] as List<dynamic>?)?.map((e) => e.toString()).toList() ?? [],
        invoiceNumber: result['invoiceNumber']?.toString(),
        taxAmount: result['taxAmount'] != null 
            ? double.tryParse(result['taxAmount'].toString()) 
            : null,
      );
    } catch (e) {
      throw Exception('解析发票识别结果失败: $e');
    }
  }

  /// 解析分类结果
  CategorizationResult _parseCategoryResult(String content) {
    try {
      final jsonMatch = RegExp(r'\{[\s\S]*\}').firstMatch(content);
      final jsonStr = jsonMatch != null ? jsonMatch.group(0)! : content;
      final result = jsonDecode(jsonStr);

      return CategorizationResult(
        categoryId: result['categoryId']?.toString() ?? 'other_expense',
        categoryName: '', // 后续通过 _getCategoryName 填充
        confidence: double.tryParse(result['confidence'].toString()) ?? 0.0,
        reasoning: result['reasoning']?.toString() ?? '',
      );
    } catch (e) {
      return const CategorizationResult(
        categoryId: 'other_expense',
        categoryName: '其他支出',
        confidence: 0,
        reasoning: '解析失败',
      );
    }
  }

  /// 日期格式标准化
  String _normalizeDate(String? dateStr) {
    if (dateStr == null || dateStr.isEmpty || dateStr == '未知') {
      return DateTime.now().toIso8601String().split('T')[0];
    }

    // 尝试解析各种中文日期格式
    final patterns = [
      RegExp(r'(\d{4})年(\d{1,2})月(\d{1,2})日'),
      RegExp(r'(\d{4})-(\d{1,2})-(\d{1,2})'),
      RegExp(r'(\d{4})/(\d{1,2})/(\d{1,2})'),
    ];

    for (final pattern in patterns) {
      final match = pattern.firstMatch(dateStr);
      if (match != null) {
        final year = match.group(1)!;
        final month = match.group(2)!.padLeft(2, '0');
        final day = match.group(3)!.padLeft(2, '0');
        return '$year-$month-$day';
      }
    }

    return DateTime.now().toIso8601String().split('T')[0];
  }

  /// 类别映射标准化
  String _mapToStandardCategory(String? category) {
    if (category == null) return 'other_expense';

    final categoryMap = {
      '餐饮': 'food',
      '美食': 'food',
      '外卖': 'food',
      '交通': 'transport',
      '出行': 'transport',
      '打车': 'transport',
      '购物': 'shopping',
      '超市': 'shopping',
      '生活缴费': 'utilities',
      '水电': 'utilities',
      '医疗': 'medical',
      '医院': 'medical',
      '教育': 'education',
      '娱乐': 'entertainment',
      '居住': 'housing',
      '工资': 'salary',
      '收入': 'salary',
      '其他': 'other_expense',
    };

    return categoryMap[category] ?? 'other_expense';
  }
}

/// 发票信息
class ReceiptInfo {
  final String merchant;
  final double amount;
  final String date;
  final String category;
  final List<String> items;
  final String? invoiceNumber;
  final double? taxAmount;

  const ReceiptInfo({
    required this.merchant,
    required this.amount,
    required this.date,
    required this.category,
    this.items = const [],
    this.invoiceNumber,
    this.taxAmount,
  });

  @override
  String toString() {
    return 'ReceiptInfo(merchant: $merchant, amount: $amount, date: $date, category: $category)';
  }
}

/// 分类结果
class CategorizationResult {
  final String categoryId;
  final String categoryName;
  final double confidence;
  final String reasoning;

  const CategorizationResult({
    required this.categoryId,
    required this.categoryName,
    required this.confidence,
    required this.reasoning,
  });

  CategorizationResult copyWith({
    String? categoryId,
    String? categoryName,
    double? confidence,
    String? reasoning,
  }) {
    return CategorizationResult(
      categoryId: categoryId ?? this.categoryId,
      categoryName: categoryName ?? this.categoryName,
      confidence: confidence ?? this.confidence,
      reasoning: reasoning ?? this.reasoning,
    );
  }

  @override
  String toString() {
    return 'CategorizationResult(categoryId: $categoryId, confidence: $confidence)';
  }
}
