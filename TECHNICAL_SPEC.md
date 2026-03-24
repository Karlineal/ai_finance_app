# AI记账助手 - 技术实现方案

## 项目结构

```
ai-finance-assistant/
├── packages/
│   ├── import-service/          # 数据导入服务
│   │   ├── src/
│   │   │   ├── parsers/
│   │   │   │   ├── alipay.ts    # 支付宝CSV解析
│   │   │   │   ├── wechat.ts    # 微信CSV解析
│   │   │   │   └── index.ts
│   │   │   ├── api/
│   │   │   │   ├── alipay-api.ts    # 支付宝API（可选）
│   │   │   │   └── wechat-api.ts    # 微信API（可选）
│   │   │   └── index.ts
│   │   ├── package.json
│   │   └── tsconfig.json
│   │
│   ├── ai-service/              # AI核心服务
│   │   ├── src/
│   │   │   ├── categorization/  # 智能分类
│   │   │   │   ├── cloud-ai.ts
│   │   │   │   ├── local-ai.ts
│   │   │   │   └── index.ts
│   │   │   ├── budget-prediction/   # 预算预测
│   │   │   │   └── index.ts
│   │   │   ├── savings-advice/      # 节省建议
│   │   │   │   └── index.ts
│   │   │   ├── ocr/             # 发票识别
│   │   │   │   ├── cloud-ocr.ts
│   │   │   │   ├── local-ocr.ts
│   │   │   │   └── index.ts
│   │   │   └── index.ts
│   │   ├── package.json
│   │   └── tsconfig.json
│   │
│   ├── web-app/                 # Web前端
│   │   ├── src/
│   │   │   ├── components/
│   │   │   ├── pages/
│   │   │   ├── hooks/
│   │   │   └── App.tsx
│   │   ├── package.json
│   │   └── vite.config.ts
│   │
│   └── shared/                  # 共享类型和工具
│       ├── src/
│       │   ├── types/
│       │   └── utils/
│       └── package.json
│
├── docker-compose.yml
├── README.md
└── package.json
```

---

## 1. 数据导入服务 (import-service)

### 1.1 支付宝CSV解析器

```typescript
// packages/import-service/src/parsers/alipay.ts
import { parse } from 'csv-parse/sync';
import { Transaction } from '@ai-finance/shared';

export interface AlipayCSVRow {
  '交易号': string;
  '商家订单号': string;
  '交易创建时间': string;
  '付款时间': string;
  '最近修改时间': string;
  '交易来源地': string;
  '类型': string;
  '交易对方': string;
  '商品名称': string;
  '金额（元）': string;
  '收/支': string;
  '交易状态': string;
  '服务类型': string;
  '资金方式': string;
  '备注': string;
}

export class AlipayParser {
  /**
   * 解析支付宝CSV文件
   */
  parseCSV(buffer: Buffer): Transaction[] {
    const content = buffer.toString('utf-8');
    
    // 跳过前4行（支付宝CSV有标题信息）
    const lines = content.split('\n');
    const csvContent = lines.slice(4).join('\n');
    
    const records = parse(csvContent, {
      columns: true,
      skip_empty_lines: true,
      encoding: 'utf-8'
    }) as AlipayCSVRow[];

    return records
      .filter(row => row['交易状态'] === '交易成功')
      .map(row => this.transformToTransaction(row));
  }

  private transformToTransaction(row: AlipayCSVRow): Transaction {
    const amount = parseFloat(row['金额（元）']);
    const type = row['收/支'] === '支出' ? 'expense' : 'income';
    
    return {
      id: `alipay_${row['交易号']}`,
      date: this.parseDate(row['交易创建时间']),
      payee: row['交易对方'],
      amount: type === 'expense' ? -Math.abs(amount) : Math.abs(amount),
      notes: row['商品名称'],
      category: null, // 待AI分类
      type,
      source: 'alipay',
      rawData: row
    };
  }

  private parseDate(dateStr: string): Date {
    // 格式：2024-01-15 14:30:25
    return new Date(dateStr.replace(/-/g, '/'));
  }
}
```

### 1.2 微信CSV解析器

