# 追踪详情

::: warning 实现中
该功能正在开发中，当前版本尚未完整实现。文档描述的是目标设计，实际功能将在后续版本中发布。
:::

追踪详情是 Snail AI 可观测性模块的核心功能页面，提供了从宏观到微观的全方位对话追踪能力。通过 Trace 列表、观测树、瀑布图等可视化手段，开发者可以完整回溯每一次 AI 对话的执行过程，精确定位性能瓶颈和异常问题。

## 访问入口

在智能体详情页中，切换到**可观测性**标签页即可进入追踪详情功能。该页面分为两个主要区域：

1. **Trace 列表**：展示该智能体下所有对话轮次的追踪记录
2. **Trace 详情**：选中某条 Trace 后，展示其完整的观测树和瀑布图

## Trace 列表

<!-- screenshot: obs-trace-list.png — Trace 列表页，展示多条追踪记录，包含时间、模型、耗时、状态、Token 用量等列 -->

Trace 列表以表格形式展示智能体的所有追踪记录，每行代表一次完整的用户交互轮次。

### 列表字段

| 字段 | 说明 |
|------|------|
| **ID** | Trace 唯一标识，点击可进入详情 |
| **会话 ID** | 所属对话会话的 ID，同一会话下可能包含多条 Trace |
| **用户** | 发起对话的用户 ID |
| **模型** | 本次对话使用的大模型名称 |
| **输入** | 用户发送的消息内容预览 |
| **输出** | AI 返回的响应内容预览 |
| **开始时间** | Trace 的起始时间戳 |
| **耗时** | 从接收用户消息到返回完整响应的总耗时（毫秒） |
| **状态** | 执行状态：成功（1）/ 失败（2） |
| **Token** | 输入 Token 数 / 输出 Token 数 |
| **费用** | 本次调用的预估费用 |
| **观测数** | 包含的 Observation 总数 |
| **工具调用** | 工具调用次数 |
| **错误/警告** | ERROR 和 WARNING 级别 Observation 的数量 |
| **收藏** | 是否已收藏（书签标记） |

### 列表筛选

Trace 列表支持多维度筛选，帮助快速定位目标记录：

- **关键词搜索**：按 Trace ID、会话 ID 或输入/输出内容进行模糊搜索
- **状态筛选**：筛选全部 / 成功 / 失败的 Trace
- **收藏筛选**：仅显示已收藏 / 未收藏 / 全部的 Trace
- **时间范围**：按日期范围过滤 Trace
- **会话 ID**：按特定会话 ID 精确筛选

```typescript
// 筛选参数类型
type TraceQueryFilters = {
  keyword: string;                          // 关键词搜索
  status: 'ALL' | 1 | 2;                  // 状态：全部/成功/失败
  bookmarked: 'ALL' | 'BOOKMARKED' | 'UNBOOKMARKED';  // 收藏状态
  dateRange: [number, number] | null;      // 时间范围
};
```

### 收藏 Trace

对于需要反复查看或后续分析的 Trace，可以点击收藏按钮将其标记为书签（Bookmark）。收藏后的 Trace 可以通过筛选器快速定位，方便后续的对比分析和问题排查。

收藏操作会调用 `PUT /agent/trace/{traceId}/bookmark` 接口切换收藏状态。

## Trace 详情

<!-- screenshot: obs-trace-detail.png — Trace 详情页，左侧为观测树，右侧为选中 Observation 的详细信息面板 -->

点击列表中的某条 Trace 后，进入详情视图。详情页提供三种视图模式：

| 视图模式 | 说明 |
|----------|------|
| **树形视图（Tree）** | 以缩进树形结构展示 Observation 的父子关系 |
| **时间线视图（Timeline）** | 以瀑布图形式展示各 Observation 的时间分布和耗时 |
| **双栏视图（Dual）** | 左侧树形 + 右侧时间线，同步联动 |

### 观测树 (Observation Tree)

观测树是追踪详情最核心的可视化组件，它以树形结构展示一次对话中所有 Observation 的层级关系和执行顺序。

```
Trace
├── AGENT: agent-execution
│   ├── GENERATION: llm-call-1
│   │   └── THINKING: reasoning-process
│   ├── TOOL: web-search
│   │   └── EVENT: search-completed
│   ├── RETRIEVER: rag-retrieval
│   │   └── EMBEDDING: query-embedding
│   └── GENERATION: llm-call-2
└── SPAN: post-processing
```

