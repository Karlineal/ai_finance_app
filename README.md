# 🍪 iCookie - 智能记账助手

一个基于 Android 原生开发的智能记账应用，采用现代化的模块化架构设计，支持 AI 辅助记账、OCR 票据识别、交易导入、资产管理等核心功能。

---

## 🏗️ 项目架构

本项目采用 **Clean Architecture** 结合 **模块化设计**，参考 Google 官方推荐的 Now in Android 架构模式。

### 模块结构

```
ai-finance-android/
├── app/                          # 应用入口模块
│   └── src/main/java/com/aifinance/app/
│       ├── MainActivity.kt       # 主Activity，集成侧边栏导航
│       └── navigation/
│           └── AiFinanceNavHost.kt   # 全局导航配置
│
├── core/                         # 核心层模块
│   ├── designsystem/            # UI设计系统（主题、颜色、字体）
│   ├── ui/                      # 通用UI组件
│   ├── model/                   # 数据模型定义
│   ├── database/                # Room数据库（实体、DAO）
│   └── data/                    # 数据仓库实现
│       ├── network/             # 网络层（API接口、数据模型）
│       │   ├── api/             # DeepSeek API, PaddleOCR API
│       │   └── repository/ai/  # AI Repository
│       └── repository/         # 数据仓库（StatisticsAnalysisBridge 等）
│
├── feature/                      # 功能模块（按特性划分）
│   ├── home/                    # 首页 - 记账记录与AI助手
│   │   ├── AiAssistantScreen.kt # AI 对话界面（iCookie 软件图标）
│   │   └── AddTransactionBottomSheet.kt  # 添加交易（手动记账 | AI记录）
│   ├── transactions/            # 交易列表与详情
│   ├── add_transaction/         # 添加交易
│   ├── statistics/              # 统计分析（含 AI 分析桥接）
│   ├── settings/                # 设置页面
│   ├── budget/                  # 预算管理
│   ├── scheduled/               # 定时记账
│   ├── category_management/     # 分类管理
│   ├── importer/                # 交易导入（微信/支付宝/银行账单解析）
│   ├── savings_goal/            # 攒钱计划（热力图、打卡日历）
│   ├── ai/                      # AI 对话功能
│   └── ocr/                     # OCR 票据识别
│
└── build-logic/                  # 构建逻辑（Convention插件）
    └── convention/
```

### 技术栈

| 层级 | 技术 |
|------|------|
| **UI** | Jetpack Compose + Material Design 3 |
| **架构** | MVVM + Repository Pattern + Clean Architecture |
| **依赖注入** | Hilt |
| **数据库** | Room v2.6.1 (SQLite), 当前版本 v11 |
| **网络** | Retrofit 2.9.0 + OkHttp 4.12.0 + Kotlinx Serialization |
| **导航** | Compose Navigation |
| **异步** | Kotlin Coroutines + Flow |
| **图片加载** | Coil |
| **动画** | Lottie Compose |
| **Excel 解析** | Apache POI (微信/支付宝/银行 xlsx 账单) |

---

## 最近更新

### 2026-06

- **手动记账界面优化**：
  - 移除手动记账区的相机拍照按钮，只保留从相册选择图片功能
  - 简化备注区域的附件操作，避免相机黑屏问题
- **Room 数据库迁移修复（v10→v11）**：
  - 修复 Migration_10_11 SQL 语法错误 — CASE 表达式误放在 INSERT INTO 列名位置
  - transactions 和 savings_records 表的 createdAt/updatedAt 字段从 TEXT 迁移到 INTEGER
- **AI 对话界面图标恢复**：
  - 恢复 PR#18 revert 时误删的 ICookieAvatar.kt 和 icookie_icon.png
  - AI 对话界面 Hero 区域使用软件图标替代旧版手绘曲奇饼干
- **热力图视觉优化**：
  - 右侧渐变淡出遮罩（24dp），替代硬截断，深色/浅色主题自适应
  - 格子尺寸 13dp → 14dp，视觉更饱满
  - 容器结构重构：外层 Box + 内层滚动 Box + 渐变遮罩三层分离
- **设置页面完善**：功能收窄到用户请求的范围，交互体验优化
- **统计分析桥接**：新增 StatisticsAnalysisBridge，打通统计页与 AI 分析的数据通道

### 2025-06

- **AI 分析功能完善**：
  - 修复统计页"开始分析"导航问题，点击后自动跳转 AI 助手
  - AI 分析页面 UI 重构：去掉聊天气泡和头像，采用文档式全屏布局
  - DeepSeek API 升级：`deepseek-reasoner` → `deepseek-v4-flash`，响应更快
  - 新增思考模式配置：`reasoning_effort` 控制推理强度
  - 新增 [DeepSeek API 调用指南](docs/deepseek-api-guide.md) 文档