```typescript
// packages/import-service/src/parsers/wechat.ts
import { parse } from 'csv-parse/sync';
import iconv from 'iconv-lite';
import { Transaction } from '@ai-finance/shared';

export interface WechatCSVRow {
  '交易时间': string;
  '交易类型': string;
  '交易对方': string;
  '商品': string;
  '收/支': string;
  '金额(元)': string;
  '支付方式': string;
  '当前状态': string;
  '交易单号': string;
  '商户单号': string;
  '备注': string;
}

export class WechatParser {
  /**
   * 解析微信CSV文件（微信CSV是GBK编码）
   */
  parseCSV(buffer: Buffer): Transaction[] {
    // 微信CSV使用GBK编码，需要转换
    const content = iconv.decode(buffer, 'gbk');
    
    // 跳过前16行（微信CSV有标题和说明）
    const lines = content.split('\n');
    const csvContent = lines.slice(16).join('\n');
    
    const records = parse(csvContent, {
      columns: true,
      skip_empty_lines: true,
      encoding: 'utf-8'
    }) as WechatCSVRow[];

    return records
      .filter(row => row['当前状态'] === '支付成功' || row['当前状态'] === '已存入零钱')
      .map(row => this.transformToTransaction(row));
  }

  private transformToTransaction(row: WechatCSVRow): Transaction {
    const amountStr = row['金额(元)'].replace('¥', '').trim();
    const amount = parseFloat(amountStr);
    const type = row['收/支'] === '支出' ? 'expense' : 'income';
    
    return {
      id: `wechat_${row['交易单号']}`,
      date: this.parseDate(row['交易时间']),
      payee: row['交易对方'],
      amount: type === 'expense' ? -Math.abs(amount) : Math.abs(amount),
      notes: row['商品'],
      category: null, // 待AI分类
      type,
      source: 'wechat',
      rawData: row
    };
  }

  private parseDate(dateStr: string): Date {
    // 格式：2024-01-15 14:30:25
    return new Date(dateStr.replace(/-/g, '/'));
  }
}
```

### 1.3 导入服务主入口

```typescript
// packages/import-service/src/index.ts
import { AlipayParser } from './parsers/alipay';
import { WechatParser } from './parsers/wechat';
import * as actual from '@actual-app/api';
import { Transaction } from '@ai-finance/shared';

export type ImportSource = 'alipay' | 'wechat';

export interface ImportOptions {
  source: ImportSource;
  file: Buffer;
  autoCategorize?: boolean; // 是否自动AI分类
}

export interface ImportResult {
  success: boolean;
  imported: number;
  skipped: number;
  errors: string[];
  transactions: Transaction[];
}

export class ImportService {
  private alipayParser = new AlipayParser();
  private wechatParser = new WechatParser();

  async import(options: ImportOptions): Promise<ImportResult> {
    const { source, file, autoCategorize = true } = options;
    
    // 1. 解析CSV
    let transactions: Transaction[];
    try {
      transactions = this.parseTransactions(source, file);
    } catch (error) {
      return {
        success: false,
        imported: 0,
        skipped: 0,
        errors: [`解析失败: ${error.message}`],
        transactions: []
      };
    }

    // 2. 去重（基于交易ID）
    const existingIds = await this.getExistingTransactionIds();
    const newTransactions = transactions.filter(
      t => !existingIds.has(t.id)
    );

    // 3. AI分类（如果启用）
    if (autoCategorize) {
      newTransactions = await this.categorizeTransactions(newTransactions);
    }

    // 4. 写入Actual Budget
    let imported = 0;
    const errors: string[] = [];
    
    for (const transaction of newTransactions) {
      try {
        await actual.addTransaction('checking', {
          date: transaction.date.toISOString().split('T')[0],
          amount: transaction.amount,
          payee: transaction.payee,
          notes: transaction.notes,
          category: transaction.category
        });
        imported++;
      } catch (error) {
        errors.push(`导入失败 ${transaction.id}: ${error.message}`);
      }
    }

    return {
      success: errors.length === 0,
      imported,
      skipped: transactions.length - newTransactions.length,
      errors,
      transactions: newTransactions
    };
  }

  private parseTransactions(source: ImportSource, file: Buffer): Transaction[] {
    switch (source) {
      case 'alipay':
        return this.alipayParser.parseCSV(file);
      case 'wechat':
        return this.wechatParser.parseCSV(file);
      default:
        throw new Error(`不支持的导入源: ${source}`);
    }
  }

  private async getExistingTransactionIds(): Promise<Set<string>> {
    // 从Actual Budget获取已有交易ID
    const transactions = await actual.getTransactions();
    return new Set(transactions.map(t => t.imported_id).filter(Boolean));
  }

  private async categorizeTransactions(
    transactions: Transaction[]
  ): Promise<Transaction[]> {
    // 调用AI服务进行分类
    const { categorizeBatch } = await import('@ai-finance/ai-service');
    return categorizeBatch(transactions);
  }
}
```

---

## 2. AI分类服务

### 2.1 云端AI分类（OpenAI）

