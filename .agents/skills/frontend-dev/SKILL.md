---
name: frontend-dev
description: Snail AI Admin 前端开发智能体。当后端 API 变更需要同步调整前端、新增前端页面/组件、修改前端业务逻辑时触发。默认使用 Claude Code 内置能力执行。
license: Apache-2.0
homepage: https://gitee.com/opensnail/snail-ai
metadata:
  version: 1.0.0
  categories:
    - frontend
    - vue
    - typescript
  tags:
    - vue3
    - naive-ui
    - unocss
    - pinia
    - vite
---

# Snail AI Admin 前端开发智能体

用于 snail-ai-admin 前端项目的开发、维护和与后端 API 变更同步。

## 前置条件：定位前端源码

执行任何前端操作前，**必须**先定位前端项目路径。

### 路径获取流程

1. 尝试读取项目根目录 `.env` 文件，解析 `SNAIL_AI_FRONTEND_PATH` 变量
2. 若 `.env` 不存在、变量未设置或路径无效 → 按 `.claude/skills/env-config/SKILL.md` 中的检查流程引导用户配置
3. 验证通过后，将路径记为 `$FRONTEND`，后续所有前端文件操作均基于此路径

## 触发场景

以下任一情况发生时，应使用本智能体：

- 后端新增/修改/删除了 Controller 接口，需要同步更新前端 API 层
- 用户要求新增前端页面、组件或功能模块
- 用户提到"前端也要改"、"同步前端"、"更新前端"等
- 用户要求修复前端 Bug 或调整 UI
- 后端 DTO/VO 结构变更，需要更新前端类型定义
- 新增路由、菜单项或导航配置

## 技术栈速览

| 层级 | 技术 |
|------|------|
| 框架 | Vue 3.5+ Composition API (`<script setup>`) |
| 语言 | TypeScript 5.9+ (strict mode) |
| 构建 | Vite 7.3+ |
| UI 组件库 | Naive UI 2.44+ |
| CSS | UnoCSS (presetWind3) + SCSS |
| 状态管理 | Pinia 3.0+ (Composition API style) |
| 路由 | Vue Router 5.0+ + @elegant-router/vue |
| HTTP | Axios (封装为 `@sa/axios` createFlatRequest) |
| 图表 | ECharts 6.0+ |
| 国际化 | vue-i18n 11.3+ |
| 包管理 | pnpm >= 10.5.0 (monorepo) |
| 代码检查 | Oxlint + ESLint 10 |

## 项目结构

```
$FRONTEND/
├── src/
│   ├── views/              # 页面组件（按功能模块分目录）
│   │   ├── agent/          # 智能体管理
│   │   ├── app/            # 应用管理
│   │   ├── home/           # 首页/仪表盘
│   │   ├── mcp/            # MCP 服务器管理
│   │   ├── model/          # 模型管理
│   │   ├── rag/            # 知识库管理
│   │   ├── resource/       # 资源管理
│   │   ├── skills/         # 技能管理
│   │   ├── store/          # 存储实例（向量+搜索）
│   │   ├── system/user/    # 用户管理
│   │   └── _builtin/       # 内置页面（登录/403/404/500）
│   ├── service/
│   │   ├── api/            # API 函数（按域拆分）
│   │   └── request/        # HTTP 请求封装
│   ├── components/
│   │   ├── common/         # 通用组件
│   │   ├── custom/         # 业务组件
│   │   └── advanced/       # 高级组件
│   ├── hooks/
│   │   ├── business/       # 业务 hooks
│   │   └── common/         # 通用 hooks（table, form, router, echarts）
│   ├── store/modules/      # Pinia stores（app, auth, route, tab, theme, search）
│   ├── router/             # 路由配置（含 elegant 自动生成）
│   ├── typings/            # TypeScript 类型声明
│   ├── constants/          # 常量定义
│   ├── enums/              # 枚举
│   ├── locales/            # 国际化配置
│   ├── styles/             # 全局样式
│   ├── theme/              # 主题配置
│   └── utils/              # 工具函数
├── packages/               # monorepo 内部包
│   ├── axios/              # @sa/axios HTTP 客户端
│   ├── hooks/              # @sa/hooks 共享 composables
│   ├── utils/              # @sa/utils 共享工具
│   ├── materials/          # @sa/materials 布局组件
│   ├── uno-preset/         # @sa/uno-preset UnoCSS 预设
│   └── ...
├── build/                  # Vite 构建配置
└── public/                 # 静态资源
```

