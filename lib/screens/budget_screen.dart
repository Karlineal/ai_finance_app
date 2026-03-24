import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../bloc/transaction_bloc.dart';
import '../constants/category_meta.dart';
import '../theme/theme.dart';
import '../widgets/widgets.dart';

/// 预算页面 - 使用 Design System
///
/// 月度预算总览 + 分类预算设置
class BudgetScreen extends StatefulWidget {
  const BudgetScreen({super.key});

  @override
  State<BudgetScreen> createState() => _BudgetScreenState();
}

class _BudgetScreenState extends State<BudgetScreen> {
  double _monthlyBudget = 5000.0;
  final Map<String, double> _categoryBudgets = {};
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadBudgets();
  }

  Future<void> _loadBudgets() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _monthlyBudget = prefs.getDouble('monthly_budget') ?? 5000.0;
      for (final cat in expenseCategories) {
        _categoryBudgets[cat.id] = prefs.getDouble('budget_${cat.id}') ?? 0.0;
      }
      _isLoading = false;
    });
  }

  Future<void> _saveMonthlyBudget(double budget) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setDouble('monthly_budget', budget);
    setState(() => _monthlyBudget = budget);
  }

  Future<void> _saveCategoryBudget(String categoryId, double budget) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setDouble('budget_$categoryId', budget);
    setState(() => _categoryBudgets[categoryId] = budget);
  }

  void _showEditBudgetDialog(
    String title,
    double currentValue,
    Function(double) onSave,
  ) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);
    final controller = TextEditingController(
      text: currentValue > 0 ? currentValue.toStringAsFixed(0) : '',
    );

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => Container(
        padding: EdgeInsets.only(
          bottom: MediaQuery.of(context).viewInsets.bottom + AppSpacing.lg,
          top: AppSpacing.lg,
          left: AppSpacing.lg,
          right: AppSpacing.lg,
        ),
        decoration: BoxDecoration(
          color: colors.cardPrimary,
          borderRadius: const BorderRadius.vertical(
            top: Radius.circular(AppRadius.xxl),
          ),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Drag handle
            Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: colors.divider,
                borderRadius: BorderRadius.circular(AppRadius.sm),
              ),
            ),
            const SizedBox(height: AppSpacing.xl),
            Text(
              title,
              style: textStyles.titleSmall,
            ),
            const SizedBox(height: AppSpacing.xl),
            TextField(
              controller: controller,
              keyboardType: TextInputType.number,
              autofocus: true,
              style: textStyles.amountLarge,
              decoration: InputDecoration(
                prefixText: '¥ ',
                prefixStyle: textStyles.titleMedium.copyWith(
                  color: colors.textSecondary,
                ),
                hintText: '0',
                hintStyle: textStyles.amountLarge.copyWith(
                  color: colors.textTertiary,
                ),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppRadius.lg),
                  borderSide: BorderSide.none,
                ),
                filled: true,
                fillColor: colors.backgroundSecondary,
              ),
            ),
            const SizedBox(height: AppSpacing.xl),
            Row(
              children: [
                Expanded(
                  child: GestureDetector(
                    onTap: () => Navigator.pop(context),
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                        vertical: AppSpacing.md,
                      ),
                      decoration: BoxDecoration(
                        color: colors.backgroundSecondary,
                        borderRadius: BorderRadius.circular(AppRadius.lg),
                      ),
                      child: Text(
                        '取消',
                        textAlign: TextAlign.center,
                        style: textStyles.bodyMedium.copyWith(
                          color: colors.textSecondary,
                        ),
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: AppSpacing.md),
                Expanded(
                  child: GestureDetector(
                    onTap: () {
                      final value = double.tryParse(controller.text) ?? 0;
                      onSave(value);
                      Navigator.pop(context);
                    },
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                        vertical: AppSpacing.md,
                      ),
                      decoration: BoxDecoration(
                        color: colors.brandPrimary,
                        borderRadius: BorderRadius.circular(AppRadius.lg),
                      ),
                      child: Text(
                        '保存',
                        textAlign: TextAlign.center,
                        style: textStyles.bodyMedium.copyWith(
                          color: colors.textInverse,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.md),
          ],
        ),
      ),
    );
  }

  Map<String, double> _calculateCategoryExpenses(TransactionLoaded state) {
    final expenses = <String, double>{};
    for (final cat in expenseCategories) {
      expenses[cat.id] = state.transactions
          .where((t) => t.category == cat.id && t.amount < 0)
          .fold<double>(0, (sum, t) => sum + t.amount.abs());
    }
    return expenses;
  }

  List<Map<String, dynamic>> _getCategoriesData() {
    return expenseCategories
        .map((cat) => {
              'id': cat.id,
              'name': cat.name,
            })
        .toList();
  }

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);

    return Scaffold(
      backgroundColor: colors.backgroundPrimary,
      body: _isLoading
          ? Center(
              child: CircularProgressIndicator(
                valueColor: AlwaysStoppedAnimation<Color>(colors.brandPrimary),
              ),
            )
          : BlocBuilder<TransactionBloc, TransactionState>(
              builder: (context, state) {
                if (state is! TransactionLoaded) {
                  return Center(
                    child: CircularProgressIndicator(
                      valueColor:
                          AlwaysStoppedAnimation<Color>(colors.brandPrimary),
                    ),
                  );
                }

                final monthlyExpense = state.monthlyExpense.abs();
                final categoryExpenses = _calculateCategoryExpenses(state);

                return CustomScrollView(
                  slivers: [
                    // Header
                    SliverToBoxAdapter(
                      child: _buildHeader(context),
                    ),

                    // Budget Summary Card
                    SliverToBoxAdapter(
                      child: BudgetSummaryCard(
                        totalBudget: _monthlyBudget,
                        usedAmount: monthlyExpense,
                        onEditTap: () => _showEditBudgetDialog(
                          '设置月度预算',
                          _monthlyBudget,
                          _saveMonthlyBudget,
                        ),
                      ),
                    ),

                    // Category Budget List
                    SliverToBoxAdapter(
                      child: BudgetCategoryList(
                        categories: _getCategoriesData(),
                        categoryBudgets: _categoryBudgets,
                        categoryExpenses: categoryExpenses,
                        onEditBudget: (id, name, budget) =>
                            _showEditBudgetDialog(
                          '设置$name预算',
                          budget,
                          (value) => _saveCategoryBudget(id, value),
                        ),
                      ),
                    ),

                    // Tips Section
                    SliverToBoxAdapter(
                      child: _buildTipsSection(context),
                    ),

                    const SliverToBoxAdapter(
                      child: SizedBox(height: AppSpacing.xxxl),
                    ),
                  ],
                );
              },
            ),
    );
  }

  Widget _buildHeader(BuildContext context) {
    final textStyles = AppTextStyles.of(context);

    return SafeArea(
      bottom: false,
      child: Container(
        padding: const EdgeInsets.fromLTRB(
          AppSpacing.lg,
          AppSpacing.sm,
          AppSpacing.lg,
          AppSpacing.sm,
        ),
        child: Row(
          children: [
            Expanded(
              child: Text(
                '预算管理',
                textAlign: TextAlign.center,
                style: textStyles.titleMedium,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTipsSection(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    return AppCard(
      margin: const EdgeInsets.all(AppSpacing.lg),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                Icons.lightbulb_outline,
                size: 18,
                color: colors.brandPrimary,
              ),
              const SizedBox(width: AppSpacing.sm),
              Text(
                '预算小贴士',
                style: textStyles.bodyMedium.copyWith(
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.md),
          _buildTipItem(context, '建议将月度预算控制在收入的 50%-70%'),
          _buildTipItem(context, '餐饮预算建议占总预算的 20%-30%'),
          _buildTipItem(context, '设置分类预算可以更精细地控制支出'),
        ],
      ),
    );
  }

  Widget _buildTipItem(BuildContext context, String text) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.sm),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 6,
            height: 6,
            margin: const EdgeInsets.only(top: 6),
            decoration: BoxDecoration(
              color: colors.brandPrimary,
              shape: BoxShape.circle,
            ),
          ),
          const SizedBox(width: AppSpacing.md),
          Expanded(
            child: Text(
              text,
              style: textStyles.bodySmall,
            ),
          ),
        ],
      ),
    );
  }
}