```typescript
// packages/ai-service/src/categorization/cloud-ai.ts
import OpenAI from 'openai';
import { Transaction, Category } from '@ai-finance/shared';

const openai = new OpenAI({
  apiKey: process.env.OPENAI_API_KEY
});

const PREDEFINED_CATEGORIES: Category[] = [
  { id: 'food', name: '餐饮', keywords: ['餐厅', '外卖', '超市', '买菜'] },
  { id: 'transport', name: '交通', keywords: ['地铁', '公交', '打车', '加油'] },
  { id: 'shopping', name: '购物', keywords: ['淘宝', '京东', '天猫', '商场'] },
  { id: 'entertainment', name: '娱乐', keywords: ['电影', '游戏', '会员', '视频'] },
  { id: 'utilities', name: '生活缴费', keywords: ['电费', '水费', '燃气', '宽带'] },
  { id: 'housing', name: '居住', keywords: ['房租', '房贷', '物业'] },
  { id: 'medical', name: '医疗', keywords: ['医院', '药店', '体检'] },
  { id: 'education', name: '教育', keywords: ['课程', '培训', '书籍'] },
  { id: 'travel', name: '旅行', keywords: ['酒店', '机票', '火车票', '景点'] },
  { id: 'salary', name: '工资', keywords: ['工资', '奖金'], type: 'income' },
  { id: 'transfer', name: '转账', keywords: ['红包', '转账'] },
  { id: 'transfer', name: '转账', keywords: ['转账', '红包'] },
  { id: 'other', name: '其他', keywords: [] }
];

export interface CategorizationResult {
  categoryId: string;
  categoryName: string;
  confidence: number;
  reasoning: string;
}

export async function categorizeWithOpenAI(
  transaction: Transaction
): Promise<CategorizationResult> {
  const prompt = `
你是一位财务分类专家。请将以下交易分类到最合适的类别。

交易信息：
- 商家: ${transaction.payee}
- 金额: ${transaction.amount} 元
- 备注: ${transaction.notes || '无'}
- 类型: ${transaction.type === 'expense' ? '支出' : '收入'}

可选类别：
${PREDEFINED_CATEGORIES.map(c => `- ${c.id}: ${c.name} (${c.keywords?.join(', ') || ''})`).join('\n')}

请以JSON格式返回：
{
  "categoryId": "类别ID",
  "confidence": 0.95,
  "reasoning": "分类理由"
}
`;

  const response = await openai.chat.completions.create({
    model: 'gpt-3.5-turbo',
    messages: [
      {
        role: 'system',
        content: '你是一个精准的财务分类助手，擅长根据交易描述判断消费类别。只返回JSON格式结果，不要有其他内容。'
      },
      { role: 'user', content: prompt }
    ],
    temperature: 0.3,
    max_tokens: 150
  });

  try {
    const result = JSON.parse(response.choices[0].message.content || '{}');
    return {
      categoryId: result.categoryId,
      categoryName: PREDEFINED_CATEGORIES.find(c => c.id === result.categoryId)?.name || '其他',
      confidence: result.confidence,
      reasoning: result.reasoning
    };
  } catch (error) {
    // 解析失败返回"其他"
    return {
      categoryId: 'other',
      categoryName: '其他',
      confidence: 0,
      reasoning: 'AI解析失败'
    };
  }
}

// 批量分类（更经济）
export async function categorizeBatchWithOpenAI(
  transactions: Transaction[]
): Promise<CategorizationResult[]> {
  const prompt = `
请将以下交易批量分类。返回JSON数组格式。

交易列表：
${transactions.map((t, i) => `
${i + 1}. 商家: ${t.payee}, 金额: ${t.amount}, 备注: ${t.notes || '无'}, 类型: ${t.type}
`).join('')}

可选类别：
${PREDEFINED_CATEGORIES.map(c => `- ${c.id}: ${c.name}`).join('\n')}

返回格式：
[
  { "index": 1, "categoryId": "...", "confidence": 0.95 },
  ...
]
`;

  const response = await openai.chat.completions.create({
    model: 'gpt-3.5-turbo',
    messages: [
      { role: 'system', content: '批量财务分类助手。只返回JSON数组。' },
      { role: 'user', content: prompt }
    ],
    temperature: 0.3,
    max_tokens: 1000
  });

  try {
    const results = JSON.parse(response.choices[0].message.content || '[]');
    return results.map((r: any) => ({
      categoryId: r.categoryId,
      categoryName: PREDEFINED_CATEGORIES.find(c => c.id === r.categoryId)?.name || '其他',
      confidence: r.confidence,
      reasoning: ''
    }));
  } catch (error) {
    // 批量失败，逐个处理
    return Promise.all(transactions.map(t => categorizeWithOpenAI(t)));
  }
}
```

### 2.2 本地AI分类（Ollama）

```typescript
// packages/ai-service/src/categorization/local-ai.ts
import axios from 'axios';
import { Transaction } from '@ai-finance/shared';
import { CategorizationResult } from './cloud-ai';

const OLLAMA_URL = process.env.OLLAMA_URL || 'http://localhost:11434';
const MODEL = process.env.OLLAMA_MODEL || 'llama3.1';

export async function categorizeWithOllama(
  transaction: Transaction
): Promise<CategorizationResult> {
  const prompt = `Classify this transaction into one category:
Merchant: ${transaction.payee}
Amount: ${transaction.amount}
Notes: ${transaction.notes || 'N/A'}

Categories: food, transport, shopping, entertainment, utilities, housing, medical, education, travel, other

Respond with JSON only: {"category": "...", "confidence": 0.9}`;

  try {
    const response = await axios.post(`${OLLAMA_URL}/api/generate`, {
      model: MODEL,
      prompt,
      stream: false,
      format: 'json'
    });

    const result = JSON.parse(response.data.response);
    return {
      categoryId: result.category,
      categoryName: result.category,
      confidence: result.confidence,
      reasoning: 'Local AI classification'
    };
  } catch (error) {
    return {
      categoryId: 'other',
      categoryName: '其他',
      confidence: 0,
      reasoning: 'Local AI failed'
    };
  }
}
```

### 2.3 混合分类策略

```typescript
// packages/ai-service/src/categorization/index.ts
import { Transaction } from '@ai-finance/shared';
import { categorizeWithOpenAI, categorizeBatchWithOpenAI } from './cloud-ai';
import { categorizeWithOllama } from './local-ai';

export interface CategorizeOptions {
  preferLocal?: boolean;      // 优先使用本地AI
  confidenceThreshold?: number; // 置信度阈值
}

export async function categorize(
  transaction: Transaction,
  options: CategorizeOptions = {}
): Promise<string> {
  const { preferLocal = false, confidenceThreshold = 0.7 } = options;

  // 1. 规则匹配（优先级最高）
  const ruleCategory = matchByRule(transaction);
  if (ruleCategory) return ruleCategory;

  // 2. 尝试本地AI
  if (preferLocal) {
    const localResult = await categorizeWithOllama(transaction);
    if (localResult.confidence >= confidenceThreshold) {
      return localResult.categoryId;
    }
  }

  // 3. 云端AI兜底
  const cloudResult = await categorizeWithOpenAI(transaction);
  return cloudResult.categoryId;
}

export async function categorizeBatch(
  transactions: Transaction[],
  options: CategorizeOptions = {}
): Promise<Transaction[]> {
  // 批量分类，提高效率
  const results = await categorizeBatchWithOpenAI(transactions);
  
  return transactions.map((t, i) => ({
    ...t,
    category: results[i]?.categoryId || 'other',
    aiConfidence: results[i]?.confidence
  }));
}

// 规则匹配（基于关键词）
function matchByRule(transaction: Transaction): string | null {
  const text = `${transaction.payee} ${transaction.notes}`.toLowerCase();
  
  const rules = [
    { pattern: /美团|饿了么|外卖|餐厅|快餐/, category: 'food' },
    { pattern: /滴滴|地铁|公交|打车|加油/, category: 'transport' },
    { pattern: /淘宝|京东|天猫|拼多多/, category: 'shopping' },
    { pattern: /电费|水费|燃气|宽带|话费/, category: 'utilities' },
    { pattern: /房租|房贷|物业/, category: 'housing' },
    { pattern: /工资|薪资|奖金/, category: 'salary' },
    { pattern: /红包|转账/, category: 'transfer' }
  ];
  
  for (const rule of rules) {
    if (rule.pattern.test(text)) {
      return rule.category;
    }
  }
  
  return null;
}
```

---

## 3. OCR发票识别（多模态大模型API）

### 3.1 Kimi Coding K2.5（默认方案）

```typescript
// packages/ai-service/src/ocr/kimi-coding-ocr.ts
import OpenAI from 'openai';

// Kimi Coding API - 注意与普通 Moonshot API 不同
const kimiCoding = new OpenAI({
  apiKey: process.env.KIMI_CODING_API_KEY,  // Kimi Coding 专用 API Key
  baseURL: 'https://api.kimi.com/coding/v1'   // Kimi Coding 端点
});

export interface ReceiptInfo {
  merchant: string;       // 商家名称
  amount: number;         // 金额
  date: string;           // 日期 YYYY-MM-DD
  category: string;       // 消费类别
  items?: string[];       // 商品列表
  invoiceNumber?: string; // 发票号码
  taxAmount?: number;     // 税额
}

/**
 * 使用 Kimi Coding K2.5 进行发票识别（默认方案）
 * 端点: https://api.kimi.com/coding/v1
 * 模型: kimi-coding/k2p5 (或 kimi-for-coding)
 * 注意：Kimi Coding API Key 与普通 Moonshot API Key 不互通
 */
export async function recognizeReceiptWithKimiCoding(
  base64Image: string
): Promise<ReceiptInfo> {
  const response = await kimiCoding.chat.completions.create({
    model: 'kimi-coding/k2p5',  // 或 'kimi-for-coding'
    messages: [
      {
        role: 'system',
        content: '你是一个专业的发票识别助手，擅长从各类发票图片中提取结构化信息。请准确识别商家名称、金额、日期等关键信息。'
      },
      {
        role: 'user',
        content: [
          {
            type: 'text',
            text: `请识别这张发票图片，提取以下信息并以JSON格式返回：
{
  "merchant": "商家名称（发票抬头）",
  "amount": 金额数字（不含税的总金额，纯数字）,
  "date": "YYYY-MM-DD",
  "category": "消费类别（餐饮/交通/购物/生活缴费/医疗/教育/娱乐/其他）",
  "items": ["商品或服务明细1", "商品或服务明细2"],
  "invoiceNumber": "发票号码（如有）",
  "taxAmount": 税额数字（如有）
}

