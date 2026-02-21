# Dynamic Route Spring Boot Starter

一个基于 SpEL 表达式的服务动态路由 Spring Boot Starter，支持在不修改代码的情况下灵活切换服务实现。

## 功能特性

- **动态路由**：根据请求参数、Header 或业务条件动态路由到不同的 Service 实现
- **SpEL 表达式**：强大的条件判断能力，支持复杂逻辑
- **正则表达式**：内置正则匹配函数，支持 Header、参数的模式匹配
- **默认 Bean**：支持可选的默认实现，不命中条件时自动回退
- **零侵入**：通过注解即可实现，无需修改现有业务代码
- **智能方法匹配**：基于参数类型和返回类型的最优匹配算法，自动识别服务接口方法

## 快速开始

### 1. 安装 Starter

```bash
# 克隆项目
git clone <repository-url>

# 安装到本地仓库
cd dynamic-route-springbooot-starter
mvn clean install
```

### 2. 在项目中使用

添加依赖到 `pom.xml`：

```xml
<dependency>
    <groupId>com.strategy</groupId>
    <artifactId>dynamic-route-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 核心注解

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
| `#headerMatches.match(name, pattern)` | 简化版匹配请求头（无需传 request） | `#headerMatches.match('version', 'v\\d+')` |
| `#paramMatches.match(name, pattern)` | 简化版匹配请求参数（无需传 request） | `#paramMatches.match('type', 'premium\|vip')` |

> SpEL 内置的 `matches` 关键字也支持正则：`#request.getHeader('version') matches 'v\\d+'`

## 使用示例

### 示例 1：根据 HTTP Header 路由

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
# 返回: {"source":"OLD_IMPLEMENTATION",...}

# 调用新服务
curl -H "version: v2" http://localhost:8080/api/users/1
# 返回: {"source":"NEW_IMPLEMENTATION_THIRD_PARTY",...}
```

---

### 示例 2：根据请求参数路由

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

---

### 示例 3：根据业务 ID 范围路由

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

---

### 示例 4：多条件组合路由

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

---

### 示例 5：POST 请求路由

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

---

### 示例 6：根据请求体字段路由

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

---

### 示例 7：无参数方法

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

---

### 示例 8：不指定 defaultBean（可选）

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

## 应用场景

### 1. 灰度发布

逐步将流量从旧系统切换到新系统：

```java
@ServiceRoute(
    condition = "T(Math.random()) < 0.1",  // 10% 流量
    targetBean = "newSystemService"
)
```

### 2. 多租户系统

不同租户使用不同的实现：

```java
@ServiceRoute(
    condition = "#request.getHeader('X-Tenant-Id') == 'tenant-a'",
    targetBean = "serviceImplementationA",
    defaultBean = "serviceImplementationB"
)
```

### 3. A/B 测试

根据用户特征进行测试：

```java
@ServiceRoute(
    condition = "#user.id % 2 == 0",  // 偶数 ID
    targetBean = "experimentalService"
)
```

### 4. 功能开关

动态控制新功能的启用：

```java
@ServiceRoute(
    condition = "@featureToggleService.isEnabled('new-feature')",
    targetBean = "newFeatureService"
)
```

## 原理说明

### 执行流程

```
1. 请求到达 @ServiceRoute 标注的 Controller 方法
2. 切面拦截请求，解析 SpEL 条件表达式
3. 根据 SpEL 变量（request、args、method 等）计算条件值
4. 如果条件为 true：
   - 路由到 targetBean
   - 查找与服务接口匹配的方法
   - 通过反射调用目标方法
5. 如果指定了 defaultBean 且条件为 false：
   - 路由到 defaultBean
   - 执行相同的逻辑
6. 否则：
   - 执行原方法（proceed）
```

### 方法匹配策略

切面使用**最优匹配算法**智能识别服务方法：

1. **精确匹配**（优先级最高）：方法名 + 参数类型完全一致
2. **兼容匹配**：遍历服务接口所有方法，按以下规则评分
   - 参数类型兼容（Controller 前 N 个参数类型与 Service 参数类型匹配）
   - 返回类型兼容（Service 返回类型必须能赋值给 Controller 返回类型）
   - 参数数量完全一致 → 额外加分
   - 选择得分最高的方法
3. **安全过滤**：Controller 有参数时，自动跳过无参候选方法

### 性能优化

- **SpEL 表达式缓存**：编译后的表达式会被缓存，避免重复解析
- **正则表达式缓存**：编译后的 Pattern 对象会被缓存复用
- **反射调用优化**：目标方法设置为可访问后会被缓存
- **零拷贝**：无需数据转换，直接传递参数

## 配置说明

### Spring Boot 自动配置

Starter 会自动配置以下组件：

- `ServiceRouteAspect`：动态路由切面（通过 `ObjectProvider` 注入 `HttpServletRequest`）
- `RequestContextFilter`：确保请求上下文在 AOP 切面中可用
- `ServiceRouteAutoConfiguration`：自动配置类

### 日志配置

调整日志级别查看详细的路由信息：

```yaml
logging:
  level:
    com.strategy.starter: DEBUG
```

## 常见问题

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

A: 使用嵌套的条件表达式或使用多个 `@ServiceRoute` 注解：

```java
// 方式1：嵌套表达式
condition = "#type == 'A' ? #beanA : ( #type == 'B' ? #beanB : #defaultBean )"

// 方式2：多个注解（不建议）
// 需要手动处理逻辑
```

## 版本信息

- **Starter 版本**: 1.0.0
- **Spring Boot**: 2.6.15
- **JDK**: 1.8+
- **构建工具**: Maven 3.x

## 版本状态

### 功能状态

- ✅ **Starter JAR 已生成**：`target/dynamic-route-spring-boot-starter-1.0.0.jar`
- ✅ **@ServiceRoute 注解**：支持 `targetBean`、`defaultBean`、`condition`
- ✅ **GET 请求路由**：带参数和无参数的 GET 请求均正常工作
- ✅ **POST 请求路由**：支持 `@RequestBody` 参数路由
- ✅ **正则表达式**：内置 `#regex`、`#matchHeader`、`#matchParam` 等函数
- ✅ **智能方法匹配**：基于参数类型 + 返回类型 + 最优得分的匹配策略

### 测试验证结果

| 测试场景 | 状态 |
|----------|------|
| 无参数 GET 接口 | ✅ 正常 |
| 带参数 GET 接口（PathVariable） | ✅ 正常 |
| 带参数 GET 接口（RequestParam） | ✅ 正常 |
| 多条件组合 GET 接口 | ✅ 正常 |
| POST 接口（RequestBody） | ✅ 正常 |
| Header / 参数正则匹配 | ✅ 正常 |

## 许可证

MIT License

---

**作者**: Strategy Starter
**文档更新**: 2026-02-14
