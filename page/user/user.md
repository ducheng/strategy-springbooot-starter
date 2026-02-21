# 使用指南

## 1. Maven 依赖

### 1.1 添加依赖

在项目的 `pom.xml` 中添加以下依赖：

```xml
<dependency>
    <groupId>com.strategy</groupId>
    <artifactId>dynamic-route-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 1.2 安装到本地仓库

首先需要将 starter 安装到本地 Maven 仓库：

```bash
cd strategy-springbooot-starter
mvn clean install
```

## 2. 核心注解说明

### @ServiceRoute

用于 Controller 方法上，根据条件动态路由到不同的 Service 实现。

#### 注解属性

| 属性 | 类型 | 必填 | 默认值 | 说明 |
|------|------|--------|----------|------|
| `condition` | String | 是 | - | SpEL 表达式，计算结果为 true 时路由到 `targetBean` |
| `targetBean` | String | 是 | - | 目标 Bean 名称，条件满足时使用 |
| `defaultBean` | String | 否 | "" | 默认 Bean 名称，条件不满足时使用（可选） |
| `cacheExpression` | boolean | 否 | true | 是否缓存 SpEL 表达式编译结果 |

#### SpEL 可用变量

| 变量 | 类型 | 说明 |
|--------|------|------|
| `#request` | HttpServletRequest | 当前 HTTP 请求对象 |
| `#args` | Object[] | 方法参数数组 |
| `#方法参数名` | Object | 通过参数名直接访问，如 `#id`, `#user` |
| `#target` | Object | 被代理的目标对象 |
| `#method` | Method | 被调用的目标方法 |

#### 正则表达式函数

| 函数 | 说明 | 示例 |
|------|------|------|
| `#regex(value, pattern)` | 通用正则匹配 | `#regex(#id.toString(), '\\d{3,}')` |
| `#matchHeader(#request, name, pattern)` | 匹配请求头 | `#matchHeader(#request, 'version', 'v\\d+')` |
| `#matchParam(#request, name, pattern)` | 匹配请求参数 | `#matchParam(#request, 'type', 'premium\|vip')` |
| `#headerMatches.match(name, pattern)` | 简化版匹配请求头 | `#headerMatches.match('version', 'v\\d+')` |
| `#paramMatches.match(name, pattern)` | 简化版匹配请求参数 | `#paramMatches.match('type', 'premium\|vip')` |

## 3. 使用示例

### 3.1 根据 HTTP Header 路由

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(@Qualifier("oldUserService") UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    @ServiceRoute(
        condition = "#request.getHeader('version') == 'v2'",
        targetBean = "newUserService",
        defaultBean = "oldUserService"
    )
    public User getUserById(@PathVariable Long id) {
        // 当 Header version=v2 时，实际调用 newUserService.findById(id)
        // 否则调用 oldUserService.findById(id)
        return userService.findById(id);
    }
}
```

**测试**：
```bash
# 调用老服务
curl http://localhost:8080/api/users/1

# 调用新服务
curl -H "version: v2" http://localhost:8080/api/users/1
```

### 3.2 根据请求参数路由

```java
@GetMapping("/{id}/info")
@ServiceRoute(
    condition = "#useNew == true",
    targetBean = "newUserService",
    defaultBean = "oldUserService"
)
public User getUserInfo(@PathVariable Long id, @RequestParam(required = false) Boolean useNew) {
    return userService.findById(id);
}
```

**测试**：
```bash
# 老服务
curl "http://localhost:8080/api/users/1/info"

# 新服务
curl "http://localhost:8080/api/users/1/info?useNew=true"
```

### 3.3 根据业务 ID 范围路由

```java
@GetMapping("/route/{id}")
@ServiceRoute(
    condition = "#id > 100",
    targetBean = "newUserService",
    defaultBean = "oldUserService"
)
public User getUserWithIdRoute(@PathVariable Long id) {
    return userService.findById(id);
}
```

**测试**：
```bash
# ID <= 100，走老服务
curl http://localhost:8080/api/users/route/1

# ID > 100，走新服务
curl http://localhost:8080/api/users/route/101
```

### 3.4 多条件组合路由

```java
@GetMapping("/check/{id}")
@ServiceRoute(
    condition = "#request.getHeader('tenantId') == 'vip' and #request.getHeader('version') == 'v2' or #forceNew == true",
    targetBean = "newUserService",
    defaultBean = "oldUserService"
)
public User checkUser(@PathVariable Long id, @RequestParam(required = false) Boolean forceNew) {
    return userService.findById(id);
}
```

**测试**：
```bash
# 方式1：VIP + v2
curl -H "tenantId: vip" -H "version: v2" http://localhost:8080/api/users/check/1

