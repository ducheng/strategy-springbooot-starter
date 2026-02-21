package com.strategy.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 动态路由注解
 *
 * <p>用于Controller层方法，根据SpEL表达式动态决定是执行原有逻辑还是切换到目标Bean
 *
 * <p>使用示例：
 * <pre>
 * &#64;GetMapping("/user")
 * &#64;ServiceRoute(condition = "#request.getHeader('version') == 'v2'", targetBean = "newUserService")
 * public User getUser(Long id) {
 *     // 当version header为v2时，会调用newUserService的同名方法
 *     // 否则执行原有逻辑
 *     return oldService.findById(id);
 * }
 * </pre>
 *
 * @author Strategy Starter
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ServiceRoute {

    /**
     * SpEL表达式条件
     *
     * <p>表达式计算结果为true时，将路由到targetBean
     *
     * <p>可用的SpEL上下文变量：
     * <ul>
     *   <li>#request: 当前 HttpServletRequest 对象</li>
     *   <li>#args: 方法参数数组</li>
     *   <li>#方法参数名: 可直接通过参数名访问，如 #id</li>
     *   <li>#target: 目标对象（被代理对象）</li>
     *   <li>#method: 目标方法对象</li>
     * </ul>
     *
     * <p>可用的正则表达式函数：
     * <ul>
     *   <li>{@code #regex(value, pattern)} — 通用正则匹配，如 {@code #regex(#id.toString(), '\\d{3,}')}</li>
     *   <li>{@code #matchHeader(#request, 'headerName', 'pattern')} — 匹配请求头</li>
     *   <li>{@code #matchParam(#request, 'paramName', 'pattern')} — 匹配请求参数</li>
     *   <li>{@code #headerMatches.match('headerName', 'pattern')} — 简化调用，自动使用当前请求</li>
     *   <li>{@code #paramMatches.match('paramName', 'pattern')} — 简化调用，自动使用当前请求</li>
     * </ul>
     *
     * <p>SpEL 内置 matches 关键字也支持正则：{@code #request.getHeader('version') matches 'v\\d+'}
     *
     * @return SpEL表达式字符串
     */
    String condition();

    /**
     * 目标Bean名称
     *
     * <p>当condition表达式为true时，将从Spring容器获取该Bean并调用与当前方法签名一致的方法
     *
     * @return Spring容器中的Bean名称
     */
    String targetBean();

    /**
     * 默认Bean名称（可选）
     *
     * <p>当condition表达式为false时，将从Spring容器获取该Bean并调用与当前方法签名一致的方法
     *
     * <p>如果为空（默认），则执行原方法逻辑；如果指定了值，则路由到默认Bean
     *
     * <p>使用场景：
     * <pre>
     * // 指定默认Bean：条件不满足时路由到 oldUserService
     * &#64;ServiceRoute(
     *     condition = "#version == 'v2'",
     *     targetBean = "newUserService",
     *     defaultBean = "oldUserService"
     * )
     * public User getUser(Long id) {
     *     // 当 version != 'v2' 时，实际调用 oldUserService.findById(id)
     *     // 当 version == 'v2' 时，实际调用 newUserService.findById(id)
     *     return userService.findById(id);  // 这里的 userService 不会被使用
     * }
     * </pre>
     *
     * @return Spring容器中的默认Bean名称，为空表示执行原方法
     */
    String defaultBean() default "";

    /**
     * 是否启用缓存（默认开启）
     *
     * <p>开启后会缓存编译后的SpEL表达式，提升性能
     *
     * @return true表示启用缓存，false表示不启用
     */
    boolean cacheExpression() default true;
}
