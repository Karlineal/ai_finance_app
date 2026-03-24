import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:intl/intl.dart';
import 'package:uuid/uuid.dart';
import '../bloc/transaction_bloc.dart';
import '../constants/category_meta.dart';
import '../models/transaction.dart';
import '../theme/theme.dart';
import '../widgets/widgets.dart';

/// 记账页面 - Design System 重构版
///
/// 页面结构：
/// [顶部金额显示区]
/// [支出 / 收入切换]
/// [分类 Emoji 网格]
/// [日期 / 备注等信息区]
/// [底部固定数字键盘]
class AddTransactionScreen extends StatefulWidget {
  const AddTransactionScreen({super.key});

  @override
  State<AddTransactionScreen> createState() => _AddTransactionScreenState();
}

class _AddTransactionScreenState extends State<AddTransactionScreen> {
  bool _isExpense = true;
  String _amount = '';
  String _selectedCategoryId = '';
  DateTime _date = DateTime.now();
  String _notes = '';

  void _onKeyPressed(String key) {
    setState(() {
      if (key == '+' || key == '-') {
        // TODO: 实现计算功能
        return;
      }
      if (_amount.length < 10) {
        if (key == '.') {
          if (!_amount.contains('.')) {
            _amount += _amount.isEmpty ? '0.' : '.';
          }
        } else {
          _amount += key;
        }
      }
    });
  }

  void _onBackspace() {
    setState(() {
      if (_amount.isNotEmpty) {
        _amount = _amount.substring(0, _amount.length - 1);
      }
    });
  }

  void _onSubmit() {
    if (_amount.isEmpty || _selectedCategoryId.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('请输入金额并选择分类')),
      );
      return;
    }

    final category = getCategoryById(_selectedCategoryId);
    if (category == null) return;

    HapticFeedback.mediumImpact();

    final transaction = Transaction(
      id: const Uuid().v4(),
      date: _date,
      payee: category.name,
      amount: double.parse(_amount),
      notes: _notes.isEmpty ? null : _notes,
      category: category.name,
      type: _isExpense ? TransactionType.expense : TransactionType.income,
      createdAt: DateTime.now(),
      updatedAt: DateTime.now(),
    );

    context.read<TransactionBloc>().add(AddTransaction(transaction));
    Navigator.pop(context);
  }

  Future<void> _selectDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _date,
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
    );
    if (picked != null) {
      setState(() => _date = picked);
    }
  }

  void _showNotesDialog() {
    final controller = TextEditingController(text: _notes);
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('添加备注'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(hintText: '输入备注...'),
          maxLines: 3,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () {
              setState(() => _notes = controller.text);
              Navigator.pop(context);
            },
            child: const Text('确定'),
          ),
        ],
      ),
    );
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
        leading: IconButton(
          icon: Icon(Icons.close, color: colors.textPrimary),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text(
          '记一笔',
          style: textStyles.titleMedium,
        ),
        centerTitle: true,
      ),
      body: Column(
        children: [
          // 金额显示
          AmountInputDisplay(
            amount: _amount,
            isExpense: _isExpense,
            onClear: () => setState(() => _amount = ''),
          ),

          // 类型切换
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.xl),
            child: TransactionTypeSwitch(
              isExpense: _isExpense,
              onChanged: (isExpense) {
                setState(() {
                  _isExpense = isExpense;
                  _selectedCategoryId = '';
                });
              },
            ),
          ),

          const SizedBox(height: AppSpacing.lg),

          // 分类网格
          Expanded(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
              child: CategoryGrid(
                type: _isExpense
                    ? TransactionType.expense
                    : TransactionType.income,
                selectedCategoryId: _selectedCategoryId,
                onSelect: (category) {
                  setState(() => _selectedCategoryId = category.id);
                  HapticFeedback.lightImpact();
                },
              ),
            ),
          ),

          // 信息区（日期、备注）
          _buildMetaSection(colors, textStyles),

          // 数字键盘
          CustomNumericKeyboard(
            onKeyPressed: _onKeyPressed,
            onBackspace: _onBackspace,
            onSubmit: _onSubmit,
            onDatePressed: _selectDate,
            submitText: '记一笔',
          ),
        ],
      ),
    );
  }

  Widget _buildMetaSection(AppColors colors, AppTextStyles textStyles) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.md,
      ),
      decoration: BoxDecoration(
        border: Border(
          top: BorderSide(color: colors.divider),
        ),
      ),
      child: Row(
        children: [
          // 日期
          GestureDetector(
            onTap: _selectDate,
            child: Row(
              children: [
                Icon(
                  Icons.calendar_today,
                  size: 16,
                  color: colors.textSecondary,
                ),
                const SizedBox(width: AppSpacing.xs),
                Text(
                  DateFormat('MM月dd日').format(_date),
                  style: textStyles.bodyMedium,
                ),
              ],
            ),
          ),
          const SizedBox(width: AppSpacing.xl),
          // 备注
          GestureDetector(
            onTap: _showNotesDialog,
            child: Row(
              children: [
                Icon(
                  Icons.edit_note,
                  size: 16,
                  color: _notes.isEmpty
                      ? colors.textSecondary
                      : colors.brandPrimary,
                ),
                const SizedBox(width: AppSpacing.xs),
                Text(
                  _notes.isEmpty ? '添加备注' : _notes,
                  style: textStyles.bodyMedium.copyWith(
                    color: _notes.isEmpty
                        ? colors.textSecondary
                        : colors.textPrimary,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
