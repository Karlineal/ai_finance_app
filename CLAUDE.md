# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run all unit tests
./gradlew testDebugUnitTest

# Run a single test class
./gradlew :feature:importer:testDebugUnitTest --tests "com.aifinance.feature.importer.AlipayBillParserTest"

# Run a single test method
./gradlew :feature:importer:testDebugUnitTest --tests "com.aifinance.feature.importer.AlipayBillParserTest.parse valid alipay csv returns correct transactions"

# Code formatting check
./gradlew spotlessCheck

# Build project (including lint checks)
./gradlew build
```

## Key Architecture

### Module Dependency Graph
```
app (entry point, navigation)
├── feature:home           → core:ui, core:data, core:model
├── feature:transactions   → core:ui, core:data, core:model
├── feature:add_transaction
├── feature:statistics
├── feature:settings
├── feature:budget
├── feature:scheduled
├── feature:category_management
├── feature:importer       (Apache POI for bill import)
├── feature:ai             (DeepSeek API chat)
├── feature:ocr            (PaddleOCR integration)
└── core:designsystem      → core:model
```

### Core Modules
- **core:model** — Domain entities (Transaction, Account, Category, BudgetPlan, etc.)
- **core:database** — Room database with DAOs (AccountDao, TransactionDao, CategoryDao, ScheduledRuleDao)
- **core:data** — Repository implementations, network layer (Retrofit for DeepSeek/PaddleOCR APIs), DataStore preferences, scheduled rule calculation
- **core:ui** — Shared Compose UI components
- **core:designsystem** — Theme (Material 3), colors, typography

### Architectural Pattern
MVVM + Clean Architecture:
- **UI Layer**: Compose screens + ViewModels (per feature module)
- **Data Layer**: Repository pattern (interfaces in core:data, implementations with `*Impl` suffix)
- **Database Layer**: Room entities (`*Entity.kt`) with DAOs, type converters in `Converters.kt`
- **DI Layer**: Hilt modules (`@Module @InstallIn(SingletonComponent::class)`)

### Key Technical Decisions
- API keys (DeepSeek, PaddleOCR) stored in `local.properties`, injected via `BuildConfig` at compile time
- Navigation via Compose Navigation with extension functions per feature (e.g., `homeScreen()`, `statisticsScreen()`)
- Scheduled transactions use WorkManager + AlarmManager + BootReceiver (triple-redundant scheduling)
- Import supports WeChat/Alipay/bank CSVs via Apache POI parsing
- Convention plugins in `build-logic/convention/` define reusable Gradle plugin IDs (`aifinance.android.*`)

### Tech Stack
| Component | Choice |
|-----------|--------|
| UI | Jetpack Compose + Material Design 3 |
| DI | Hilt (v2.50) |
| DB | Room (v2.6.1) |
| Network | Retrofit 2.9.0 + OkHttp 4.12.0 + Kotlinx Serialization |
| Async | Kotlin Coroutines + Flow |
| Images | Coil 2.5.0 |
| Animation | Lottie Compose 6.3.0 |
| Excel | Apache POI 5.2.5 |

### LSP (Language Server Protocol)
Kotlin LSP (`kotlin-language-server` v1.3.13) is configured in `.claude/settings.json`.
Use the LSP tool for symbol-level navigation (`goToDefinition`, `findReferences`, `hover`, `documentSymbol`) — especially in multi-module Gradle projects.

Note: first-time LSP init triggers a Gradle project import which can take ~2min. If LSP times out, run `./gradlew compileDebugKotlin` first to warm the cache, then retry in a fresh session.

### Testing
- Unit tests: JUnit 4 + MockK + Turbine (for Flow testing) + kotlinx-coroutines-test
- Robolectric for ViewModel tests that need Android framework
- Compose UI Test framework for UI tests
- Tests primarily in `feature/importer/src/test/` and `feature/home/src/test/`

## Project Conventions

### Naming Patterns
| Type | Pattern | Example |
|------|---------|---------|
| Entity | `*Entity` | `TransactionEntity`, `AccountEntity` |
| DAO | `*Dao` | `TransactionDao`, `AccountDao` |
| Repository Interface | `*Repository` | `TransactionRepository` |
| Repository Impl | `*RepositoryImpl` | `TransactionRepositoryImpl` |
| ViewModel | `*ViewModel` | `HomeViewModel`, `AddTransactionViewModel` |
| Screen (Composable) | `*Screen` | `HomeScreen`, `AddTransactionScreen` |
| Navigation ext | `navController.featureName()` | `navController.home()` |
| DI Module | `*Module` | `NetworkModule`, `DatabaseModule` |
| Convention Plugin | `aifinance.android.*` | `aifinance.android.compose` |

### Code Style
- Kotlin idiomatic: data class, sealed class, extension function, scope function
- Null safety: prefer `?.` and `?:` over `!!`
- Coroutines: structured concurrency, `viewModelScope`, `Dispatchers.IO` for DB/network
- Flow: `StateFlow` for UI state, `SharedFlow` for one-shot events
- Compose: avoid `remember` with complex logic, use `viewModel()` for state
- Hilt: `@Inject constructor` for dependencies, `@HiltViewModel` for ViewModels

### File Organization
```
feature/<name>/
├── src/main/java/com/aifinance/feature/<name>/
│   ├── <Name>Screen.kt          # Compose UI
│   ├── <Name>ViewModel.kt       # State management
│   ├── <Name>UiState.kt         # UI state data class (optional)
│   ├── component/               # Reusable UI components (optional)
│   ├── navigation/
│   │   └── <Name>Navigation.kt  # NavHost extension functions
│   └── state/                   # Additional state classes (optional)
├── src/test/                    # Unit tests
└── build.gradle.kts
```

### Common Pitfalls
- **Room Migration 不可省略**: schema 变更必须有对应 Migration，否则安装新版会崩溃
- **Navigation route 唯一**: 重复 route 导致运行时异常
- **Hilt 多模块**: 每个 feature 模块独立 `@HiltViewModel`，跨模块注入需在 core:data 提供
- **Compose Recomposition**: 避免在 Composable 中创建新对象（用 `remember` 或 `derivedStateOf`）
- **API Key 安全**: 永远不要硬编码 key，用 `local.properties` + `BuildConfig`
- **Git Worktree**: 项目有 `.claude/worktrees/` 和 `.worktrees/` 目录，切换分支前确认当前 worktree

### Debugging Rules
- **闪退/崩溃第一步永远是 logcat**: `adb logcat -d -s AndroidRuntime:E`，不要先逐文件读代码
- **Room Migration 失败**: 检查 Entity 类型和 Migration SQL 的列类型是否一致（TypeConverter 映射）
- **构建成功但运行崩溃**: 编译通过不代表运行时正确，优先看 crash log

### Claude Code 工作流规则
- **WSL Gradle 编译慢（~5min）**: 永远前台执行、设 timeout 300000，**禁止后台任务**；只在最终验证时编译一次，中间改动不编译
- **读文件要批量**: 先用 `find`/`grep` 定位所有相关文件，一次性读完再改，不要读一个改一个来回折腾
- **简单改动不重复确认**: 改完直接编译验证 + commit，不要反复读文件确认

### ⚠️ 分支合并流程（强制执行）

**必须严格遵守以下流程，禁止跳过任何步骤：**

#### 1. 代码审查阶段
- 使用 workflow 并行检查分支代码质量
- 生成问题清单（按严重程度排序）
- **等待用户确认**问题清单后才能开始修复

#### 2. 代码修复阶段
- 按用户确认的问题清单逐项修复
- 解决与 main 分支的合并冲突
- 提交修复到 feature 分支

#### 3. 编译验证阶段（Windows 端）
- 切换到 feature 分支：`git checkout feature/<branch-name>`
- 执行编译：`./gradlew assembleDebug`
- **等待编译成功**

#### 4. 模拟器测试阶段（用户手动执行）
- 在 Android Studio 中运行到模拟器
- **用户手动测试所有相关功能**
- **用户确认测试通过**

#### 5. 合并阶段
- **只有用户明确确认测试通过后**，才能执行合并
- 创建 PR：`gh pr create --base main --head feature/<branch-name>`
- 合并 PR：`gh pr merge <pr-number> --merge --delete-branch`

#### ⛔ 禁止事项
- ❌ 编译未通过就合并
- ❌ 用户未测试就合并
- ❌ 用户未确认就合并
- ❌ 跳过模拟器测试直接合并

#### ✅ 正确流程示例
```
1. Claude: "分支已修复，请在 Windows 端编译并测试"
2. 用户: "编译成功"
3. 用户: "测试通过，功能正常"
4. Claude: "好的，现在合并到 main"
```