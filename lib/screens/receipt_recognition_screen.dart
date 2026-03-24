import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';
import '../models/transaction.dart';
import '../services/ai_service.dart';
import '../database/database_helper.dart';
import '../theme/theme.dart';
import '../widgets/widgets.dart';

/// 发票识别页面 - Cookie 记账风格
class ReceiptRecognitionScreen extends StatefulWidget {
  const ReceiptRecognitionScreen({super.key});

  @override
  State<ReceiptRecognitionScreen> createState() =>
      _ReceiptRecognitionScreenState();
}

class _ReceiptRecognitionScreenState extends State<ReceiptRecognitionScreen> {
  final AIService _aiService = AIService();
  final ImagePicker _imagePicker = ImagePicker();
  final DatabaseHelper _db = DatabaseHelper.instance;

  Uint8List? _selectedImageBytes;
  bool _isProcessing = false;
  ReceiptInfo? _recognizedReceipt;
  String? _errorMessage;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);
    final textStyles = AppTextStyles.of(context);

    return Scaffold(
      backgroundColor: colors.backgroundPrimary,
      appBar: AppBar(
        backgroundColor: colors.backgroundPrimary,
        elevation: 0,
        title: Text('AI 发票识别', style: textStyles.titleMedium),
        centerTitle: true,
        leading: IconButton(
          icon: Icon(Icons.arrow_back_ios_new, color: colors.textPrimary),
          onPressed: () => Navigator.pop(context),
        ),
        actions: [
          if (_recognizedReceipt != null)
            TextButton(
              onPressed: _saveTransaction,
              child: Text(
                '保存',
                style: textStyles.bodyLarge.copyWith(
                  color: colors.brandPrimary,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (_selectedImageBytes != null) ...[
              _buildImagePreview(),
              const SizedBox(height: AppSpacing.lg),
            ],
            ReceiptUploadPanel(
              onCameraTap: _takePhoto,
              onGalleryTap: _pickImage,
            ),
            const SizedBox(height: AppSpacing.lg),
            if (_isProcessing) ...[
              const AIProcessingIndicator(
                message: '正在识别发票',
                subtitle: '使用 Kimi Coding K2.5 免费识别',
              ),
            ],
            if (_errorMessage != null) ...[
              _buildErrorState(),
            ],
            if (_recognizedReceipt != null && !_isProcessing) ...[
              ReceiptResultCard(
                amount: _recognizedReceipt!.amount,
                merchant: _recognizedReceipt!.merchant,
                date: _recognizedReceipt!.date,
                category: _getCategoryName(_recognizedReceipt!.category),
                categoryIcon: _getCategoryIcon(_recognizedReceipt!.category),
                editable: false,
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildImagePreview() {
    return ClipRRect(
      borderRadius: BorderRadius.circular(AppRadius.lg),
      child: Image.memory(
        _selectedImageBytes!,
        height: 240,
        width: double.infinity,
        fit: BoxFit.cover,
      ),
    );
  }

  Widget _buildErrorState() {
    final colors = AppColors.of(context);

    return EmptyStateView(
      icon: Icons.error_outline,
      title: '识别失败',
      subtitle: _errorMessage,
      action: TextButton.icon(
        onPressed: () {
          setState(() {
            _errorMessage = null;
            _selectedImageBytes = null;
            _recognizedReceipt = null;
          });
        },
        icon: Icon(Icons.refresh, color: colors.brandPrimary),
        label: Text(
          '重试',
          style: TextStyle(color: colors.brandPrimary),
        ),
      ),
    );
  }

  IconData _getCategoryIcon(String categoryId) {
    final icons = {
      'food': Icons.restaurant_outlined,
      'transport': Icons.directions_car_outlined,
      'shopping': Icons.shopping_bag_outlined,
      'entertainment': Icons.movie_outlined,
      'utilities': Icons.bolt_outlined,
      'housing': Icons.home_outlined,
      'medical': Icons.local_hospital_outlined,
      'education': Icons.school_outlined,
      'other_expense': Icons.more_horiz_outlined,
    };
    return icons[categoryId] ?? Icons.receipt_outlined;
  }

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
      'other_expense': '其他支出',
    };
    return names[categoryId] ?? categoryId;
  }

  Future<void> _takePhoto() async {
    HapticFeedback.mediumImpact();
    final pickedFile = await _imagePicker.pickImage(
      source: ImageSource.camera,
      imageQuality: 85,
    );
    if (pickedFile != null) {
      final bytes = await pickedFile.readAsBytes();
      setState(() {
        _selectedImageBytes = bytes;
        _recognizedReceipt = null;
        _errorMessage = null;
      });
      await _processImage();
    }
  }

  Future<void> _pickImage() async {
    HapticFeedback.mediumImpact();
    final pickedFile = await _imagePicker.pickImage(
      source: ImageSource.gallery,
      imageQuality: 85,
    );
    if (pickedFile != null) {
      final bytes = await pickedFile.readAsBytes();
      setState(() {
        _selectedImageBytes = bytes;
        _recognizedReceipt = null;
        _errorMessage = null;
      });
      await _processImage();
    }
  }

  Future<void> _processImage() async {
    if (_selectedImageBytes == null) return;

    setState(() {
      _isProcessing = true;
      _errorMessage = null;
    });

    try {
      final base64Image = base64Encode(_selectedImageBytes!);
      final receipt = await _aiService.recognizeReceiptWithKimi(base64Image);

      setState(() {
        _recognizedReceipt = receipt;
        _isProcessing = false;
      });
    } catch (e) {
      setState(() {
        _errorMessage = '识别失败: $e';
        _isProcessing = false;
      });
    }
  }

  Future<void> _saveTransaction() async {
    if (_recognizedReceipt == null) return;

    final receipt = _recognizedReceipt!;
    final transaction = Transaction(
      id: DateTime.now().millisecondsSinceEpoch.toString(),
      date: DateTime.parse(receipt.date),
      payee: receipt.merchant,
      amount: -receipt.amount.abs(),
      notes: receipt.items.isNotEmpty ? receipt.items.join(', ') : null,
      category: receipt.category,
      type: TransactionType.expense,
      source: 'ocr',
      createdAt: DateTime.now(),
      updatedAt: DateTime.now(),
    );

    try {
      await _db.insertTransaction(transaction);
      if (mounted) {
        HapticFeedback.mediumImpact();
        final colors = AppColors.of(context);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            backgroundColor: colors.success,
            behavior: SnackBarBehavior.floating,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(AppRadius.md),
            ),
            content: Row(
              children: [
                Icon(Icons.check_circle, color: colors.textInverse),
                const SizedBox(width: AppSpacing.md),
                Text('交易已保存', style: TextStyle(color: colors.textInverse)),
              ],
            ),
          ),
        );
        Navigator.pop(context, true);
      }
    } catch (e) {
      if (mounted) {
        final colors = AppColors.of(context);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            backgroundColor: colors.error,
            content: Text(
              '保存失败: $e',
              style: TextStyle(color: colors.textInverse),
            ),
          ),
        );
      }
    }
  }
}