注意：
1. 金额只需返回数字，不要包含"¥"或"元"
2. 日期统一转换为 YYYY-MM-DD 格式
3. 如果无法识别某项，使用 null 或 "未知"
4. 只返回 JSON，不要有其他内容`
          },
          {
            type: 'image_url',
            image_url: {
              url: `data:image/jpeg;base64,${base64Image}`
            }
          }
        ]
      }
    ],
    temperature: 0.1,
    max_tokens: 800
  });

  return parseKimiCodingResult(response.choices[0].message.content);
}

function parseKimiCodingResult(content: string | null): ReceiptInfo {
  try {
    // 提取 JSON 部分（Kimi 可能会返回 markdown 代码块）
    const jsonMatch = content?.match(/```json\n?([\s\S]*?)\n?```/) || 
                      content?.match(/\{[\s\S]*\}/);
    const jsonStr = jsonMatch ? jsonMatch[1] || jsonMatch[0] : '{}';
    const result = JSON.parse(jsonStr);
    
    return {
      merchant: result.merchant || result.merchantName || '未知商家',
      amount: parseFloat(result.amount) || 0,
      date: normalizeDate(result.date),
      category: mapToStandardCategory(result.category),
      items: Array.isArray(result.items) ? result.items : [],
      invoiceNumber: result.invoiceNumber || null,
      taxAmount: result.taxAmount ? parseFloat(result.taxAmount) : undefined
    };
  } catch (error) {
    console.error('Kimi Coding OCR 解析失败:', error, '原始内容:', content);
    throw new Error(`发票识别结果解析失败: ${error.message}`);
  }
}

// 日期格式标准化
function normalizeDate(dateStr: string): string {
  if (!dateStr || dateStr === '未知') {
    return new Date().toISOString().split('T')[0];
  }
  
  // 尝试解析各种中文日期格式
  const patterns = [
    /(\d{4})年(\d{1,2})月(\d{1,2})日/,
    /(\d{4})-(\d{1,2})-(\d{1,2})/,
    /(\d{4})\/(\d{1,2})\/(\d{1,2})/
  ];
  
  for (const pattern of patterns) {
    const match = dateStr.match(pattern);
    if (match) {
      const [, year, month, day] = match;
      return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`;
    }
  }
  
  return new Date().toISOString().split('T')[0];
}

// 类别映射标准化
function mapToStandardCategory(category: string): string {
  const categoryMap: Record<string, string> = {
    '餐饮': 'food',
    '美食': 'food',
    '外卖': 'food',
    '交通': 'transport',
    '出行': 'transport',
    '打车': 'transport',
    '购物': 'shopping',
    '超市': 'shopping',
    '生活缴费': 'utilities',
    '水电': 'utilities',
    '医疗': 'medical',
    '医院': 'medical',
    '教育': 'education',
    '娱乐': 'entertainment',
    '其他': 'other'
  };
  
  return categoryMap[category] || 'other';
}
```

### 3.2 OpenAI GPT-4V/GPT-4o 实现（备选）

```typescript
// packages/ai-service/src/ocr/openai-ocr.ts
import OpenAI from 'openai';

const openai = new OpenAI({
  apiKey: process.env.OPENAI_API_KEY
});

export interface ReceiptInfo {
  merchant: string;      // 商家名称
  amount: number;        // 金额
  date: string;          // 日期 YYYY-MM-DD
  category: string;      // 消费类别
  items?: string[];      // 商品列表
  invoiceNumber?: string; // 发票号码
}

/**
 * 使用GPT-4o-mini进行发票识别（成本较低）
 */