- **攒钱计划日历视图修复**：
  - 修正热力图日期范围，已完成计划显示完整打卡记录
  - 修正 52 周/365 天计划进度计算（按周期而非天数）
  - 改进打卡期数计算，根据日期自动计算实际期数
  - 优化图例文案："少/多" → "未打卡/已打卡"
  - 已完成/失败的计划也能查看历史打卡记录
- **死代码清理**：删除未使用的 CheckInCalendarViewModel 等4个文件
- **弃用像素形状系统清理**：删除 `SavingsPixelShapes.kt`（心形/小木屋/小狗像素网格，已被卡片列表+热力图双视图替代）及相关文档
- **代码质量提升**：
  - 自动格式化 187 个文件（spotlessApply）
  - 优化 ktlint 配置，禁用过于严格的规则
  - 更新 .editorconfig 统一代码风格

### 2025-05

- **攒钱计划深色主题适配**：创建/编辑界面全面支持深色模式
  - 顶部区域采用条件渲染，深色主题下使用渐变背景 + 品牌标题，浅色主题保持原插画
  - 所有硬编码颜色替换为 Material3 `ColorScheme` 语义化颜色
  - 储蓄方式卡片、周期选择器、快捷金额标签等组件完整适配深色主题
  - 底部说明文字使用品牌主色（primary），确保深色背景下清晰可见
- **自动储蓄计划**：支持按日/周/月定时从默认账户自动扣款存入攒钱计划
- **转账体验优化**：转出记录显示红色、转入记录显示绿色；删除转账时自动级联删除配对记录并同步攒钱计划金额

---

## ✨ 已实现功能

### 🤖 AI 智能功能
- ✅ **AI 记账助手 (iCookie)**：智能对话助手，支持自然语言查询账目、分析消费习惯
  - 基于 DeepSeek API（`deepseek-v4-flash` 模型，支持思考模式）
  - 支持 Markdown 格式回复，自动适配浅色/深色主题
  - **文档式分析视图**：统计分析结果全屏铺满显示，阅读体验更佳
  - **智能提示轮播**：3 组共 12 个常用问题，支持"换一换"轮播
  - **Hero 欢迎区**：打招呼 + iCookie 形象展示

- ✅ **统计页 AI 分析**：一键生成财务分析报告
  - 从统计页直接调用 AI 分析当前周期数据
  - 自动生成包含收入、支出、分类占比的分析报告
  - AI 给出个性化的省钱建议和预算规划

- ✅ **AI 智能识别记账**：拍照或选择图片，自动识别账单信息
  - 基于 PaddleOCR API 进行票据文字识别
  - DeepSeek AI 解析金额、分类、商家、日期
  - 支持编辑识别结果后保存

### 🏠 首页 (Home)
- **双页卡片轮播**：
  - 📊 净资产卡片（金色渐变）- 展示总资产、负债、净资产
  - 📈 月度支出卡片（蓝色渐变）- 展示当月收入、支出、结余
- **金额隐私保护**：一键隐藏/显示金额
- **交易时间线**：按日期分组展示交易记录
- **快速记账**：悬浮按钮一键记账
- **侧边栏导航**：左滑呼出菜单，快速切换页面
  - 📅 **记账热力图**：3x10 网格展示当月记账打卡情况
  - 🔥 **连续记账统计**：记录坚持天数与连续记录天数

### 📝 交易管理
- **快速记账**：
  - 手动记账：金额输入、分类选择、账户选择、从相册添加附件
  - **AI 记录**：选图 → OCR 识别 → AI 解析 → 确认保存
- **交易列表**：支持按月份筛选
- **日历记录**：以日历视图查看全部交易记录，按日期快速定位
- **交易详情**：点击交易查看/编辑详情
- **快速分类**：点击分类标签可直接修改分类
- **长按删除**：长按交易记录可删除
- **交易类型**：收入、支出、转账
- **转账记录关联删除**：删除转账记录时自动删除关联的配对记录（转入/转出），并同步更新攒钱计划金额
- **转账颜色区分**：转出记录显示红色，转入记录显示绿色，增强可读性

### ⏰ 定时记账
- **定时规则管理**：创建周期性记账规则
- **自动提醒**：到期自动生成分类记账提醒
- **后台可靠调度**：BootReceiver + AlarmManager + WorkManager 三重保障