#### 树节点信息

每个树节点展示以下关键信息：

- **类型图标**：使用不同颜色和图标区分 GENERATION、TOOL、THINKING 等类型
- **名称**：Observation 的名称标识
- **耗时**：该步骤的执行耗时
- **级别标记**：WARNING 和 ERROR 级别以醒目颜色标注
- **展开/收起**：可以展开或收起子节点

#### 节点数据结构

```typescript
interface TreeNode extends Observation {
  children: TreeNode[];    // 子节点列表
  depth: number;           // 树深度
  hasChildren: boolean;    // 是否有子节点
  isExpanded: boolean;     // 是否展开
  index: number;           // 节点索引
  parentIndex: number | null; // 父节点索引
}
```

#### 过滤器

观测树支持以下过滤条件，用于在复杂的追踪数据中快速定位目标：

| 过滤维度 | 说明 |
|----------|------|
| **类型过滤** | 按 Observation 类型（GENERATION、TOOL、THINKING 等）过滤 |
| **状态过滤** | 按执行状态过滤 |
| **耗时范围** | 设置最小/最大耗时阈值 |
| **关键词搜索** | 在 Observation 名称和内容中搜索 |

### 瀑布图 (Waterfall)

<!-- screenshot: obs-trace-waterfall.png — 瀑布图视图，水平条形图展示各 Observation 的时间偏移和耗时长度 -->

瀑布图以水平时间轴的形式，直观展示每个 Observation 的开始时间偏移和持续时长。这种可视化方式特别适合分析以下场景：

- **性能瓶颈识别**：最长的横条即为耗时最多的步骤
- **并发执行分析**：同一时刻有多个横条并行，说明存在并发执行
- **时序依赖分析**：观察各步骤的先后顺序和等待间隙

#### 瀑布图数据结构

```typescript
type TimelineItem = {
  id: string;                           // Observation ID
  type: ObservationType;                // 观测类型
  name: string;                         // 名称
  depth: number;                        // 层级深度（决定缩进）
  offset: number;                       // 相对于 Trace 开始的时间偏移（毫秒）
  duration: number;                     // 持续时长（毫秒）
  observation: Observation;             // 完整 Observation 数据
};
```

#### 瀑布图交互

- **悬停高亮**：鼠标悬停在横条上时，显示该 Observation 的关键摘要
- **点击选中**：点击横条可选中该 Observation，在右侧面板显示详细信息
- **缩放**：支持时间轴缩放，适应不同时间跨度的 Trace
- **同步联动**：在双栏视图模式下，瀑布图与观测树的选中状态保持同步

## Observation 详情面板

选中某个 Observation 后，右侧面板展示该观测步骤的完整详细信息。

### 通用信息

所有类型的 Observation 都包含以下通用字段：

| 字段 | 说明 |
|------|------|
| **ID** | Observation 唯一标识 |
| **类型** | GENERATION / TOOL / THINKING / SPAN / EVENT / AGENT / RETRIEVER / EMBEDDING |
| **名称** | 步骤名称 |
| **开始时间** | 执行开始的时间戳 |
| **结束时间** | 执行结束的时间戳 |
| **耗时** | 持续时长（毫秒） |
| **级别** | DEBUG / DEFAULT / WARNING / ERROR |
| **状态** | 执行状态及状态消息 |

### 输入/输出

每个 Observation 都记录了该步骤的输入和输出数据：

- **Input（输入）**：发送给该步骤的数据，如发送给 LLM 的 prompt、工具调用的参数等
- **Output（输出）**：该步骤返回的数据，如 LLM 生成的文本、工具执行的结果等

输入和输出数据以格式化的 JSON 或文本形式展示，支持折叠/展开长文本。

### GENERATION 专有字段

当观测类型为 GENERATION（大模型调用）时，额外展示以下信息：

| 字段 | 说明 |
|------|------|
| **模型** | 使用的模型名称（如 gpt-4o、claude-3.5-sonnet） |
| **模型参数** | temperature、maxTokens 等调用参数 |
| **Token 用量** | 输入 Token 数、输出 Token 数、总 Token 数的详细明细 |
| **费用明细** | 按输入/输出分别计算的费用 |
| **总费用** | 本次调用的总费用 |
| **首 Token 时间** | completionStartTime 与 startTime 的差值，反映模型响应速度 |
| **结束原因** | finishReason（如 stop、length、tool_calls） |
| **思维链内容** | 推理模型的内部思维过程（thinkingContent） |
| **工具定义** | 本次调用中可用的工具定义列表 |
| **工具调用** | 模型决定调用的工具列表，包含工具名称和参数 |
| **Prompt** | 关联的 Prompt 模板信息（ID、名称、版本） |

