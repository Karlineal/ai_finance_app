import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:uuid/uuid.dart';
import '../theme/theme.dart';
import '../widgets/widgets.dart';

/// 存钱计划模型
class SavingsPlan {
  final String id;
  String name;
  double targetAmount;
  double savedAmount;
  DateTime? deadline;
  String? icon;
  int color;
  DateTime createdAt;

  SavingsPlan({
    required this.id,
    required this.name,
    required this.targetAmount,
    this.savedAmount = 0,
    this.deadline,
    this.icon,
    required this.color,
    required this.createdAt,
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'targetAmount': targetAmount,
        'savedAmount': savedAmount,
        'deadline': deadline?.toIso8601String(),
        'icon': icon,
        'color': color,
        'createdAt': createdAt.toIso8601String(),
      };

  factory SavingsPlan.fromJson(Map<String, dynamic> json) => SavingsPlan(
        id: json['id'],
        name: json['name'],
        targetAmount: json['targetAmount'],
        savedAmount: json['savedAmount'] ?? 0,
        deadline:
            json['deadline'] != null ? DateTime.parse(json['deadline']) : null,
        icon: json['icon'],
        color: json['color'],
        createdAt: DateTime.parse(json['createdAt']),
      );

  double get progress =>
      targetAmount > 0 ? (savedAmount / targetAmount).clamp(0.0, 1.0) : 0.0;
  bool get isCompleted => savedAmount >= targetAmount;
}

/// 存钱计划页面 - 使用新 Design System
class SavingsPlanScreen extends StatefulWidget {
  const SavingsPlanScreen({super.key});
  @override
  State<SavingsPlanScreen> createState() => _SavingsPlanScreenState();
}

class _SavingsPlanScreenState extends State<SavingsPlanScreen> {
  List<SavingsPlan> _plans = [];
  bool _isLoading = true;

  final _templates = [
    {'name': '旅行', 'emoji': '✈️'},
    {'name': '买车', 'emoji': '🚗'},
    {'name': '购房', 'emoji': '🏠'},
    {'name': '应急', 'emoji': '🛡️'},
    {'name': '数码', 'emoji': '💻'},
    {'name': '学习', 'emoji': '📚'},
    {'name': '结婚', 'emoji': '💍'},
    {'name': '育儿', 'emoji': '👶'},
  ];

  @override
  void initState() {
    super.initState();
    _loadPlans();
  }

  Future<void> _loadPlans() async {
    final prefs = await SharedPreferences.getInstance();
    final plansJson = prefs.getStringList('savings_plans') ?? [];
    setState(() {
      _plans =
          plansJson.map((j) => SavingsPlan.fromJson(jsonDecode(j))).toList();
      _isLoading = false;
    });
  }

  Future<void> _savePlans() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setStringList(
        'savings_plans', _plans.map((p) => jsonEncode(p.toJson())).toList());
  }

  void _addPlan(SavingsPlan plan) {
    setState(() => _plans.add(plan));
    _savePlans();
  }

  void _updatePlan(SavingsPlan plan) {
    setState(() {
      final i = _plans.indexWhere((p) => p.id == plan.id);
      if (i != -1) _plans[i] = plan;
    });
    _savePlans();
  }

  void _deletePlan(String id) {
    setState(() => _plans.removeWhere((p) => p.id == id));
    _savePlans();
  }

  String _getEmoji(SavingsPlan plan) {
    if (plan.icon?.isNotEmpty == true) return plan.icon!;
    final t = _templates.firstWhere((t) => plan.name.contains(t['name']!),
        orElse: () => {'emoji': '💰'});
    return t['emoji']!;
  }