### 💰 预算管理
- **月度预算规划**：设定每月总预算和分类预算
- **预算使用追踪**：实时显示已用金额、剩余金额、使用比例
- **今日预算计算**：根据剩余天数动态计算每日可用预算
- **分类预算看板**：各分类预算使用状态可视化
- **预算向导**：引导式创建首月预算方案

### 📊 资产管理
- **资产账户管理**
  - 添加账户：支持多种预设账户类型（储蓄卡、微信、支付宝、信用卡、股票基金等）
  - 编辑账户：点击账户可修改名称、余额、备注等
  - 删除账户：支持删除不再使用的账户
- **按类型分组展示**：账户按资金账户 / 理财账户 / 信用账户 / 其他分组，每组独立标题与颜色标识
- **颜色区分**：每个账户卡片使用专属颜色（图标背景 + 左侧彩色竖条），视觉上快速辨识账户类型
- **负债追踪**：信用卡、花呗、借呗等负债账户管理
- **净资产实时计算**：资产减去负债，实时计算净资产

### 🎯 攒钱计划
- **储蓄目标管理**：创建多个攒钱计划（52周存钱法、365天存钱法、12存单法、定额存钱等），设定目标金额与截止日期
- **自动储蓄计划**：支持按日/周/月定时自动从默认账户扣款存入攒钱计划
- **专属账户**：每个攒钱计划自动创建专属"小荷包"账户，独立管理资金
- **进度跟踪**：环形进度条展示完成比例，实时显示已存金额与剩余天数
- **打卡日历视图**：
  - 热力图展示打卡记录，直观显示坚持情况
  - 52 周计划按周显示，365 天计划按天显示
  - 已完成/失败的计划也能查看历史打卡记录
- **快捷存入**：一键快速存入预设金额，自动创建转入/转出转账记录
- **转账关联**：存入/取出自动创建配对转账记录，删除时级联同步
- **状态管理**：进行中 → 已完成 / 已放弃

### ⚙️ 设置
- **主题切换**：支持浅色 / 深色 / 跟随系统三种模式，全局深色模式已适配所有模块
- **清空历史**：一键清空所有交易记录并将账户余额归零
- **网络安全配置**：禁用明文通信，API 请求强制 HTTPS

### 🎨 UI/UX 特性
- **玻璃拟态设计** (Glassmorphism)：毛玻璃效果卡片
- **流畅动画**：页面切换、交互动画
- **Material Design 3**：符合现代设计规范
- **响应式布局**：适配不同屏幕尺寸
- **深色模式**：全模块深色主题适配，跟随系统/手动切换
- **主题感知组件**：AI 助手 Markdown、交易卡片等自动适配当前主题色

---

### 📥 账单导入
- **支持渠道**：微信账单、支付宝账单、银行对账单
- **文件格式**：CSV、Excel（.xlsx/.xls）
- **智能解析**：
  - 自动识别表头和日期/金额/摘要列
  - 支持多种日期格式（yyyy-MM-dd、yyyy/MM/dd 等）
  - 银行账单支持收支方向关键词匹配
- **智能批量处理**：
  - **智能去重**：自动检测已有交易重复 + 文件内重复，避免重复记账
  - **自动分类**：基于关键词规则匹配（收入 6 类 / 支出 8 类），自动归入对应分类
  - **置信度评估**：分类置信度 >= 0.72 的交易自动确认，低置信度标记为待审核
  - **导入统计**：实时显示导入条数、去重条数、自动分类条数
- **错误处理**：格式异常、空文件等边界条件有防护

---

## 🚧 开发中/规划功能

### 统计分析
- ✅ 📊 **图表展示**：饼图（KoalaPlot）、趋势折线图
- ✅ 📅 **周期分析**：按周、月、年统计
- ✅ 🏷️ **分类占比**：各分类支出占比分析
- ✅ 🤖 **AI 智能分析**：一键生成财务分析报告
- [ ] 📈 **趋势预测**：基于历史数据预测未来支出

### 其他功能
- [ ] ☁️ **数据备份/恢复**
- [ ] 🔒 **本地加密存储**
- [ ] 🔔 **记账提醒（推送通知）**

---

## 🔑 API 配置

本项目使用以下第三方 API 服务：

| 服务 | 用途 | 配置位置 | 文档 |
|------|------|----------|------|
| **DeepSeek** | AI 对话与分析 | `local.properties` | [API 调用指南](docs/deepseek-api-guide.md) |
| **PaddleOCR** | 票据 OCR 识别 | `local.properties` | - |

### 本地开发配置

1. 复制模板文件：
   ```bash
   cp local.properties.example local.properties
   ```

