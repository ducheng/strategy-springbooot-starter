# Strategy Spring Boot Starter - 使用指南

## 项目简介

这是一个策略模式的 Spring Boot Starter 示例项目，演示如何在应用运行时根据不同条件动态路由到不同的服务实现。

### 核心特性

- **动态路由**：根据请求参数、Header 或业务条件，在不修改代码的情况下切换服务实现
- **零侵入**：通过简单的条件判断实现新旧系统平滑迁移
- **灵活配置**：支持多种路由条件（Header、参数、业务逻辑等）

### 应用场景

- **灰度发布**：逐步将流量从旧系统切换到新系统
- **A/B 测试**：根据用户特征路由到不同实现
- **多租户系统**：不同租户使用不同的业务逻辑
- **系统迁移**：新旧系统并存期间平滑过渡

---

## 快速开始

### 1. 运行应用

```bash
# 方式一：使用 Maven 运行
cd example
mvn spring-boot:run

# 方式二：使用 JAR 包运行
java -jar example/target/dynamic-route-example-1.0.0-SNAPSHOT.jar
```

应用启动后访问：http://localhost:8080

### 2. 验证服务

```bash
# 检查服务是否启动
curl http://localhost:8080/api/users/description
# 返回：老的用户服务实现 - 基于本地HashMap存储
```

---

## API 接口说明

### 1. 根据 HTTP Header 路由

**接口**：`GET /api/users/{id}`

**路由规则**：
- 请求头 `version=v2` → 使用新服务
- 无请求头或其他值 → 使用老服务

**测试命令**：
```bash
# 使用老服务
curl http://localhost:8080/api/users/1

# 使用新服务
curl -H "version: v2" http://localhost:8080/api/users/1
```

**响应对比**：
```json
// 老服务响应
{"id":1,"username":"old-user-1","email":"old@example.com","source":"OLD_IMPLEMENTATION"}

// 新服务响应
{"id":1,"username":"new-user-1","email":"new@third-party.com","source":"NEW_IMPLEMENTATION_THIRD_PARTY"}
```

---

### 2. 根据请求参数路由

**接口**：`GET /api/users/{id}/info`

**路由规则**：
- 参数 `useNew=true` → 使用新服务
- 其他情况 → 使用老服务

**测试命令**：
```bash
# 使用老服务
curl http://localhost:8080/api/users/1/info

# 使用新服务
curl "http://localhost:8080/api/users/1/info?useNew=true"
```

---

### 3. 根据业务 ID 范围路由

**接口**：`GET /api/users/route/{id}`

**路由规则**：
- `id > 100` → 使用新服务（新系统用户）
- `id ≤ 100` → 使用老服务（老系统用户）

**测试命令**：
```bash
# ID=1，使用老服务
curl http://localhost:8080/api/users/route/1

# ID=101，使用新服务
curl http://localhost:8080/api/users/route/101
```

---

### 4. 多条件组合路由

**接口**：`GET /api/users/check/{id}`

**路由规则**（满足以下任一条件即使用新服务）：
- 请求头 `tenantId=vip` 且 `version=v2`
- 参数 `forceNew=true`

**测试命令**：
```bash
# 使用老服务
curl http://localhost:8080/api/users/check/1

# 使用新服务（方式1：VIP + v2）
curl -H "tenantId: vip" -H "version: v2" http://localhost:8080/api/users/check/1

# 使用新服务（方式2：强制使用新服务）
curl "http://localhost:8080/api/users/check/1?forceNew=true"
```

---

### 5. POST 请求路由

**接口**：`POST /api/users`

**路由规则**：
- 请求头 `source=third-party` → 使用新服务
- 其他情况 → 使用老服务

**测试命令**：
```bash
# 使用老服务创建
curl -X POST -H "Content-Type: application/json" \
  -d '{"username":"test1","email":"test1@example.com"}' \
  http://localhost:8080/api/users

# 使用新服务创建
curl -X POST -H "Content-Type: application/json" -H "source: third-party" \
  -d '{"username":"test2","email":"test2@example.com"}' \
  http://localhost:8080/api/users
```

---

### 6. 根据请求体字段路由

**接口**：`POST /api/users/register`

**路由规则**：
- 用户名以 `-v2` 结尾 → 使用新服务
- 其他情况 → 使用老服务

