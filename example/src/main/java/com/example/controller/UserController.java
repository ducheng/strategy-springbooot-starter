package com.example.controller;

import com.example.model.User;
import com.example.service.UserService;
import com.strategy.starter.annotation.ServiceRoute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户控制器
 *
 * <p>演示如何使用 @ServiceRoute 注解实现动态路由
 *
 * @author Example
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    /**
     * 默认的用户服务（@ServiceRoute 会自动将 defaultBean 设为 @Primary，无需 @Qualifier）
     */
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 示例1: 根据 HTTP Header 动态路由
     *
     * <p>使用场景: 当请求头 version=v2 时，使用新实现；否则使用老实现
     *
     * <p>测试命令:
     * <pre>
     * # 使用老实现（默认）
     * curl http://localhost:8080/api/users/1
     *
     * # 使用新实现
     * curl -H "version: v2" http://localhost:8080/api/users/1
     * </pre>
     */
    @GetMapping("/{id}")
    @ServiceRoute(
        condition = "#request.getHeader('version') == 'v2'",
        targetBean = "newUserService",
        defaultBean = "oldUserService"
    )
    public User getUserById(@PathVariable Long id) {
        log.info("正在查询用户，ID: {}", id);
        // 实际执行的服务由 @ServiceRoute 注解决定
        return userService.findById(id);
    }

    /**
     * 示例2: 根据请求参数动态路由
     *
     * <p>使用场景: 当请求参数 useNew=true 时，使用新实现；否则使用老实现
     *
     * <p>测试命令:
     * <pre>
     * # 使用老实现
     * curl http://localhost:8080/api/users/1/info
     *
     * # 使用新实现
     * curl "http://localhost:8080/api/users/1/info?useNew=true"
     * </pre>
     */
    @GetMapping("/{id}/info")
    @ServiceRoute(
        condition = "#useNew == true",
        targetBean = "newUserService",
        defaultBean = "oldUserService"
    )
    public User getUserInfo(@PathVariable Long id, @RequestParam(required = false) Boolean useNew) {
        log.info("正在查询用户信息，ID: {}, useNew: {}", id, useNew);
        return userService.findById(id);
    }

    /**
     * 示例3: 根据用户ID范围动态路由
     *
     * <p>使用场景: ID > 100 的用户走新实现（假设新系统ID从100开始）
     *
     * <p>测试命令:
     * <pre>
     * # ID=1 走老实现
     * curl http://localhost:8080/api/users/route/1
     *
     * # ID=101 走新实现
     * curl http://localhost:8080/api/users/route/101
     * </pre>
     */
    @GetMapping("/route/{id}")
    @ServiceRoute(
        condition = "#id > 100",
        targetBean = "newUserService",
        defaultBean = "oldUserService"
    )
    public User getUserWithIdRoute(@PathVariable Long id) {
        log.info("正在查询用户（根据ID路由），ID: {}", id);
        return userService.findById(id);
    }

    /**
     * 示例4: 复杂的 SpEL 表达式 - 多条件组合
     *
     * <p>使用场景: 根据多个 Header 和参数组合判断
     * <ul>
     *   <li>当 tenantId=vip 且 version=v2 时，使用新实现</li>
     *   <li>或者当 forceNew=true 时，使用新实现</li>
     * </ul>
     *
     * <p>测试命令:
     * <pre>
     * # 走老实现
     * curl http://localhost:8080/api/users/check/1
     *
     * # 走新实现（方式1）
     * curl -H "tenantId: vip" -H "version: v2" http://localhost:8080/api/users/check/1
     *
     * # 走新实现（方式2）
     * curl "http://localhost:8080/api/users/check/1?forceNew=true"
     * </pre>
     */
    @GetMapping("/check/{id}")
    @ServiceRoute(
        condition = "#request.getHeader('tenantId') == 'vip' and #request.getHeader('version') == 'v2' or #forceNew == true",
        targetBean = "newUserService",
        defaultBean = "oldUserService"
    )
    public User checkUser(@PathVariable Long id, @RequestParam(required = false) Boolean forceNew) {
        log.info("正在检查用户，ID: {}, forceNew: {}", id, forceNew);
        return userService.findById(id);
    }

    /**
     * 示例5: POST 请求的动态路由
     *
     * <p>使用场景: 创建用户时，根据请求头决定使用哪个实现
     *
     * <p>测试命令:
     * <pre>
     * # 使用老实现创建
     * curl -X POST -H "Content-Type: application/json" \
     *   -d '{"username":"test1","email":"test1@example.com"}' \
     *   http://localhost:8080/api/users
     *
     * # 使用新实现创建
     * curl -X POST -H "Content-Type: application/json" -H "source: third-party" \
     *   -d '{"username":"test2","email":"test2@example.com"}' \
     *   http://localhost:8080/api/users
     * </pre>
     */
    @PostMapping
    @ServiceRoute(
        condition = "#request.getHeader('source') == 'third-party'",
        targetBean = "newUserService",
        defaultBean = "oldUserService"
    )
    public User createUser(@RequestBody User user) {
        log.info("正在创建用户，用户名: {}", user.getUsername());
        return userService.create(user);
    }

    /**
     * 示例6: 使用表达式访问对象属性
     *
     * <p>使用场景: 根据请求体中的某个字段值决定路由
     *
     * <p>测试命令:
     * <pre>
     * # username=admin-v2 时走新实现
     * curl -X POST -H "Content-Type: application/json" \
     *   -d '{"username":"admin-v2","email":"admin@example.com"}' \
     *   http://localhost:8080/api/users/register
     * </pre>
     */
    @PostMapping("/register")
    @ServiceRoute(
        condition = "#user.username != null and #user.username.endsWith('-v2')",
        targetBean = "newUserService",
        defaultBean = "oldUserService"
    )
    public User register(@RequestBody User user) {
        log.info("正在注册用户: {}", user.getUsername());
        return userService.create(user);
    }

    /**
     * 示例7: 获取服务描述（无参数）
     *
     * <p>注意: 无参数的方法也可以正常工作
     *
     * <p>测试命令:
     * <pre>
     * # 老实现
     * curl http://localhost:8080/api/users/description
     *
     * # 新实现
     * curl -H "new: true" http://localhost:8080/api/users/description
     * </pre>
     */
    @GetMapping("/description")
    @ServiceRoute(
        condition = "#request.getHeader('new') == 'true'",
        targetBean = "newUserService",
        defaultBean = "oldUserService"
    )
    public String getDescription() {
        log.info("正在获取服务描述");
        return userService.getDescription();
    }
}
