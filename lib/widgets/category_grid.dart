import 'package:flutter/material.dart';
import '../constants/category_meta.dart';
import '../models/transaction.dart';
import '../theme/theme.dart';

/// 分类网格
///
/// 4列网格显示 Emoji 分类
class CategoryGrid extends StatelessWidget {
  final TransactionType type;
  final String? selectedCategoryId;
  final ValueChanged<CategoryMeta> onSelect;

  const CategoryGrid({
    super.key,
    required this.type,
    this.selectedCategoryId,
    required this.onSelect,
  });

  @override
  Widget build(BuildContext context) {
    final categories = getCategoriesByType(type);

    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 4,
        childAspectRatio: 0.85,
        crossAxisSpacing: AppSpacing.md,
        mainAxisSpacing: AppSpacing.md,
      ),
      itemCount: categories.length,
      itemBuilder: (context, index) {
        final category = categories[index];
        final isSelected = category.id == selectedCategoryId;

        return CategoryGridItem(
          category: category,
          isSelected: isSelected,
          onTap: () => onSelect(category),
        );
      },
    );
  }
}

/// 分类网格项
class CategoryGridItem extends StatelessWidget {
  final CategoryMeta category;
  final bool isSelected;
  final VoidCallback onTap;

  const CategoryGridItem({
    super.key,
    required this.category,
    required this.isSelected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);

    return GestureDetector(
      onTap: onTap,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 56,
            height: 56,
            decoration: BoxDecoration(
              color: isSelected
                  ? category.color.withValues(alpha: 0.2)
                  : colors.backgroundSecondary,
              borderRadius: BorderRadius.circular(AppRadius.lg),
              border: isSelected
                  ? Border.all(color: category.color, width: 2)
                  : null,
            ),
            child: Center(
              child: Text(
                category.emoji,
                style: const TextStyle(fontSize: 28),
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.xs),
          Text(
            category.name,
            style: TextStyle(
              fontSize: 12,
              fontWeight: isSelected ? FontWeight.w600 : FontWeight.w500,
              color: isSelected ? category.color : colors.textSecondary,
            ),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
        ],
      ),
    );
  }
}
