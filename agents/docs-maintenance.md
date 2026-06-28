---
name: docs-maintenance
description: Maintain Snail AI documentation against current source, scripts, and VitePress configuration.
license: Apache-2.0
homepage: https://gitee.com/opensnail/snail-ai
metadata:
  version: 0.0.6
  categories:
    - documentation
    - vitepress
    - release
  tags:
    - docs
    - source-validation
    - packaging
---

# Docs Maintenance Agent

Use this agent when Snail AI documentation must be checked against current source code, scripts, configuration, or release changes.

## Triggers

Run this maintenance flow when any of these change:

- Controller paths, request/response DTOs, interceptors, or authentication headers.
- `pom.xml` versions, Maven modules, Java version, or dependency baselines.
- `snail-ai-starter/src/main/resources/application.yml` configuration keys or defaults.
- Files under `docs/docker/` or `docs/sql/`.
- VitePress config under `docs/.vitepress/`.
- Docs package metadata or packaging script under `docs/package.json` or `docs/package-docs.sh`.
- Model provider implementations under `snail-ai-models/`.
- Agent Client, OpenAPI SDK, MCP, RAG, Skill, memory, or resource storage implementations.
- Release notes, changelog, packaging, or deployment scripts.

## Information source priority

When documentation conflicts with implementation, trust sources in this order:

1. Current source code.
2. Documentation runtime assets under `docs/docker/`, `docs/sql/`, and `docs/package-docs.sh`.
3. Root `pom.xml` and module `pom.xml` files.
4. `snail-ai-starter/src/main/resources/application.yml`.
5. VitePress config and docs package metadata.
6. Existing documentation.

Do not preserve old documentation claims when they conflict with higher-priority sources.

## Maintenance rules

- Do not describe planned or partial features as supported production capabilities.
- Keep executable deployment steps limited to currently supported databases and scripts.
- Use `docs/docker/docker-compose.yaml` for dependency Compose examples.
- Use `docs/sql/snail_ai_schema.sql` for MySQL initialization.
- Use `docs/sql/snail_ai_schema_pgsql.sql` for PostgreSQL initialization.
- Use `docs/sql/snail_ai_schema_dameng.sql` for Dameng initialization only after confirming matching datasource and driver support from source configuration.
- OpenAPI external integrations must use `Snail-Ai-App-Id` and `Snail-Ai-Token`.
- `Snail-Ai-Auth` is for Admin API or client Chat session contexts, not generic OpenAPI integration.
- Do not document third-party Admin authentication frameworks unless matching source dependencies and wiring exist. Current Admin authentication is source-backed by `AuthenticationInterceptor`, `@LoginRequired`, `RoleEnum`, and `Snail-Ai-Auth`.
- Model docs must distinguish source-supported providers from OpenAI-compatible endpoint experiments.
- Current source-supported model adapters are OpenAI-compatible Chat, OpenAI-compatible Embedding, and Qwen/HTTP Rerank unless source proves otherwise.
- Current business database implementations are MySQL, PostgreSQL, and Dameng when the matching storage module, initialization script, driver, and datasource configuration are present.
- SQL Server and MariaDB belong in roadmap/planning sections unless current scripts and code prove direct support.
- Java version, Spring Boot version, Spring AI version, modules, ports, and context path must be verified from source before editing docs.
- VitePress navigation and sidebar links must match actual files.
- Prefer concise pages with `概览`, `当前支持状态`, `快速示例`, and `相关源码` sections for AI readability.

## Open-source edition boundary

This repository is the source of truth for the open-source edition. Do not document commercial, internal, mock, screenshot-only, or planned capabilities as open-source supported features.

A feature is open-source supported only when all relevant evidence exists in this repository:

- API docs: matching Controller route, request/response DTOs, service implementation, and authentication behavior.
- UI docs: matching route/component in the open-source frontend or embedded UI artifact, not only a screenshot placeholder.
- Persistent features: matching PO/Mapper/storage implementation and SQL schema or migration.
- Deployment docs: matching script, runtime configuration, dependency, and startup module wiring.
- SDK/client docs: matching published module or source module under `snail-ai-agent/`, including public APIs and auto-configuration.

Current source-backed open-source areas include:

- Admin management for users, agents, apps/client nodes, models, RAG, MCP servers, skills, resources, store instances, and simplified agent analytics.
- OpenAPI external integration for users, agents, conversations, chat, and client Chat token endpoints.
- Java Agent Client, OpenAPI Java SDK, gRPC Server-Agent execution, route strategies, MCP/Skill tools, RAG tools, client Chat mode, and Tavily-based Web Search when configured.
- Client Chat mode is source-backed by the independent `snail-ai-chat` frontend and `snail-ai-agent/snail-ai-agent-chat` backend modules; built frontend assets are embedded under `snail-ai-agent-chat-starter/src/main/resources/META-INF/chat`.
- Resource storage through local and MinIO implementations.
- Short-term memory runtime/storage; Admin Memory API docs require a matching `MemoryController` and service before being described as supported.
- Business database support for MySQL, PostgreSQL, and Dameng; vector/search storage support according to the storage modules and Compose file.

Current open-source partial or unsupported areas must not be written as completed features:

- Full Langfuse-style observability, Trace list/detail, Observation tree/waterfall UI, Trace bookmark APIs, and Trace/Observation/Score persistence are not source-backed unless matching Controller, service, PO/Mapper, and SQL objects exist. Document only the simplified `/agent/{id}/analytics` endpoint and client-side logging/token/thinking collection that are present in source.
- Independent Admin Memory APIs under `/memory/**` are not source-backed unless `MemoryController` and related Admin services exist; document memory runtime behavior instead of CRUD/config/debug endpoints.
- Global Dashboard/system health pages are not source-backed unless matching Dashboard Controller/service or frontend route exists.
- Independent OpenAPI RAG search or RAG QA endpoints are not present; document RAG usage through bound-agent chat unless an OpenAPI Controller proves otherwise.
- Non-Java Agent Client SDKs, plugin marketplace, enterprise-only features, and cloud/SaaS-only workflows belong only in roadmap or upcoming pages.
- Placeholder pages may remain only when clearly marked as `实现中`, `规划中`, or `当前未提供`, and they must not include executable API paths or deployment steps that do not exist.

## Standard validation workflow

1. Read source facts:
   - `pom.xml`
   - `snail-ai-starter/src/main/resources/application.yml`
   - relevant controllers, interceptors, constants, DTOs, and module code
   - `docs/docker/docker-compose.yaml`
   - `docs/sql/`
   - `docs/package.json`
   - `docs/package-docs.sh`
   - `docs/.vitepress/config.mts`, `docs/.vitepress/config/nav.ts`, and `docs/.vitepress/config/sidebar.ts`
2. Search docs for stale references:
   - `script/sql`
   - `script/docker`
   - `script/package-docs.sh`
   - `deploy/docker`
   - `localhost:8080`
   - `Java 17`
   - `/open-api`
   - `Snail-Ai-Auth` in OpenAPI contexts
   - provider names listed as built-in without source proof
   - unsupported open-source feature claims:
     - `Langfuse`
     - `/agent/{id}/traces`, `/agent/{id}/trace`, `/agent/trace`, `/agent/score`
     - `TraceController`, `ScoreController`, `t_trace_`, `trace_observation`, `trace_score`
     - `Trace 列表`, `Trace 详情`, `Observation 树`, `瀑布图`, `评分系统`
     - `MemoryController`, `/snail-ai/memory`, `GET /snail-ai/memory`, `POST /snail-ai/memory`, `PUT /snail-ai/memory`, `DELETE /snail-ai/memory`
     - `DashboardController`, `系统健康雷达图`, `整体健康分`, `fetchSystemHealth`
     - independent `/openapi/v1/rag` endpoints
     - Python/Go/Node.js Agent Client SDKs outside roadmap/upcoming pages
3. Update docs with current paths and source-supported behavior.
4. Update `docs/.vitepress/config/nav.ts` and `docs/.vitepress/config/sidebar.ts` when pages move or are deleted.
5. Build docs.
6. Package docs site if release artifacts are needed.

## Required checks before completion

Run or request equivalent checks:

```bash
pnpm --dir docs install --frozen-lockfile
pnpm --dir docs docs:build
bash docs/package-docs.sh
```

If dependencies are already installed and network access is unavailable, `bash docs/package-docs.sh --skip-install` is an acceptable equivalent for packaging; report that substitution explicitly.

Then check residual references:

```bash
rg "script/sql|script/docker|script/package-docs|localhost:8080|Java 17|/open-api|snail-job" docs --glob '!docs/node_modules/**' --glob '!docs/dist/**' --glob '!docs/.vitepress/cache/**'
rg "Snail-Ai-Auth" docs/api/openapi docs/guide/quick-start.md docs/faq/index.md
rg "Langfuse|/agent/\\{id\\}/traces|/agent/\\{id\\}/trace|/agent/trace|/agent/score|TraceController|ScoreController|t_trace_|trace_observation|trace_score|Trace 列表|Trace 详情|Observation 树|瀑布图|评分系统|MemoryController|/snail-ai/memory|GET /snail-ai/memory|POST /snail-ai/memory|PUT /snail-ai/memory|DELETE /snail-ai/memory|DashboardController|系统健康雷达图|整体健康分|fetchSystemHealth|/openapi/v1/rag|Python Agent Client SDK|Go Agent Client SDK|Node\\.js Agent Client SDK" docs --glob '!docs/node_modules/**' --glob '!docs/dist/**' --glob '!docs/.vitepress/cache/**'
```

Expected results:

- `docs/dist/index.html` exists.
- A docs archive exists under `dist/packages/`.
- Old `script/sql`, `script/docker`, and `script/package-docs.sh` paths are gone, except intentionally retained historical changelog references.
- OpenAPI docs use `Snail-Ai-App-Id` and `Snail-Ai-Token`.
- Any remaining `Snail-Ai-Auth` references are explicitly Admin API or client Chat contexts.
- Unsupported open-source feature references either do not exist in current-support docs or are explicitly marked as roadmap/upcoming/implementation-in-progress without executable API or deployment claims.

## Output style

When reporting results:

- List changed documentation areas, not every edited line.
- Mention any validation command that failed and why.
- Call out intentionally retained references, such as changelog history or source default values that differ from current recommended config.
- Do not claim UI or docs build success unless the command actually passed.
