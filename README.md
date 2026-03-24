# AI记账助手 - Flutter Android 版

基于 Flutter 开发的 Android 智能记账应用，集成 AI 发票识别和自动分类功能。

## 功能特性

- ✅ **基础记账** - 收入/支出记录、分类管理
- ✅ **AI 发票识别** - 使用 Kimi Coding K2.5（免费）
- ✅ **智能分类** - 规则引擎 + OpenAI API
- ✅ **数据导入** - 支持支付宝/微信 CSV 导入
- ✅ **统计图表** - 月度收支分析
- ✅ **本地存储** - SQLite 数据库

## 项目结构

```
lib/
├── main.dart                      # 应用入口
├── models/
│   └── transaction.dart           # 交易数据模型
├── database/
│   └── database_helper.dart       # SQLite 数据库操作
├── bloc/
│   ├── transaction_bloc.dart      # 状态管理逻辑
│   ├── transaction_event.dart     # 事件定义
│   └── transaction_state.dart     # 状态定义
├── screens/
│   ├── home_screen.dart           # 首页
│   ├── add_transaction_screen.dart    # 添加交易
│   ├── statistics_screen.dart     # 统计页面
│   ├── receipt_recognition_screen.dart # 发票识别
│   └── settings_screen.dart       # 设置
├── services/
│   ├── ai_service.dart            # AI 服务（OCR + 分类）
│   └── csv_import_service.dart    # CSV 导入服务
└── widgets/
    ├── summary_card.dart          # 收支摘要卡片
    └── transaction_list_item.dart # 交易列表项
```

## 已完成的功能

### Week 1 基础框架 ✅
- [x] 项目结构和依赖配置
- [x] SQLite 数据库（sqflite）
- [x] BLoC 状态管理
- [x] 首页收支展示
- [x] 添加交易（支出/收入）
- [x] 交易列表
- [x] 统计图表（fl_chart）
- [x] 分类管理

### Week 2 AI 功能 ✅
- [x] AI 发票识别（Kimi Coding K2.5）
- [x] 智能分类（规则引擎 + OpenAI）
- [x] CSV 导入框架（支付宝/微信）
- [x] 设置页面（API Key 配置）

## 环境要求

- Flutter SDK >= 3.0.0
- Dart SDK >= 3.0.0
- Android SDK (API 21+)

## 安装运行

### 1. 确保环境已安装
```bash
flutter doctor
```

### 2. 获取依赖
```bash
flutter pub get
```

### 3. 配置 API Keys（可选）

在设置页面配置：
- **Kimi Coding API Key** - 用于发票识别（免费）
- **OpenAI API Key** - 用于智能分类（可选）

### 4. 运行应用
```bash
# 连接手机或启动模拟器
flutter devices

# 运行
flutter run
```

### 5. 构建 APK
```bash
flutter build apk --release
```

## API 配置

### Kimi Coding（推荐，免费）
- 端点: `https://api.kimi.com/coding/v1`
- 模型: `kimi-coding/k2.5`
- 获取 API Key: https://platform.kimi.com

### OpenAI（备选）
- 模型: `gpt-3.5-turbo`
- 获取 API Key: https://platform.openai.com

## 技术栈

- **框架**: Flutter 3.x
- **状态管理**: flutter_bloc
- **数据库**: sqflite (SQLite)
- **图表**: fl_chart
- **网络**: dio
- **图片**: image_picker

## 下一步开发计划

### Week 3: 数据导入完善
- [ ] 支付宝 CSV 导入测试
- [ ] 微信 CSV 导入测试
- [ ] 数据导出备份

### Week 4: 高级功能
- [ ] 预算预测
- [ ] 消费分析
- [ ] 数据可视化增强

### Week 5: 优化发布
- [ ] UI/UX 优化
- [ ] 性能优化
- [ ] 打包发布

## 许可

MIT License
