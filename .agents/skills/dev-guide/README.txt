# Snail AI 开发规范 Skill

## 文件结构

dev-guide/
├── SKILL.md                    # 主文档（包含 YAML frontmatter）
├── architecture.md             # 架构设计指南
├── coding-standards.md         # 编码规范详解
├── database-guide.md           # 数据库和持久层指南
├── api-design.md               # API 设计规范
├── naming-conventions.md       # 命名规范速查表
├── common-patterns.md          # 常用设计模式
├── examples/                   # 示例代码
│   ├── controller-example.java
│   ├── service-example.java
│   ├── vo-example.java
│   └── mapper-example.java
└── README.txt                  # 本文件

## 如何上传

### 方式 1: 打包为 zip 上传

1. 打包为 zip:
   cd /Users/zhangshuguang/opensnail/snail-ai/upload/skills/
   zip -r dev-guide.zip dev-guide/

2. 通过 Web 界面上传:
   POST /skill/upload
   file: dev-guide.zip

### 方式 2: 直接使用（文件已在正确位置）

如果文件已经在 upload/skills/dev-guide/ 目录下，可以直接在数据库中创建记录：

INSERT INTO snail_ai_skill (name, description, file_path, has_files, create_dt, update_dt)
VALUES (
  'snail-ai-dev-guide',
  'Snail AI 项目开发规范和最佳实践指南。提供代码风格、命名规范、架构设计、数据库操作等全方位的开发指导。',
  'upload/skills/dev-guide/',
  1,
  NOW(),
  NOW()
);

## 使用示例

### 场景 1: 查询命名规范
用户: "我要创建一个用户查询VO,应该怎么命名?"
AI: [调用 read_skill("snail-ai-dev-guide")]
AI: "根据 Snail AI 开发规范，查询类 VO 应该命名为 UserQueryVO..."

### 场景 2: 查看代码示例
用户: "给我一个标准的 Controller 示例"
AI: [调用 read_skill("snail-ai-dev-guide")]
AI: [读取 examples/controller-example.java]
AI: "这是一个符合 Snail AI 规范的 Controller 示例..."

### 场景 3: 代码审查
用户: "帮我审查这段代码是否符合规范"
AI: [调用 read_skill("snail-ai-dev-guide")]
AI: "发现以下不符合规范的地方: 1. 方法参数超过3个... 2. 使用了魔法值..."

## 文档内容

- **SKILL.md**: 开发规范概览，核心原则，快速开始
- **architecture.md**: 模块架构，分层设计，依赖关系
- **coding-standards.md**: 类/方法/字段命名，注解使用，注释规范
- **database-guide.md**: 表设计，PO类，Mapper使用，查询示例
- **api-design.md**: RESTful API设计，Controller规范，异常处理
- **naming-conventions.md**: 命名规范速查表（快速查询）
- **common-patterns.md**: 设计模式，最佳实践，代码示例
- **examples/**: 完整的代码示例（Controller、Service、VO、Mapper）

## 版本

- 版本: 1.0.0
- 作者: opensnail
- 创建日期: 2026-04-01
- 基于项目: Snail AI

## 相关文档

- 项目完整规范: /docs/CODE_STYLE.md
- 记忆系统指南: /MEMORY_QUICK_START.md