2. 编辑 `local.properties`，填入你的 API Key：
   ```properties
   sdk.dir=YOUR_SDK_PATH
   DEEPSEEK_API_KEY=your_deepseek_api_key_here
   PADDLEOCR_TOKEN=your_paddleocr_token_here
   ```

3. 重新构建项目，API Key 将通过 `BuildConfig` 自动注入。

> ⚠️ **注意**：`local.properties` 已加入 `.gitignore`，不会被提交到版本控制。请确保不要将包含真实 API Key 的文件上传至公开仓库。

---

## 🛠️ 环境要求

- **Android Studio**: Hedgehog (2023.1.1) 或更高版本
- **Kotlin**: 1.9.22
- **minSdk**: 26 (Android 8.0)
- **targetSdk**: 34 (Android 14)
- **Java**: 17

### 所需权限
- `INTERNET` - 网络访问
- `CAMERA` - 拍照识别
- `READ_EXTERNAL_STORAGE` - 读取相册图片
- `WRITE_EXTERNAL_STORAGE` - 缓存图片文件

---

## 🚀 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/Karlineal/ai_finance_app.git
cd ai_finance_app
```

### 2. 使用 Android Studio 打开

- 打开 Android Studio
- 选择 `Open an existing project`
- 选择项目根目录

### 3. 配置 API Key

- 复制 `local.properties.example` 为 `local.properties`
- 填入你的 `DEEPSEEK_API_KEY` 和 `PADDLEOCR_TOKEN`

### 4. 构建并运行

- 点击 ▶️ Run 按钮
- 选择模拟器或真机设备

---

## 📚 文档

| 文档 | 说明 |
|------|------|
| [DeepSeek API 调用指南](docs/deepseek-api-guide.md) | API 配置、请求格式、思考模式、错误排查 |

---

## 📁 核心数据结构

### Transaction（交易记录）
```kotlin
data class Transaction(
    val id: UUID,
    val accountId: UUID,          // 关联账户
    val categoryId: UUID?,        // 关联分类
    val type: TransactionType,    // 收入/支出/转账
    val amount: BigDecimal,       // 金额
    val currency: CurrencyCode,   // 币种
    val title: String,            // 标题
    val description: String?,     // 描述
    val date: LocalDate,          // 日期
    val time: Instant,            // 时间
    val isPending: Boolean,       // 是否待确认
    val sourceType: TransactionSourceType,  // 来源（手动/导入/OCR）
    val linkedTransactionId: UUID?,         // 关联转账记录ID（转入/转出配对）
    // ... AI 相关字段
)
```

### Account（账户）
```kotlin
data class Account(
    val id: UUID,
    val name: String,             // 账户名称
    val type: AccountType,        // 类型（现金/银行卡/信用卡/理财等）
    val initialBalance: BigDecimal,  // 初始余额
    val currentBalance: BigDecimal,  // 当前余额
    val color: Int,               // 账户主题色
    val icon: String,             // 图标 emoji
    val note: String?,            // 备注
    val includeInTotalAssets: Boolean,  // 是否计入总资产
    val isDefaultIncomeExpense: Boolean, // 是否为默认收支账户
    // ...
)
```

### SavingsRecord（攒钱记录）
```kotlin
data class SavingsRecord(
    val id: UUID,
    val savingsGoalId: UUID,      // 关联攒钱计划
    val amount: BigDecimal,       // 存入金额
    val periodIndex: Int,         // 期数索引（52周=1-52，365天=1-365）
    val date: LocalDate,          // 打卡日期
    val note: String?,            // 备注
    // ...
)
```

---

## 🧩 模块依赖关系

```
app
├── feature:home
│   ├── core:ui
│   ├── core:data
│   └── core:model
├── feature:transactions
│   └── ...
├── feature:statistics
│   └── ...
├── feature:settings
│   └── ...
├── feature:budget
│   └── ...
├── feature:scheduled
│   └── ...
├── feature:category_management
│   └── ...
├── feature:importer
│   └── ...
├── feature:savings_goal
│   ├── core:ui
│   ├── core:data
│   └── core:model
└── core:designsystem
    └── core:model
```

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

### 提交规范
- 使用 [Conventional Commits](https://www.conventionalcommits.org/)
- 提交前请确保代码通过 `./gradlew spotlessCheck`

---

## 📄 许可证

本项目采用 [MIT License](LICENSE) 开源协议。

---

## 📧 联系方式

如有问题或建议，欢迎通过 GitHub Issues 联系。

---

<p align="center">Made with ❤️ by Karlineal</p>