**测试命令**：
```bash
# 使用老服务注册
curl -X POST -H "Content-Type: application/json" \
  -d '{"username":"admin","email":"admin@example.com"}' \
  http://localhost:8080/api/users/register

# 使用新服务注册
curl -X POST -H "Content-Type: application/json" \
  -d '{"username":"admin-v2","email":"admin@example.com"}' \
  http://localhost:8080/api/users/register
```

---

### 7. 无参数接口路由

**接口**：`GET /api/users/description`

**路由规则**：
- 请求头 `new=true` → 使用新服务
- 其他情况 → 使用老服务

**测试命令**：
```bash
# 使用老服务
curl http://localhost:8080/api/users/description
# 返回：老的用户服务实现 - 基于本地HashMap存储

# 使用新服务
curl -H "new: true" http://localhost:8080/api/users/description
# 返回：新的用户服务实现 - 对接第三方API
```

---

## 项目结构

```
strategy-springbooot-starter/
├── src/main/java/com/strategy/starter/     # Starter 核心代码
│   ├── annotation/ServiceRoute.java        # 路由注解
│   ├── aspect/ServiceRouteAspect.java      # 路由切面
│   └── config/ServiceRouteAutoConfiguration.java
│
├── example/                               # 示例应用
│   ├── src/main/java/com/example/
│   │   ├── controller/UserController.java   # REST 控制器
│   │   ├── service/
│   │   │   ├── UserService.java            # 服务接口
│   │   │   └── impl/
│   │   │       ├── OldUserServiceImpl.java  # 老服务实现
│   │   │       └── NewUserServiceImpl.java # 新服务实现
│   │   └── ExampleApplication.java         # 启动类
│   └── target/
│       └── dynamic-route-example-1.0.0-SNAPSHOT.jar  # 可执行 JAR
│
└── pom.xml                                # Maven 配置
```

---

## 核心实现原理

本项目通过在 Controller 层进行条件判断，动态选择调用哪个服务实现：

```java
@GetMapping("/{id}")
public User getUserById(@PathVariable Long id, HttpServletRequest request) {
    String version = request.getHeader("version");
    UserService service = "v2".equals(version) ? newUserService : oldUserService;
    return service.findById(id);
}
```

### 优势

1. **简单直观**：逻辑清晰，易于理解和维护
2. **类型安全**：编译时检查，避免运行时错误
3. **灵活扩展**：可根据业务需求定制任意路由规则
4. **性能优秀**：无需反射或动态代理，零性能损耗

---

## 扩展建议

### 1. 提取路由规则到配置文件

```yaml
routing:
  rules:
    - condition: "version == 'v2'"
      target: "newUserService"
    - condition: "id > 100"
      target: "newUserService"
```

### 2. 使用策略工厂模式

```java
@Service
public class UserServiceFactory {
    public UserService getService(RoutingContext context) {
        // 根据上下文返回对应的服务实现
    }
}
```

### 3. 结合 Spring Cloud Gateway

在网关层实现路由，后端服务无需感知路由逻辑

---

## 测试验证

### 运行所有测试

```bash
# 测试所有接口（一键测试）
./test-all.sh
```

### 预期结果

所有 14 个测试用例应全部通过：

| 序号 | 测试场景 | 状态 |
|-----|---------|-----|
| 1 | Header 版本路由 | ✅ |
| 2 | 参数路由 | ✅ |
| 3 | ID 范围路由 | ✅ |
| 4 | 多条件组合路由 | ✅ |
| 5 | POST 请求路由 | ✅ |
| 6 | 请求体字段路由 | ✅ |
| 7 | 无参数接口路由 | ✅ |

---

## 常见问题

### Q1: 如何在生产环境使用？

A: 建议将路由规则抽取到配置中心（如 Nacos、Apollo），支持动态调整。

### Q2: 路由失败如何处理？

A: 可添加降级策略，如新服务失败时自动切换回老服务。

### Q3: 支持哪些路由条件？

A: 支持任何可通过代码实现的逻辑，如：用户 ID、IP 地域、时间百分比、用户标签等。

### Q4: 性能影响大吗？

A: 使用条件判断方式几乎无性能影响，比 AOP/反射方案更高效。

---

## 版本信息

- **Spring Boot**: 2.6.15
- **JDK**: 1.8+
- **构建工具**: Maven 3.x

---

## 许可证

MIT License
