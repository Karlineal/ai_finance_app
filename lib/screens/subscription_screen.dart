import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import 'package:uuid/uuid.dart';
import '../database/database_helper.dart';
import '../models/subscription.dart';
import '../theme/theme.dart';
import '../widgets/widgets.dart';

/// 订阅与分期管理页面
class SubscriptionScreen extends StatefulWidget {
  const SubscriptionScreen({super.key});

  @override
  State<SubscriptionScreen> createState() => _SubscriptionScreenState();
}

class _SubscriptionScreenState extends State<SubscriptionScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  List<Subscription> _subscriptions = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _loadSubscriptions();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _loadSubscriptions() async {
    setState(() => _isLoading = true);
    try {
      final subscriptions = await DatabaseHelper.instance.getSubscriptions();
      setState(() {
        _subscriptions = subscriptions;
        _isLoading = false;
      });
    } catch (e) {
      setState(() => _isLoading = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('加载失败: $e')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);
    final subscriptions = _subscriptions.where((s) => s.isActive).toList();
    final subscriptionList = subscriptions
        .where((s) => s.type == SubscriptionType.subscription)
        .toList();
    final installmentList = subscriptions
        .where((s) => s.type == SubscriptionType.installment)
        .toList();

    return Scaffold(
      backgroundColor: colors.backgroundPrimary,
      appBar: AppBar(
        backgroundColor: colors.backgroundPrimary,
        elevation: 0,
        title: Text(
          '订阅与分期',
          style: textStyles.titleSmall.copyWith(
            color: colors.textPrimary,
          ),
        ),
        centerTitle: true,
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: colors.brandPrimary,
          labelColor: colors.brandPrimary,
          unselectedLabelColor: colors.textSecondary,
          labelStyle: textStyles.bodyLarge.copyWith(
            fontWeight: FontWeight.w600,
          ),
          unselectedLabelStyle: textStyles.bodyLarge,
          tabs: [
            Tab(text: '订阅 (${subscriptionList.length})'),
            Tab(text: '分期 (${installmentList.length})'),
          ],
        ),
      ),
      body: _isLoading
          ? Center(
              child: CircularProgressIndicator(
                color: colors.brandPrimary,
              ),
            )
          : TabBarView(
              controller: _tabController,
              children: [
                _buildSubscriptionList(subscriptionList),
                _buildInstallmentList(installmentList),
              ],
            ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _showAddSubscriptionDialog,
        backgroundColor: colors.brandPrimary,
        icon: Icon(Icons.add, color: colors.textInverse),
        label: Text(
          '新增',
          style: textStyles.bodyLarge.copyWith(
            color: colors.textInverse,
          ),
        ),
      ),
    );
  }

  Widget _buildSubscriptionList(List<Subscription> list) {
    if (list.isEmpty) {
      return const EmptyStateView(
        icon: Icons.subscriptions_outlined,
        title: '暂无订阅',
        subtitle: '添加你的 Netflix、Spotify 等订阅服务',
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.all(AppSpacing.lg),
      itemCount: list.length + 1,
      itemBuilder: (context, index) {
        if (index == 0) {
          return _buildMonthlyTotalCard();
        }
        final subscription = list[index - 1];
        return SubscriptionItemCard(
          subscription: subscription,
          onDelete: () => _deleteSubscription(subscription),
        );
      },
    );
  }

  Widget _buildInstallmentList(List<Subscription> list) {
    if (list.isEmpty) {
      return const EmptyStateView(
        icon: Icons.payment_outlined,
        title: '暂无分期',
        subtitle: '添加你的 iPhone 分期、贷款等',
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.all(AppSpacing.lg),
      itemCount: list.length,
      itemBuilder: (context, index) {
        final subscription = list[index];
        return InstallmentItemCard(
          subscription: subscription,
          onDelete: () => _deleteSubscription(subscription),
        );
      },
    );
  }

  Widget _buildMonthlyTotalCard() {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);
    final monthlyTotal = _subscriptions
        .where((s) => s.isActive && s.type == SubscriptionType.subscription)
        .fold<double>(0.0, (sum, s) => sum + s.amount);
    final activeCount = _subscriptions
        .where((s) => s.isActive && s.type == SubscriptionType.subscription)
        .length;

    return Container(
      margin: const EdgeInsets.only(bottom: AppSpacing.lg),
      padding: const EdgeInsets.all(AppSpacing.xl),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [colors.brandPrimary, colors.brandSecondary],
        ),
        borderRadius: BorderRadius.circular(AppRadius.lg),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '本月订阅支出',
            style: textStyles.bodyMedium.copyWith(
              color: colors.textInverse.withValues(alpha: 0.8),
            ),
          ),
          const SizedBox(height: AppSpacing.sm),
          Text(
            '¥${monthlyTotal.toStringAsFixed(2)}',
            style: textStyles.amountLarge.copyWith(
              color: colors.textInverse,
            ),
          ),
          const SizedBox(height: AppSpacing.xs),
          Text(
            '$activeCount 个活跃订阅',
            style: textStyles.bodySmall.copyWith(
              color: colors.textInverse.withValues(alpha: 0.8),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _deleteSubscription(Subscription subscription) async {
    try {
      await DatabaseHelper.instance.deleteSubscription(subscription.id);
      _loadSubscriptions();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('已删除')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('删除失败: $e')),
        );
      }
    }
  }

  void _showAddSubscriptionDialog() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => AddSubscriptionSheet(
        onSave: (subscription) async {
          await DatabaseHelper.instance.insertSubscription(subscription);
          _loadSubscriptions();
        },
      ),
    );
  }
}