export async function recognizeReceiptWithGPT4oMini(
  base64Image: string
): Promise<ReceiptInfo> {
  const response = await openai.chat.completions.create({
    model: 'gpt-4o-mini',
    messages: [
      {
        role: 'system',
        content: '你是一个发票识别助手。从图片中提取关键信息，以JSON格式返回。'
      },
      {
        role: 'user',
        content: [
          {
            type: 'text',
            text: `请识别这张发票，提取以下信息并以JSON格式返回：
{
  "merchant": "商家名称",
  "amount": 金额数字,
  "date": "YYYY-MM-DD",
  "category": "消费类别（餐饮/交通/购物/生活缴费/其他）",
  "items": ["商品1", "商品2"],
  "invoiceNumber": "发票号码（如有）"
}`
          },
          {
            type: 'image_url',
            image_url: {
              url: `data:image/jpeg;base64,${base64Image}`
            }
          }
        ]
      }
    ],
    max_tokens: 500
  });

  return parseOCRResult(response.choices[0].message.content);
}

/**
 * 使用GPT-4o进行发票识别（准确率更高，成本较高）
 */
export async function recognizeReceiptWithGPT4o(
  base64Image: string
): Promise<ReceiptInfo> {
  const response = await openai.chat.completions.create({
    model: 'gpt-4o',
    messages: [
      {
        role: 'system',
        content: '你是一个专业的发票识别助手，擅长从各类发票图片中提取结构化信息。'
      },
      {
        role: 'user',
        content: [
          {
            type: 'text',
            text: `请仔细识别这张发票图片，提取以下信息：
- 商家名称（merchant）
- 消费金额（amount），注意区分金额和税额
- 消费日期（date），格式YYYY-MM-DD
- 消费类别（category）：餐饮、交通、购物、生活缴费、医疗、教育、娱乐、其他
- 商品明细（items），列出主要商品或服务
- 发票号码（invoiceNumber），如有

以JSON格式返回，不要包含其他内容。`
          },
          {
            type: 'image_url',
            image_url: {
              url: `data:image/jpeg;base64,${base64Image}`
            }
          }
        ]
      }
    ],
    max_tokens: 800
  });

  return parseOCRResult(response.choices[0].message.content);
}

function parseOCRResult(content: string | null): ReceiptInfo {
  try {
    const jsonMatch = content?.match(/\{[\s\S]*\}/);
    const result = JSON.parse(jsonMatch ? jsonMatch[0] : '{}');
    
    return {
      merchant: result.merchant || result.merchantName || '未知商家',
      amount: parseFloat(result.amount) || 0,
      date: result.date || new Date().toISOString().split('T')[0],
      category: result.category || 'other',
      items: result.items || [],
      invoiceNumber: result.invoiceNumber
    };
  } catch (error) {
    throw new Error(`发票识别结果解析失败: ${error.message}`);
  }
}
```

### 3.2 Anthropic Claude 3 实现

```typescript
// packages/ai-service/src/ocr/claude-ocr.ts
import Anthropic from '@anthropic-ai/sdk';

const anthropic = new Anthropic({
  apiKey: process.env.ANTHROPIC_API_KEY
});

/**
 * 使用Claude 3 Sonnet进行发票识别
 * 优势：对中文发票支持较好，价格相对GPT-4o更便宜
 */
export async function recognizeReceiptWithClaude(
  base64Image: string
): Promise<ReceiptInfo> {
  const response = await anthropic.messages.create({
    model: 'claude-3-sonnet-20240229',
    max_tokens: 1024,
    messages: [{
      role: 'user',
      content: [
        {
          type: 'text',
          text: `请识别这张发票图片，提取以下信息并以JSON格式返回：
{
  "merchant": "商家名称",
  "amount": 金额数字（仅数字，不含单位）,
  "date": "YYYY-MM-DD",
  "category": "消费类别",
  "items": ["商品1", "商品2"]
}`
        },
        {
          type: 'image',
          source: {
            type: 'base64',
            media_type: 'image/jpeg',
            data: base64Image
          }
        }
      ]
    }]
  });

  const content = response.content[0].type === 'text' 
    ? response.content[0].text 
    : '{}';
  
  return parseOCRResult(content);
}
```

### 3.3 多服务商统一接口

```typescript
// packages/ai-service/src/ocr/index.ts
import { recognizeReceiptWithKimiCoding } from './kimi-coding-ocr';
import { recognizeReceiptWithGPT4oMini, recognizeReceiptWithGPT4o } from './openai-ocr';
import { recognizeReceiptWithClaude } from './claude-ocr';

export type OCRProvider = 'kimi-coding' | 'gpt-4o-mini' | 'gpt-4o' | 'claude-3-sonnet';

export interface OCROptions {
  provider?: OCRProvider;
  fallback?: boolean;      // 失败时是否切换备用服务商
}

/**
 * 统一发票识别接口
 * 默认使用 Kimi Coding K2.5，失败后降级到其他服务商
 */