## 编码规范

### 1. 文件和目录命名

| 类型 | 规则 | 示例 |
|------|------|------|
| Vue 组件文件 | kebab-case | `operate-drawer.vue`, `auth-image.vue` |
| TypeScript 模块 | kebab-case | `use-table.ts`, `ai-model.ts` |
| 组件 name | PascalCase (defineOptions) | `defineOptions({ name: 'AuthAvatar' })` |
| API 函数 | `fetch` + PascalCase | `fetchAgentList`, `fetchRagDetail` |
| Store | `use` + PascalCase + `Store` | `useAuthStore`, `useRouteStore` |
| Hooks/Composables | `use` + PascalCase | `useRouterPush`, `useTableOperate` |
| 类型命名空间 | PascalCase 点分 | `Api.Agent`, `App.Theme` |

### 2. Vue 组件标准结构

```vue
<script setup lang="ts">
// 1. imports
// 2. defineOptions({ name: 'ComponentName' })
// 3. props / emits
// 4. composables & stores
// 5. reactive state
// 6. computed
// 7. methods
// 8. lifecycle (onMounted, etc.)
</script>

<template>
  <!-- Naive UI 组件使用 n- 前缀 -->
  <!-- UnoCSS 工具类 -->
</template>

<style scoped lang="scss">
/* 组件私有样式 */
/* :deep() 用于穿透 Naive UI 组件 */
</style>
```

### 3. API 函数规范

所有 API 函数放在 `$FRONTEND/src/service/api/{domain}.ts`，使用 `createFlatRequest` 模式：

```ts
import { request } from '../request';

// 分页列表
export function fetchXxxList(params?: { page?: number; size?: number; keyword?: string }) {
  return request<Api.Xxx.XxxList>({
    url: '/xxx/page',
    params
  });
}

// 创建
export function fetchCreateXxx(data: Api.Xxx.CreateRequest) {
  return request<Api.Xxx.XxxItem>({
    url: '/xxx',
    method: 'post',
    data
  });
}

// 更新
export function fetchUpdateXxx(data: Api.Xxx.UpdateRequest) {
  return request<Api.Xxx.XxxItem>({
    url: '/xxx',
    method: 'put',
    data
  });
}

// 删除
export function fetchDeleteXxx(id: number) {
  return request<Api.Xxx.XxxItem>({
    url: `/xxx/${id}`,
    method: 'delete'
  });
}
```

**关键约定：**
- 使用 `{ data, error }` 解构响应（flat request 模式，不抛异常）
- 先检查 `if (!error)` 再使用 `data`
- 流式请求（SSE）使用原生 `fetch()` + `ReadableStream.getReader()`

### 4. 页面标准模式 (Table + Drawer)

列表页面使用 `useTable` + `useTableOperate` 组合：

```vue
<script setup lang="tsx">
import { useTable, useTableOperate } from '@/hooks/common/table';
import { fetchXxxList, fetchDeleteXxx } from '@/service/api/xxx';

const {
  columns,
  data,
  getData,
  loading,
  pagination
} = useTable({
  apiFn: fetchXxxList,
  columns: () => [
    { key: 'name', title: '名称' },
    { key: 'status', title: '状态', render: (row) => <NTag>{row.status}</NTag> },
    {
      key: 'operate',
      title: '操作',
      render: (row) => (
        <div class="flex gap-8px">
          <NButton onClick={() => handleEdit(row)}>编辑</NButton>
          <NButton onClick={() => handleDelete(row.id)}>删除</NButton>
        </div>
      )
    }
  ]
});

const {
  drawerVisible,
  editingData,
  handleAdd,
  handleEdit
} = useTableOperate(data, getData);
</script>
```

### 5. 表单/抽屉模式

创建/编辑使用 `NDrawer` + `NDrawerContent`：

```vue
<template>
  <NDrawer v-model:show="visible" :width="500">
    <NDrawerContent title="标题" closable>
      <NForm ref="formRef" :model="formData" :rules="rules">
        <!-- 表单项 -->
      </NForm>
      <template #footer>
        <NButton @click="visible = false">取消</NButton>
        <NButton type="primary" @click="handleSubmit">确定</NButton>
      </template>
    </NDrawerContent>
  </NDrawer>
</template>
```

### 6. 路由配置

路由通过 `@elegant-router/vue` 从文件路径自动生成。新增页面后运行 `pnpm gen-route` 更新路由。