  void _showAddDialog() {
    final nameCtrl = TextEditingController();
    final amtCtrl = TextEditingController();
    var selectedIdx = 0;
    final colors = AppColors.of(context);

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setModal) => Container(
          padding: EdgeInsets.only(
              bottom: MediaQuery.of(ctx).viewInsets.bottom + AppSpacing.xl,
              top: AppSpacing.xl,
              left: AppSpacing.xl,
              right: AppSpacing.xl),
          decoration: BoxDecoration(
              color: colors.cardPrimary,
              borderRadius: const BorderRadius.vertical(
                  top: Radius.circular(AppRadius.xl))),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                  child: Container(
                      width: 40,
                      height: 4,
                      decoration: BoxDecoration(
                          color: colors.divider,
                          borderRadius: BorderRadius.circular(2)))),
              const SizedBox(height: AppSpacing.xl),
              Text('新建存钱计划', style: AppTextStyles.of(ctx).titleMedium),
              const SizedBox(height: AppSpacing.xl),
              _input(nameCtrl, '计划名称', '例如：旅行基金'),
              const SizedBox(height: AppSpacing.lg),
              _input(amtCtrl, '目标金额', '0',
                  prefix: '¥ ', type: TextInputType.number),
              const SizedBox(height: AppSpacing.xl),
              Text('选择图标', style: AppTextStyles.of(ctx).bodySmall),
              const SizedBox(height: AppSpacing.md),
              Wrap(
                  spacing: AppSpacing.md,
                  runSpacing: AppSpacing.md,
                  children: _templates.asMap().entries.map((e) {
                    final i = e.key;
                    final t = e.value;
                    final isSel = selectedIdx == i;
                    return GestureDetector(
                      onTap: () => setModal(() => selectedIdx = i),
                      child: Container(
                          width: 60,
                          height: 60,
                          decoration: BoxDecoration(
                              color: isSel
                                  ? colors.brandPrimary
                                  : colors.backgroundSecondary,
                              borderRadius: BorderRadius.circular(AppRadius.lg),
                              border: isSel
                                  ? Border.all(
                                      color: colors.brandSecondary, width: 2)
                                  : null),
                          child: Center(
                              child: Text(t['emoji']!,
                                  style: const TextStyle(fontSize: 28)))),
                    );
                  }).toList()),
              const SizedBox(height: AppSpacing.xxl),
              Row(children: [
                Expanded(child: _btn('取消', () => Navigator.pop(ctx), false)),
                const SizedBox(width: AppSpacing.md),
                Expanded(
                    child: _btn('创建', () {
                  if (nameCtrl.text.isNotEmpty && amtCtrl.text.isNotEmpty) {
                    _addPlan(SavingsPlan(
                        id: const Uuid().v4(),
                        name: nameCtrl.text,
                        targetAmount: double.parse(amtCtrl.text),
                        icon: _templates[selectedIdx]['emoji'],
                        color: 0,
                        createdAt: DateTime.now()));
                    Navigator.pop(ctx);
                  }
                })),
              ]),
            ],
          ),
        ),
      ),
    );
  }

  void _showTransactionDialog(SavingsPlan plan, bool isDeposit) {
    final ctrl = TextEditingController();
    final colors = AppColors.of(context);

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => Container(
        padding: EdgeInsets.only(
            bottom: MediaQuery.of(ctx).viewInsets.bottom + AppSpacing.xl,
            top: AppSpacing.xl,
            left: AppSpacing.xl,
            right: AppSpacing.xl),
        decoration: BoxDecoration(
            color: colors.cardPrimary,
            borderRadius: const BorderRadius.vertical(
                top: Radius.circular(AppRadius.xl))),
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Center(
              child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                      color: colors.divider,
                      borderRadius: BorderRadius.circular(2)))),
          const SizedBox(height: AppSpacing.xl),
          Text(plan.name, style: AppTextStyles.of(ctx).titleSmall),
          const SizedBox(height: AppSpacing.sm),
          Text(
              '当前: ¥${plan.savedAmount.toStringAsFixed(0)} / ¥${plan.targetAmount.toStringAsFixed(0)}',
              style: AppTextStyles.of(ctx).bodySmall),
          const SizedBox(height: AppSpacing.xl),
          TextField(
              controller: ctrl,
              keyboardType: TextInputType.number,
              autofocus: true,
              style: AppTextStyles.of(ctx).amountLarge,
              decoration: InputDecoration(
                  prefixText: '¥ ',
                  hintText: '0',
                  filled: true,
                  fillColor: colors.backgroundSecondary,
                  border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(AppRadius.lg),
                      borderSide: BorderSide.none))),
          const SizedBox(height: AppSpacing.xxl),
          Row(children: [
            Expanded(child: _btn('取消', () => Navigator.pop(ctx), false)),
            const SizedBox(width: AppSpacing.md),
            Expanded(
                child: _btn(isDeposit ? '存入' : '取出', () {
              final amt = double.tryParse(ctrl.text) ?? 0;
              if (amt > 0) {
                plan.savedAmount = isDeposit
                    ? plan.savedAmount + amt
                    : (plan.savedAmount - amt).clamp(0.0, double.infinity);
                _updatePlan(plan);
                Navigator.pop(ctx);
              }
            }, true, isDeposit ? colors.income : colors.expense)),
          ]),
        ]),
      ),
    );
  }

  Widget _input(TextEditingController ctrl, String label, String hint,
      {String? prefix, TextInputType? type}) {
    final c = AppColors.of(context);
    return TextField(
        controller: ctrl,
        keyboardType: type,
        decoration: InputDecoration(
            labelText: label,
            hintText: hint,
            prefixText: prefix,
            filled: true,
            fillColor: c.backgroundSecondary,
            border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(AppRadius.lg),
                borderSide: BorderSide.none)));
  }

  Widget _btn(String label, VoidCallback onTap,
      [bool isPrimary = true, Color? bg]) {
    final c = AppColors.of(context);
    return GestureDetector(
      onTap: onTap,
      child: Container(
          padding: const EdgeInsets.symmetric(vertical: AppSpacing.lg),
          decoration: BoxDecoration(
              color: bg ?? (isPrimary ? c.brandPrimary : c.backgroundSecondary),
              borderRadius: BorderRadius.circular(AppRadius.lg)),
          child: Center(
              child: Text(label,
                  style: AppTextStyles.of(context).bodyLarge.copyWith(
                      color: isPrimary ? c.textInverse : c.textPrimary,
                      fontWeight: FontWeight.w600)))),
    );
  }

  @override
  Widget build(BuildContext context) {
    final c = AppColors.of(context);
    return Scaffold(
      backgroundColor: c.backgroundPrimary,
      appBar: AppBar(
          backgroundColor: c.backgroundPrimary,
          elevation: 0,
          centerTitle: true,
          title: Text('存钱计划', style: AppTextStyles.of(context).titleMedium),
          actions: [
            IconButton(
                icon: Icon(Icons.add, color: c.brandPrimary),
                onPressed: _showAddDialog)
          ]),
      body: _isLoading
          ? Center(child: CircularProgressIndicator(color: c.brandPrimary))
          : _plans.isEmpty
              ? EmptyStateView(
                  icon: Icons.savings_outlined,
                  title: '还没有存钱计划',
                  subtitle: '点击右上角 + 创建你的第一个存钱计划',
                  action: GestureDetector(
                      onTap: _showAddDialog,
                      child: Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: AppSpacing.xl,
                              vertical: AppSpacing.md),
                          decoration: BoxDecoration(
                              color: c.brandPrimary,
                              borderRadius:
                                  BorderRadius.circular(AppRadius.lg)),
                          child: Text('创建计划',
                              style: AppTextStyles.of(context)
                                  .bodyMedium
                                  .copyWith(
                                      color: c.textInverse,
                                      fontWeight: FontWeight.w600)))))
              : ListView.builder(
                  padding: const EdgeInsets.all(AppSpacing.lg),
                  itemCount: _plans.length,
                  itemBuilder: (ctx, i) => SavingsPlanCard(
                      name: _plans[i].name,
                      emoji: _getEmoji(_plans[i]),
                      currentAmount: _plans[i].savedAmount,
                      targetAmount: _plans[i].targetAmount,
                      endDate: _plans[i].deadline,
                      onDeposit: () => _showTransactionDialog(_plans[i], true),
                      onWithdraw: () =>
                          _showTransactionDialog(_plans[i], false),
                      onDelete: () => _deletePlan(_plans[i].id))),
    );
  }
}
