import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import '../theme/theme.dart';
import '../widgets/amount_text.dart';

/// 甜甜圈统计图表组件
///
/// 中心显示总计，环形显示分类占比
class StatisticsDonutChart extends StatefulWidget {
  final List<Map<String, dynamic>> data;
  final double total;
  final String title;
  final String subtitle;

  const StatisticsDonutChart({
    super.key,
    required this.data,
    required this.total,
    this.title = '总计',
    this.subtitle = '',
  });

  @override
  State<StatisticsDonutChart> createState() => _StatisticsDonutChartState();
}

class _StatisticsDonutChartState extends State<StatisticsDonutChart> {
  int _touchedIndex = -1;

  @override
  Widget build(BuildContext context) {
    final colors = AppColors.of(context);

    return AspectRatio(
      aspectRatio: 1.3,
      child: Stack(
        alignment: Alignment.center,
        children: [
          // 甜甜圈图
          PieChart(
            PieChartData(
              pieTouchData: PieTouchData(
                touchCallback: (FlTouchEvent event, pieTouchResponse) {
                  setState(() {
                    if (!event.isInterestedForInteractions ||
                        pieTouchResponse == null ||
                        pieTouchResponse.touchedSection == null) {
                      _touchedIndex = -1;
                      return;
                    }
                    _touchedIndex =
                        pieTouchResponse.touchedSection!.touchedSectionIndex;
                  });
                },
              ),
              borderData: FlBorderData(show: false),
              sectionsSpace: 2,
              centerSpaceRadius: 70,
              sections: _buildSections(),
            ),
          ),

          // 中心信息
          Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                widget.title,
                style: TextStyle(
                  fontSize: 14,
                  color: colors.textTertiary,
                ),
              ),
              const SizedBox(height: 4),
              AmountText.expense(
                widget.total,
                style: TextStyle(
                  fontSize: 28,
                  fontWeight: FontWeight.bold,
                  color: colors.textPrimary,
                ),
              ),
              if (widget.subtitle.isNotEmpty) ...[
                const SizedBox(height: 4),
                Text(
                  widget.subtitle,
                  style: TextStyle(
                    fontSize: 12,
                    color: colors.textSecondary,
                  ),
                ),
              ],
            ],
          ),
        ],
      ),
    );
  }

  List<PieChartSectionData> _buildSections() {
    if (widget.data.isEmpty) return [];

    return widget.data.asMap().entries.map((entry) {
      final index = entry.key;
      final item = entry.value;
      final isTouched = index == _touchedIndex;
      final radius = isTouched ? 55.0 : 45.0;
      final color = Color(item['categoryColor'] as int);
      final value = (item['total'] as double).abs();

      return PieChartSectionData(
        color: color,
        value: value,
        title: '',
        radius: radius,
        badgeWidget: isTouched ? _buildBadge(item, color) : null,
        badgePositionPercentageOffset: 1.1,
      );
    }).toList();
  }

  Widget _buildBadge(Map<String, dynamic> item, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(8),
        boxShadow: [
          BoxShadow(
            color: color.withOpacity(0.4),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Text(
        item['categoryName'] as String,
        style: const TextStyle(
          color: Colors.white,
          fontSize: 11,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}