export async function recognizeReceipt(
  base64Image: string,
  options: OCROptions = {}
): Promise<ReceiptInfo> {
  const { provider = 'kimi-coding', fallback = true } = options;
  
  const providers: OCRProvider[] = ['kimi-coding', 'gpt-4o', 'claude-3-sonnet'];
  const startIndex = providers.indexOf(provider);
  const tryOrder = [...providers.slice(startIndex), ...providers.slice(0, startIndex)];
  
  for (const p of tryOrder) {
    try {
      switch (p) {
        case 'kimi-coding':
          return await recognizeReceiptWithKimiCoding(base64Image);
        case 'gpt-4o-mini':
          return await recognizeReceiptWithGPT4oMini(base64Image);
        case 'gpt-4o':
          return await recognizeReceiptWithGPT4o(base64Image);
        case 'claude-3-sonnet':
          return await recognizeReceiptWithClaude(base64Image);
      }
    } catch (error) {
      console.warn(`OCR ${p} 失败:`, error);
      if (!fallback) throw error;
      // 继续尝试下一个服务商
    }
  }
  
  throw new Error('所有OCR服务商均识别失败');
}

/**
 * 批量发票识别（降低成本）
 */
export async function recognizeReceiptsBatch(
  images: string[],
  options: OCROptions = {}
): Promise<ReceiptInfo[]> {
  // 串行处理以避免触发速率限制
  const results: ReceiptInfo[] = [];
  
  for (const image of images) {
    try {
      const result = await recognizeReceipt(image, options);
      results.push(result);
    } catch (error) {
      results.push({
        merchant: '识别失败',
        amount: 0,
        date: new Date().toISOString().split('T')[0],
        category: 'other',
        error: error.message
      } as any);
    }
    
    // 避免触发API限流
    await delay(500);
  }
  
  return results;
}

function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}
```

### 3.4 成本对比

| 服务商 | 模型 | 输入价格 | 输出价格 | 特点 | 推荐场景 |
|--------|------|---------|---------|------|---------|
| **Kimi Coding** ⭐ | **kimi-coding/k2.5** | **免费** | **免费** | 中文强、编程优化、**免费** | **默认方案** 🇨🇳 |
| Moonshot | kimi-k2.5 | ¥12/M tokens | ¥60/M tokens | 中文强、多模态 | 备选方案 |
| OpenAI | gpt-4o-mini | $0.15/M tokens | $0.60/M tokens | 成本最低 | 备选方案 |
| OpenAI | gpt-4o | $5.00/M tokens | $15.00/M tokens | 准确率最高 | 复杂发票备选 |
| Anthropic | claude-3-sonnet | $3.00/M tokens | $15.00/M tokens | 中文较好 | 备选方案 |

**成本估算**（单张发票约1000 tokens）：
- **Kimi Coding K2.5**: **免费** ⭐ **默认使用**
- Moonshot kimi-k2.5: ~¥0.015/张
- GPT-4o-mini: ~$0.0002/张（约0.0014元）
- Claude 3 Sonnet: ~$0.004/张（约0.03元）
- GPT-4o: ~$0.007/张（约0.05元）

**⚠️ 重要提醒**：
- **Kimi Coding** 和 **Moonshot API** 是两个独立服务
- Kimi Coding 目前是**免费**的，但有使用限制
- 两者 API Key 不互通

**选择建议**：
1. **默认使用 Kimi Coding K2.5** - 免费 + 中文发票理解能力强
2. **备选方案** - 识别失败或限额时降级到 GPT-4o 或 Claude 3
3. **高并发场景** - 准备多个服务商 API Key 做负载均衡

---

## 4. 预算预测

```typescript
// packages/ai-service/src/budget-prediction/index.ts
import OpenAI from 'openai';
import { Transaction } from '@ai-finance/shared';

const openai = new OpenAI();

export interface BudgetPrediction {
  category: string;
  predictedAmount: number;
  confidence: number;
  reasoning: string;
  trend: 'increasing' | 'decreasing' | 'stable';
}