### TOOL 专有字段

当观测类型为 TOOL（工具调用）时，额外展示：

| 字段 | 说明 |
|------|------|
| **工具调用 ID** | toolCallId，关联回发起调用的 GENERATION |
| **工具名称** | 通过 name 字段标识 |
| **输入参数** | 工具的输入参数（JSON 格式） |
| **执行结果** | 工具的返回值 |
| **执行耗时** | 工具执行的持续时间 |

### 思维链展示

对于使用推理模型（如 OpenAI o1/o3 系列、Claude 思维模式等）的对话，THINKING 类型的 Observation 或 GENERATION 的 `thinkingContent` 字段会展示模型的完整思维链内容。

思维链通常以 Markdown 格式渲染，包含模型的：
- 问题分解过程
- 逐步推理逻辑
- 方案评估与选择
- 最终结论总结

## Token 用量分析

在 Trace 详情页的顶部，汇总展示本次对话的整体 Token 用量统计：

| 统计项 | 说明 |
|--------|------|
| **总 Trace 数** | 包含的 Trace 总数 |
| **总消息数** | 对话消息总数 |
| **总 Generation 数** | LLM 调用总次数 |
| **工具调用次数** | 工具调用总次数 |
| **总耗时** | 所有 Trace 的累计耗时 |
| **输入 Token** | 所有 GENERATION 的输入 Token 之和 |
| **输出 Token** | 所有 GENERATION 的输出 Token 之和 |
| **总费用** | 所有调用的累计费用 |
| **平均评分** | 各评分维度的平均值 |

## API 接口

追踪相关的 API 接口列表：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/agent/{agentId}/traces` | 获取智能体的 Trace 分页列表 |
| GET | `/agent/{agentId}/trace/{traceId}` | 获取单条 Trace 的完整详情（含 Observations 和 Scores） |
| GET | `/agent/{agentId}/conversation/{conversationId}/trace` | 获取某次会话的完整追踪数据 |
| PUT | `/agent/trace/{traceId}/bookmark` | 切换 Trace 的收藏状态 |

### Trace 列表请求参数

```typescript
{
  page?: number;           // 页码
  size?: number;           // 每页大小
  start?: string;          // 起始时间
  end?: string;            // 结束时间
  keyword?: string;        // 关键词搜索
  bookmarked?: boolean;    // 仅收藏
  status?: number | string;// 状态筛选
  conversationId?: string; // 会话 ID
}
```

### Trace 详情响应结构

```typescript
type Trace = {
  id: string;
  input?: any;
  output?: any;
  model?: string;
  startTime: number;
  endTime?: number;
  durationMs?: number;
  status: number | string;
  statusMessage?: string;
  environment?: string;
  release?: string;
  bookmarked?: boolean;
  tags?: string[];
  observations: Observation[];  // 观测列表（构建树形结构）
  scores: Score[];              // 关联的评分列表
};
```

## 最佳实践

### 问题排查流程

1. **定位异常 Trace**：在列表中按状态筛选失败的 Trace，或按耗时排序找出慢查询
2. **分析观测树**：展开观测树，查看哪个步骤出现了 ERROR 或 WARNING
3. **检查瀑布图**：通过瀑布图识别耗时最长的步骤
4. **查看详情**：点击具体的 Observation，检查输入输出数据和错误信息
5. **收藏标记**：将需要后续跟进的 Trace 收藏，便于团队协作排查

### 性能优化建议

- 关注 GENERATION 类型 Observation 的**首 Token 时间**，过长可能表示模型负载较高
- 检查 TOOL 类型的执行耗时，外部工具调用的延迟往往是总耗时的主要组成部分
- 比较多条相似 Trace 的 Token 用量，优化 prompt 以减少不必要的 Token 消耗
- 利用 RETRIEVER 类型的耗时数据，评估 RAG 检索策略的效率

## 下一步

- [统计分析](./analytics.md) -- 查看 Agent 维度的宏观数据统计
- [评分系统](./score.md) -- 对 Trace 和 Observation 进行评分
- [可观测性概览](./index.md) -- 返回可观测性总览