/// 添加订阅底部弹窗
class AddSubscriptionSheet extends StatefulWidget {
  final Function(Subscription) onSave;

  const AddSubscriptionSheet({super.key, required this.onSave});

  @override
  State<AddSubscriptionSheet> createState() => _AddSubscriptionSheetState();
}

class _AddSubscriptionSheetState extends State<AddSubscriptionSheet> {
  final _nameController = TextEditingController();
  final _amountController = TextEditingController();
  final _notesController = TextEditingController();
  SubscriptionType _type = SubscriptionType.subscription;
  SubscriptionFrequency _frequency = SubscriptionFrequency.monthly;
  final int _frequencyValue = 1;
  DateTime _startDate = DateTime.now();
  DateTime? _endDate;
  String? _selectedIcon;
  String _category = '软件订阅';

  final List<String> _categories = [
    '软件订阅',
    '视频会员',
    '音乐会员',
    '云存储',
    '游戏',
    '健身',
    '教育',
    '其他',
  ];

  @override
  void dispose() {
    _nameController.dispose();
    _amountController.dispose();
    _notesController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    return Container(
      height: MediaQuery.of(context).size.height * 0.85,
      decoration: BoxDecoration(
        color: colors.cardPrimary,
        borderRadius: const BorderRadius.vertical(
          top: Radius.circular(AppRadius.xxl),
        ),
      ),
      child: Column(
        children: [
          // 拖动条
          Container(
            margin: const EdgeInsets.only(top: AppSpacing.md),
            width: 40,
            height: 4,
            decoration: BoxDecoration(
              color: colors.divider,
              borderRadius: BorderRadius.circular(AppRadius.sm),
            ),
          ),

          // 标题
          Padding(
            padding: const EdgeInsets.all(AppSpacing.xl),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  '新增订阅/分期',
                  style: textStyles.titleSmall,
                ),
                TextButton(
                  onPressed: _save,
                  child: Text(
                    '保存',
                    style: textStyles.bodyLarge.copyWith(
                      color: colors.brandPrimary,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ],
            ),
          ),

          // 类型选择
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.xl),
            child: Container(
              decoration: BoxDecoration(
                color: colors.backgroundSecondary,
                borderRadius: BorderRadius.circular(AppRadius.md),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: _buildTypeButton(
                      title: '订阅',
                      isSelected: _type == SubscriptionType.subscription,
                      onTap: () =>
                          setState(() => _type = SubscriptionType.subscription),
                    ),
                  ),
                  Expanded(
                    child: _buildTypeButton(
                      title: '分期',
                      isSelected: _type == SubscriptionType.installment,
                      onTap: () =>
                          setState(() => _type = SubscriptionType.installment),
                    ),
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: AppSpacing.sm),

          // 表单内容
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(AppSpacing.xl),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 图标选择
                  Text(
                    '选择图标',
                    style: textStyles.label.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.md),
                  SizedBox(
                    height: 80,
                    child: ListView.builder(
                      scrollDirection: Axis.horizontal,
                      itemCount: subscriptionIconTemplates.length,
                      itemBuilder: (context, index) {
                        final template = subscriptionIconTemplates[index];
                        final isSelected = _selectedIcon == template.icon;
                        return GestureDetector(
                          onTap: () =>
                              setState(() => _selectedIcon = template.icon),
                          child: Container(
                            width: 64,
                            margin: const EdgeInsets.only(right: AppSpacing.md),
                            decoration: BoxDecoration(
                              color: isSelected
                                  ? Color(template.color).withValues(alpha: 0.2)
                                  : colors.backgroundSecondary,
                              borderRadius: BorderRadius.circular(AppRadius.md),
                              border: isSelected
                                  ? Border.all(
                                      color: Color(template.color),
                                      width: 2,
                                    )
                                  : null,
                            ),
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Icon(
                                  _getIconData(template.icon),
                                  color: Color(template.color),
                                  size: 28,
                                ),
                                const SizedBox(height: AppSpacing.xs),
                                Text(
                                  template.name,
                                  style: textStyles.caption.copyWith(
                                    color: isSelected
                                        ? Color(template.color)
                                        : colors.textSecondary,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
                  ),

                  const SizedBox(height: AppSpacing.xl),

                  // 名称输入
                  _buildTextField(
                    controller: _nameController,
                    label: '名称',
                    hintText: '如：Netflix、iPhone 15 Pro',
                  ),

                  const SizedBox(height: AppSpacing.lg),

                  // 金额输入
                  _buildTextField(
                    controller: _amountController,
                    label: '金额',
                    hintText: '0.00',
                    keyboardType: TextInputType.number,
                    prefix: Text(
                      '¥',
                      style: textStyles.titleSmall,
                    ),
                  ),

                  const SizedBox(height: AppSpacing.lg),

                  // 周期选择
                  Text(
                    '扣费周期',
                    style: textStyles.label.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.md),
                  Wrap(
                    spacing: AppSpacing.sm,
                    children: [
                      _buildFrequencyChip(SubscriptionFrequency.daily, '每天'),
                      _buildFrequencyChip(SubscriptionFrequency.weekly, '每周'),
                      _buildFrequencyChip(SubscriptionFrequency.monthly, '每月'),
                      _buildFrequencyChip(SubscriptionFrequency.yearly, '每年'),
                    ],
                  ),

                  const SizedBox(height: AppSpacing.lg),

                  // 分类选择
                  Text(
                    '分类',
                    style: textStyles.label.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.md),
                  Wrap(
                    spacing: AppSpacing.sm,
                    runSpacing: AppSpacing.sm,
                    children: _categories
                        .map((cat) => _buildCategoryChip(cat))
                        .toList(),
                  ),

                  const SizedBox(height: AppSpacing.lg),

                  // 开始日期
                  _buildDatePicker(
                    label: '开始日期',
                    date: _startDate,
                    onTap: () => _selectDate(context, true),
                  ),

                  // 分期显示结束日期
                  if (_type == SubscriptionType.installment) ...[
                    const SizedBox(height: AppSpacing.lg),
                    _buildDatePicker(
                      label: '结束日期',
                      date: _endDate,
                      hintText: '选择结束日期',
                      onTap: () => _selectDate(context, false),
                    ),
                  ],

                  const SizedBox(height: AppSpacing.lg),

                  // 备注
                  _buildTextField(
                    controller: _notesController,
                    label: '备注（可选）',
                    hintText: '添加备注...',
                    maxLines: 2,
                  ),

                  const SizedBox(height: AppSpacing.xl),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTypeButton({
    required String title,
    required bool isSelected,
    required VoidCallback onTap,
  }) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: AppSpacing.md),
        decoration: BoxDecoration(
          color: isSelected ? colors.brandPrimary : Colors.transparent,
          borderRadius: BorderRadius.circular(AppRadius.sm),
        ),
        child: Text(
          title,
          textAlign: TextAlign.center,
          style: textStyles.bodyLarge.copyWith(
            color: isSelected ? colors.textInverse : colors.textSecondary,
            fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
          ),
        ),
      ),
    );
  }

  Widget _buildTextField({
    required TextEditingController controller,
    required String label,
    String? hintText,
    TextInputType? keyboardType,
    Widget? prefix,
    int maxLines = 1,
  }) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: textStyles.label.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: AppSpacing.sm),
        TextField(
          controller: controller,
          keyboardType: keyboardType,
          maxLines: maxLines,
          style: textStyles.bodyLarge,
          decoration: InputDecoration(
            hintText: hintText,
            hintStyle: textStyles.bodyMedium.copyWith(
              color: colors.textTertiary,
            ),
            prefixIcon: prefix != null
                ? Padding(
                    padding: const EdgeInsets.only(
                      left: AppSpacing.lg,
                      right: AppSpacing.sm,
                    ),
                    child: prefix,
                  )
                : null,
            filled: true,
            fillColor: colors.backgroundSecondary,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(AppRadius.md),
              borderSide: BorderSide.none,
            ),
            contentPadding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.lg,
              vertical: AppSpacing.lg,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildFrequencyChip(SubscriptionFrequency freq, String label) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);
    final isSelected = _frequency == freq;

    return ChoiceChip(
      label: Text(label),
      selected: isSelected,
      onSelected: (_) => setState(() => _frequency = freq),
      selectedColor: colors.brandPrimary.withValues(alpha: 0.2),
      backgroundColor: colors.backgroundSecondary,
      labelStyle: textStyles.bodyMedium.copyWith(
        color: isSelected ? colors.brandPrimary : colors.textSecondary,
        fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
      ),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppRadius.round),
        side: BorderSide(
          color: isSelected ? colors.brandPrimary : Colors.transparent,
        ),
      ),
    );
  }

  Widget _buildCategoryChip(String category) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);
    final isSelected = _category == category;

    return ChoiceChip(
      label: Text(category),
      selected: isSelected,
      onSelected: (_) => setState(() => _category = category),
      selectedColor: colors.brandPrimary.withValues(alpha: 0.2),
      backgroundColor: colors.backgroundSecondary,
      labelStyle: textStyles.bodySmall.copyWith(
        color: isSelected ? colors.brandPrimary : colors.textSecondary,
        fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
      ),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppRadius.round),
        side: BorderSide(
          color: isSelected ? colors.brandPrimary : Colors.transparent,
        ),
      ),
    );
  }

  Widget _buildDatePicker({
    required String label,
    DateTime? date,
    String? hintText,
    required VoidCallback onTap,
  }) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    return GestureDetector(
      onTap: onTap,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: textStyles.label.copyWith(
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: AppSpacing.sm),
          Container(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.lg,
              vertical: AppSpacing.lg,
            ),
            decoration: BoxDecoration(
              color: colors.backgroundSecondary,
              borderRadius: BorderRadius.circular(AppRadius.md),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  date != null
                      ? DateFormat('yyyy年MM月dd日').format(date)
                      : hintText ?? '选择日期',
                  style: textStyles.bodyLarge.copyWith(
                    color:
                        date != null ? colors.textPrimary : colors.textTertiary,
                  ),
                ),
                Icon(
                  Icons.calendar_today,
                  size: 20,
                  color: colors.textSecondary,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _selectDate(BuildContext context, bool isStartDate) async {
    final colors = AppColors.of(context);
    final picked = await showDatePicker(
      context: context,
      initialDate: isStartDate ? _startDate : (_endDate ?? DateTime.now()),
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: ColorScheme.light(
              primary: colors.brandPrimary,
            ),
          ),
          child: child!,
        );
      },
    );
    if (picked != null) {
      setState(() {
        if (isStartDate) {
          _startDate = picked;
        } else {
          _endDate = picked;
        }
      });
    }
  }

  IconData _getIconData(String iconName) {
    final map = {
      'movie': Icons.movie,
      'music_note': Icons.music_note,
      'cloud': Icons.cloud,
      'apps': Icons.apps,
      'sports_esports': Icons.sports_esports,
      'fitness_center': Icons.fitness_center,
      'newspaper': Icons.newspaper,
      'shopping_bag': Icons.shopping_bag,
      'delivery_dining': Icons.delivery_dining,
      'directions_car': Icons.directions_car,
      'school': Icons.school,
      'more_horiz': Icons.more_horiz,
      'subscriptions': Icons.subscriptions,
    };
    return map[iconName] ?? Icons.subscriptions;
  }

  void _save() {
    if (_nameController.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('请输入名称')),
      );
      return;
    }

    if (_amountController.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('请输入金额')),
      );
      return;
    }

    if (_type == SubscriptionType.installment && _endDate == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('请选择分期结束日期')),
      );
      return;
    }

    HapticFeedback.mediumImpact();

    final subscription = Subscription(
      id: const Uuid().v4(),
      name: _nameController.text,
      icon: _selectedIcon ?? 'more_horiz',
      amount: double.parse(_amountController.text),
      type: _type,
      frequency: _frequency,
      frequencyValue: _frequencyValue,
      startDate: _startDate,
      endDate: _endDate,
      category: _category,
      notes: _notesController.text.isEmpty ? null : _notesController.text,
      isActive: true,
      createdAt: DateTime.now(),
      updatedAt: DateTime.now(),
    );

    widget.onSave(subscription);
    Navigator.pop(context);
  }
}