**路由 meta 字段：**
```ts
{
  title: 'pageName',
  i18nKey: 'route.pageName',  // 国际化 key
  icon: 'mdi:icon-name',      // 侧边栏图标
  order: 10,                  // 排序
  roles: ['1'],               // 权限角色
  keepAlive: true,            // 是否缓存
  hideInMenu: false,          // 菜单中隐藏
}
```

### 7. 类型定义

后端 API 类型定义在 `$FRONTEND/src/typings/api/` 下，使用 `declare namespace` 模式：

```ts
declare namespace Api {
  namespace Xxx {
    interface XxxItem {
      id: number;
      name: string;
      // ...
    }
    type XxxList = Api.Common.PaginatingResponse<XxxItem>;
  }
}
```

## 后端同步工作流

当后端 API 发生变更时，按以下顺序执行：

### 1. 分析后端变更

从当前分支/提交中识别后端改动：
- 新增/修改的 Controller 路由（`@RequestMapping`、`@GetMapping`、`@PostMapping` 等）
- 请求参数结构（DTO/VO 类字段）
- 响应结构（Result<> 泛型类型）
- 新模块或新功能区域

### 2. 同步 API 层

根据后端变更，在 `$FRONTEND/src/service/api/` 中：
- **新增接口**：按对应域文件添加 `fetch*` 函数
- **修改接口**：更新 URL 路径、HTTP 方法、参数类型
- **删除接口**：移除对应函数

### 3. 同步类型定义

在 `$FRONTEND/src/typings/api/` 中：
- 新增类型文件或向现有文件追加接口定义
- 确保字段类型与后端 DTO/VO 匹配
- 更新 barrel export（`index.d.ts`）

### 4. 同步页面组件

- 新增功能 → 在 `$FRONTEND/src/views/` 下创建对应目录和组件
- 字段变更 → 更新表格 columns、表单字段
- 状态枚举变更 → 更新 `render` 函数中的状态映射

### 5. 更新国际化

在 `$FRONTEND/src/locales/langs/zh-cn.ts` 和 `en-us.ts` 中添加/更新对应的翻译 key。

### 6. 更新路由（如需要）

新增页面后在 `$FRONTEND/` 下运行 `pnpm gen-route` 自动生成路由。

### 7. 验证

```bash
cd $FRONTEND && pnpm typecheck     # TypeScript 类型检查
cd $FRONTEND && pnpm lint          # Lint 检查
```

## 样式规范

- **优先 UnoCSS 工具类**：`flex`、`gap-16px`、`text-primary`、`h-full`、`justify-between`
- **Naive UI 变量**：使用 `var(--n-*)` 引用组件内部 CSS 变量
- **暗黑模式**：通过 `toggleCssDarkMode` + `html.dark` class 控制
- **SCSS 作用域**：`<style scoped lang="scss">`，穿透用 `:deep(.n-xxx)`
- **全局 SCSS**：`$FRONTEND/src/styles/scss/global.scss` 自动注入

## 通用组件清单

新增功能时优先复用以下已有组件：

| 组件 | 路径 | 用途 |
|------|------|------|
| SearchForm | `@/components/common/search-form.vue` | 通用搜索表单 |
| OperateDrawer | `@/components/common/operate-drawer.vue` | 通用操作抽屉 |
| DatetimeRange | `@/components/common/datetime-range.vue` | 日期范围选择 |
| TableColumnSetting | `@/components/advanced/table-column-setting.vue` | 表格列设置 |
| ResourcePickerModal | `@/components/custom/resource-picker-modal.vue` | 资源选择器 |
| SvgIcon | `@/components/custom/svg-icon.vue` | SVG 图标 |
| IconTooltip | `@/components/common/icon-tooltip.vue` | 图标提示 |
| ReloadButton | `@/components/common/reload-button.vue` | 刷新按钮 |

## 注意事项

- 所有用户可见文本必须使用 `$t('key')` 国际化
- 表格 columns 定义为 `() => [...]` 以保证响应性
- 使用 JSX `render()` 做复杂表格列渲染时，文件需使用 `<script setup lang="tsx">`
- Pinia store 之间互相引用时，必须在 setup 函数内部调用 `useOtherStore()`，不可在顶层
- 不要手动编辑 `$FRONTEND/src/router/elegant/` 下的文件（自动生成）
- 保持与现有代码风格一致：单引号、无尾逗号、120 字符行宽、箭头函数参数不加括号