export async function predictBudget(
  category: string,
  history: Transaction[]
): Promise<BudgetPrediction> {
  // 聚合月度数据
  const monthlyData = aggregateMonthly(history);
  
  const prompt = `
作为财务顾问，基于以下"${category}"类别的历史支出数据，预测下月预算：

历史数据（最近12个月）：
${JSON.stringify(monthlyData, null, 2)}

请分析：
1. 支出趋势（增长/下降/稳定）
2. 季节性因素
3. 异常值影响

以JSON格式返回：
{
  "predictedAmount": 预测金额,
  "confidence": 置信度0-1,
  "reasoning": "分析理由",
  "trend": "increasing/decreasing/stable"
}
`;

  const response = await openai.chat.completions.create({
    model: 'gpt-4',
    messages: [{ role: 'user', content: prompt }],
    temperature: 0.3
  });

  try {
    const content = response.choices[0].message.content || '{}';
    const jsonMatch = content.match(/\{[\s\S]*\}/);
    const result = JSON.parse(jsonMatch ? jsonMatch[0] : '{}');
    
    return {
      category,
      predictedAmount: result.predictedAmount,
      confidence: result.confidence,
      reasoning: result.reasoning,
      trend: result.trend
    };
  } catch (error) {
    // 使用简单平均作为兜底
    const avg = monthlyData.reduce((a, b) => a + b.amount, 0) / monthlyData.length;
    return {
      category,
      predictedAmount: Math.round(avg),
      confidence: 0.5,
      reasoning: '使用历史平均值（AI解析失败）',
      trend: 'stable'
    };
  }
}

function aggregateMonthly(transactions: Transaction[]): { month: string; amount: number }[] {
  const monthly: Record<string, number> = {};
  
  for (const t of transactions) {
    const month = t.date.toISOString().slice(0, 7); // YYYY-MM
    monthly[month] = (monthly[month] || 0) + Math.abs(t.amount);
  }
  
  return Object.entries(monthly)
    .map(([month, amount]) => ({ month, amount }))
    .sort((a, b) => a.month.localeCompare(b.month))
    .slice(-12); // 最近12个月
}
```

---

## 5. 共享类型定义

```typescript
// packages/shared/src/types/index.ts

export interface Transaction {
  id: string;
  date: Date;
  payee: string;
  amount: number;  // 正数收入，负数支出
  notes?: string;
  category?: string | null;
  type: 'income' | 'expense';
  source: string;
  rawData?: any;
  aiConfidence?: number;
}

export interface Category {
  id: string;
  name: string;
  keywords?: string[];
  type?: 'income' | 'expense';
  icon?: string;
  color?: string;
}

export interface Budget {
  categoryId: string;
  amount: number;
  period: 'monthly' | 'yearly';
}

export interface Receipt {
  id: string;
  imageUrl: string;
  merchant: string;
  amount: number;
  date: Date;
  items: string[];
  category: string;
}
```

---

## 6. Docker部署配置

```yaml
# docker-compose.yml
version: '3.8'

services:
  actual-server:
    image: actualbudget/actual-server:latest
    ports:
      - '5006:5006'
    volumes:
      - actual-data:/data
    environment:
      - ACTUAL_LOGIN_METHOD=password
    restart: unless-stopped

  ai-service:
    build:
      context: ./packages/ai-service
      dockerfile: Dockerfile
    environment:
      - KIMI_CODING_API_KEY=${KIMI_CODING_API_KEY}        # Kimi Coding（默认OCR）
      - MOONSHOT_API_KEY=${MOONSHOT_API_KEY}              # Moonshot（备选OCR）
      - OPENAI_API_KEY=${OPENAI_API_KEY}                  # OpenAI（备选）
      - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}            # Claude（备选）
      - DEFAULT_OCR_PROVIDER=${DEFAULT_OCR_PROVIDER:-kimi-coding}
      - ENABLE_OCR_FALLBACK=${ENABLE_OCR_FALLBACK:-true}
    restart: unless-stopped

  web-app:
    build:
      context: ./packages/web-app
      dockerfile: Dockerfile
    ports:
      - '3000:80'
    environment:
      - VITE_ACTUAL_URL=http://localhost:5006
    restart: unless-stopped

volumes:
  actual-data:
```

---

## 7. 环境变量配置

```bash
# .env

# Kimi Coding 配置（默认OCR方案）⭐ 免费
# 端点: https://api.kimi.com/coding/v1
# 注意：Kimi Coding API Key 与 Moonshot API Key 不互通
KIMI_CODING_API_KEY=sk-xxxxxxxx

# Moonshot AI 配置（备选OCR方案）
# 端点: https://api.moonshot.cn/v1
MOONSHOT_API_KEY=sk-xxxxxxxx

# OpenAI配置（备选方案）
OPENAI_API_KEY=sk-xxxxxxxx

# Anthropic配置（备选方案）
ANTHROPIC_API_KEY=sk-ant-xxxxxxxx

# Actual Budget配置
ACTUAL_URL=http://localhost:5006
ACTUAL_PASSWORD=your-password

# 应用配置
NODE_ENV=development
LOG_LEVEL=info

# AI配置
DEFAULT_OCR_PROVIDER=kimi-coding   # 默认OCR：kimi-coding | openai | claude
DEFAULT_LLM_PROVIDER=openai        # 默认LLM：openai | moonshot
ENABLE_OCR_FALLBACK=true           # OCR失败时是否降级
```

---

这个技术方案涵盖了所有功能模块的详细实现。需要我进一步展开哪个部分？