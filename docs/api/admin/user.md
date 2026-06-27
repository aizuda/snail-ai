# 用户 API

用户模块提供登录认证、用户信息获取、Token 刷新以及用户管理（CRUD、角色、密码）等能力。

---

## 用户登录

```
POST /snail-ai/user/login
```

**请求体：**

| 字段       | 类型     | 必填 | 说明   |
|------------|----------|------|--------|
| `username` | `string` | 是   | 用户名 |
| `password` | `string` | 是   | 密码   |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/user/login' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "admin",
    "password": "your-password"
  }'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlIjoxLCJleHAiOjE3MTcyODYwMDB9.xxxxx",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInVzZXJJZCI6MX0.xxxxx",
    "email": "admin@example.com"
  }
}
```

> 登录成功后，将返回的 `token` 放入后续请求的 `Snail-Ai-Auth` 头中。

---

## 获取当前用户信息

```
GET /snail-ai/user
```

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/user' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "role": 1,
    "roleName": "管理员",
    "tokens": 50000,
    "totals": 100000
  }
}
```

**角色说明：**

| role | 说明     |
|------|----------|
| `1`  | 管理员   |
| `2`  | 普通用户 |

---

## 刷新 Token

```
POST /snail-ai/auth/refreshToken
```

**请求体：**

| 字段           | 类型     | 必填 | 说明                 |
|----------------|----------|------|----------------------|
| `refreshToken` | `string` | 是   | 登录时获取的刷新令牌 |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/auth/refreshToken' \
  -H 'Content-Type: application/json' \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...(新token)",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...(新refreshToken)"
  }
}
```

---

## 用户管理

> 以下接口需要管理员权限。

### 获取用户列表

```
GET /snail-ai/user/page/list
```

**请求参数：**

| 参数    | 类型     | 必填 | 说明           |
|---------|----------|------|----------------|
| `page`  | `number` | 否   | 页码，默认 `1` |
| `size`  | `number` | 否   | 每页条数       |
| `email` | `string` | 否   | 邮箱搜索       |

**curl 示例：**

```bash
curl -X GET 'http://localhost:8900/snail-ai/user/page/list?page=1&size=10' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "data": [
      {
        "id": 1,
        "role": 1,
        "roleName": "管理员",
        "email": "admin@example.com",
        "username": "admin",
        "tokens": 50000,
        "expireDt": "2026-01-01 00:00:00",
        "totals": 100000,
        "createDt": "2025-01-01 00:00:00",
        "updateDt": "2025-06-01 12:00:00"
      },
      {
        "id": 2,
        "role": 2,
        "roleName": "普通用户",
        "email": "user@example.com",
        "username": "user1",
        "tokens": 10000,
        "expireDt": "2025-12-31 00:00:00",
        "totals": 20000,
        "createDt": "2025-03-15 10:00:00",
        "updateDt": "2025-06-01 08:00:00"
      }
    ],
    "page": 1,
    "size": 10,
    "total": 15
  }
}
```

---

### 创建用户

```
POST /snail-ai/user
```

**请求体：**

| 字段       | 类型     | 必填 | 说明                   |
|------------|----------|------|------------------------|
| `username` | `string` | 是   | 用户名                 |
| `password` | `string` | 是   | 密码                   |
| `email`    | `string` | 否   | 邮箱                   |
| `role`     | `number` | 是   | 角色：`1`=管理员 `2`=普通用户 |
| `totals`   | `number` | 否   | Token 总额度           |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/user' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "username": "newuser",
    "password": "secure-password-123",
    "email": "newuser@example.com",
    "role": 2,
    "totals": 50000
  }'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": null
}
```

---

### 更新用户

```
PUT /snail-ai/user/{id}
```

**请求体：**

| 字段       | 类型     | 必填 | 说明   |
|------------|----------|------|--------|
| `role`     | `number` | 是   | 角色   |
| `email`    | `string` | 否   | 邮箱   |
| `password` | `string` | 否   | 新密码 |

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/user/2' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "role": 2,
    "email": "updated@example.com"
  }'
```

---

### 更新用户角色

```
PUT /snail-ai/user/{id}/role
```

**请求参数：**

| 参数   | 类型     | 必填 | 说明                          |
|--------|----------|------|-------------------------------|
| `role` | `number` | 是   | 角色：`1`=管理员 `2`=普通用户 |

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/user/2/role?role=1' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...'
```

---

### 重置用户密码（管理员）

管理员重置指定用户的密码。

```
PUT /snail-ai/user/{id}/password
```

**请求体：**

| 字段       | 类型     | 必填 | 说明   |
|------------|----------|------|--------|
| `password` | `string` | 是   | 新密码 |

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/user/2/password' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{"password": "new-secure-password"}'
```

---

### 修改当前用户密码

当前登录用户修改自己的密码。

```
PUT /snail-ai/user/password
```

**请求体：**

| 字段          | 类型     | 必填 | 说明   |
|---------------|----------|------|--------|
| `oldPassword` | `string` | 是   | 旧密码 |
| `newPassword` | `string` | 是   | 新密码 |

**curl 示例：**

```bash
curl -X PUT 'http://localhost:8900/snail-ai/user/password' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "oldPassword": "current-password",
    "newPassword": "new-secure-password"
  }'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": null
}
```

---

### 授权用户 Token 额度

为用户授权 Token 使用额度和有效期。

```
POST /snail-ai/user/authorize/code
```

**请求体：**

| 字段     | 类型     | 必填 | 说明                     |
|----------|----------|------|--------------------------|
| `email`  | `string` | 是   | 用户邮箱                 |
| `days`   | `number` | 是   | 有效天数                 |
| `totals` | `number` | 是   | 授权的 Token 总额度      |

**curl 示例：**

```bash
curl -X POST 'http://localhost:8900/snail-ai/user/authorize/code' \
  -H 'Content-Type: application/json' \
  -H 'Snail-Ai-Auth: eyJhbGciOiJIUzI1NiJ9...' \
  -d '{
    "email": "user@example.com",
    "days": 30,
    "totals": 100000
  }'
```

**响应示例：**

```json
{
  "code": 1,
  "msg": "success",
  "data": null
}
```
