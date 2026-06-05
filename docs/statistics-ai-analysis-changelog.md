# 图表统计 AI 分析卡片 → iCookie 跳转功能

> 记录时间：2026-06-05  
> 当前误挂在分支：`5.29-saving-reviewed`（该分支主题是储蓄目标/coloring_view，与本次功能无关）  
> 建议基线：从 `origin/main` 新建功能分支进行测试

---

## 功能概述

1. **图表统计页**「AI分析」占位卡片 → 票据风格可点击卡片
2. 进入/切换周期时，DeepSeek 自动生成 ~30 字消费建议 + 4 条快捷问题
3. 点击卡片 → 跳转主页 **iCookie** 标签，自动带入当前周/月/年账单上下文
4. iCookie 空状态展示周期相关快捷问题（如「给一些省钱建议」）

---

## 改动文件清单（11 个）

### 新增（3）

| 文件 | 说明 |
|------|------|
| `core/model/.../StatisticsAnalysisContext.kt` | 统计页 → AI 聊天的上下文数据模型 |
| `core/data/.../StatisticsAnalysisBridge.kt` | 跨页面桥接单例（pending context + 打开 AI Tab 信号） |
| `feature/home/.../HomeContainerViewModel.kt` | 主页容器 VM，监听 bridge 并消费上下文 |

### 修改（8）

| 文件 | 变更摘要 |
|------|----------|
| `core/data/.../ai/AIRepository.kt` | 新增 `generateOneShot()`，一次性 AI 调用，不写入聊天历史 |
| `feature/statistics/.../StatisticsScreen.kt` | 替换 `AiPlaceholderCard` 为 `StatisticsAiAnalysisCard`；ViewModel 接入 DeepSeek 生成 tip/suggestions |
| `feature/statistics/.../StatisticsNavigation.kt` | 新增 `onNavigateToAiAssistant` 回调 |
| `feature/home/.../AssistantUiState.kt` | 新增 `customSuggestions`、`statisticsContextLabel` |
| `feature/home/.../AssistantViewModel.kt` | `applyStatisticsContext()` + 周期账单 system prompt 注入 |
| `feature/home/.../AiAssistantScreen.kt` | 支持统计页跳转后的自定义快捷问题 |
| `feature/home/.../HomeContainerScreen.kt` | 监听 bridge，自动切到 iCookie Tab |
| `app/.../AiFinanceNavHost.kt` | 统计页点击卡片时 `navigate(HOME_ROUTE)` 并触发 Tab 切换 |

### 本地配置（未纳入 Git）

| 文件 | 说明 |
|------|------|
| `local.properties` | 需手动添加 `DEEPSEEK_API_KEY=...`（已在 .gitignore） |

---

## 数据流

```
StatisticsScreen (点击 AI 卡片)
  → StatisticsViewModel.prepareAiNavigation()
  → StatisticsAnalysisBridge.navigateToAiWithContext(context)
  → NavHost navigate(HOME_ROUTE)
  → HomeContainerScreen 切到 HomeTopTab.AI_ASSISTANT
  → AiAssistantScreen 消费 context
  → AssistantViewModel.applyStatisticsContext()
  → 展示周期快捷问题 + 聊天时注入周期账单 system prompt
```

---

## 迁移到新分支步骤

```powershell
# 1. 保存当前改动（含未跟踪文件）
git stash push -u -m "feat: statistics AI analysis card -> icookie"

# 2. 拉取 main 并新建功能分支
git fetch origin main
git checkout -b feat/statistics-ai-analysis origin/main

# 3. 恢复改动
git stash pop

# 4. 确认 local.properties 中有 DeepSeek Key
# DEEPSEEK_API_KEY=your_key_here

# 5. 编译验证
.\gradlew :app:compileDebugKotlin
```

若 stash 冲突，可使用根目录备份 patch：

- `statistics-ai-analysis.patch` — 已跟踪文件的 diff
- 新增 3 个文件需从 stash 或手动复制

---

## 测试要点

- [ ] 图表统计页加载后 AI 卡片显示生成中的文案，随后变为 AI 建议
- [ ] 切换 周/月/年 或翻页后，建议文案刷新
- [ ] 点击卡片跳转到 iCookie，标题显示「分析 {周期}」
- [ ] 快捷问题与当前周期相关，点击可正常发起对话
- [ ] AI 回答基于所选周期的账单数据（非默认「本月」）