# 方式2：强制参数
curl "http://localhost:8080/api/users/check/1?forceNew=true"
```

### 3.5 POST 请求路由

```java
@PostMapping
@ServiceRoute(
    condition = "#request.getHeader('source') == 'third-party'",
    targetBean = "newUserService",
    defaultBean = "oldUserService"
)
public User createUser(@RequestBody User user) {
    return userService.create(user);
}
```

**测试**：
```bash
# 老服务
curl -X POST -H "Content-Type: application/json" \
  -d '{"username":"test1","email":"test1@example.com"}' \
  http://localhost:8080/api/users

# 新服务
curl -X POST -H "Content-Type: application/json" -H "source: third-party" \
  -d '{"username":"test2","email":"test2@example.com"}' \
  http://localhost:8080/api/users
```

### 3.6 根据请求体字段路由

```java
@PostMapping("/register")
@ServiceRoute(
    condition = "#user.username != null and #user.username.endsWith('-v2')",
    targetBean = "newUserService",
    defaultBean = "oldUserService"
)
public User register(@RequestBody User user) {
    return userService.create(user);
}
```

**测试**：
```bash
# 用户名以 -v2 结尾，走新服务
curl -X POST -H "Content-Type: application/json" \
  -d '{"username":"admin-v2","email":"admin@example.com"}' \
  http://localhost:8080/api/users/register
```

### 3.7 无参数方法

```java
@GetMapping("/description")
@ServiceRoute(
    condition = "#request.getHeader('new') == 'true'",
    targetBean = "newUserService",
    defaultBean = "oldUserService"
)
public String getDescription() {
    return userService.getDescription();
}
```

**测试**：
```bash
# 老服务
curl http://localhost:8080/api/users/description

# 新服务
curl -H "new: true" http://localhost:8080/api/users/description
```

### 3.8 不指定 defaultBean（可选）

```java
@GetMapping("/optional")
@ServiceRoute(
    condition = "#useNew == true",
    targetBean = "newUserService"
    // 不指定 defaultBean，条件不满足时执行原方法
)
public User optionalRoute(@PathVariable Long id, @RequestParam(required = false) Boolean useNew) {
    // useNew=false 时，执行这里的逻辑（原方法）
    // useNew=true 时，路由到 newUserService
    return userService.findById(id);
}
```

## 4. 应用场景

### 4.1 灰度发布

逐步将流量从旧系统切换到新系统：

```java
@ServiceRoute(
    condition = "T(Math.random()) < 0.1",  // 10% 流量
    targetBean = "newSystemService"
)
```

### 4.2 多租户系统

不同租户使用不同的实现：

```java
@ServiceRoute(
    condition = "#request.getHeader('X-Tenant-Id') == 'tenant-a'",
    targetBean = "serviceImplementationA",
    defaultBean = "serviceImplementationB"
)
```

### 4.3 A/B 测试

根据用户特征进行测试：

```java
@ServiceRoute(
    condition = "#user.id % 2 == 0",  // 偶数 ID
    targetBean = "experimentalService"
)
```

### 4.4 功能开关

动态控制新功能的启用：

```java
@ServiceRoute(
    condition = "@featureToggleService.isEnabled('new-feature')",
    targetBean = "newFeatureService"
)
```

## 5. 配置说明

### 5.1 日志配置

调整日志级别查看详细的路由信息：

```yaml
logging:
  level:
    com.strategy.starter: DEBUG
```

### 5.2 自动配置

Starter 会自动配置以下组件：

- `ServiceRouteAspect`：动态路由切面
- `RequestContextFilter`：确保请求上下文在 AOP 切面中可用
- `ServiceRouteAutoConfiguration`：自动配置类

## 6. 常见问题

### Q1: 如何知道路由到哪个 Bean？

A: 查看日志（需要 DEBUG 级别）：

```
【动态路由】正在路由到Bean: [newUserService], 方法: [findById], 参数: [1]
```

### Q2: 条件表达式写错了怎么办？

A: SpEL 表达式解析失败时，会记录错误日志并返回 `false`，走默认实现。不会中断请求。

### Q3: 如何禁用表达式缓存？

A: 设置 `cacheExpression = false`：

```java
@ServiceRoute(
    condition = "...",
    targetBean = "...",
    cacheExpression = false  // 每次都重新编译
)
```

### Q4: 支持 Groovy 表达式吗？

A: 当前版本只支持 SpEL（Spring Expression Language），不支持 Groovy。

### Q5: 如何处理多个目标 Bean？

A: 使用嵌套的条件表达式：

```java
condition = "#type == 'A' ? #beanA : ( #type == 'B' ? #beanB : #defaultBean )"
```
