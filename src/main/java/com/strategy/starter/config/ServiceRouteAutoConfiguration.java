package com.strategy.starter.config;

import com.strategy.starter.aspect.ServiceRouteAspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.RequestContextFilter;

import javax.servlet.http.HttpServletRequest;

/**
 * 动态路由自动配置类
 *
 * <p>Spring Boot 2.x 自动配置规范
 *
 * <p>通过 {@code META-INF/spring.factories} 文件进行自动装配
 *
 * @author Strategy Starter
 * @since 1.0.0
 */
@Configuration
@ConditionalOnWebApplication
public class ServiceRouteAutoConfiguration {

    /**
     * 配置 ServiceRouteAspect 切面
     *
     * <p>使用 @ConditionalOnMissingBean 确保用户可以覆盖默认的切面实现
     *
     * <p>通过 ObjectProvider 注入 HttpServletRequest，确保在 AOP 切面中能可靠地获取到当前请求对象
     *
     * @param requestProvider HttpServletRequest 的 ObjectProvider
     * @return ServiceRouteAspect 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ServiceRouteAspect serviceRouteAspect(ObjectProvider<HttpServletRequest> requestProvider) {
        ServiceRouteAspect aspect = new ServiceRouteAspect();
        aspect.setRequestProvider(requestProvider);
        return aspect;
    }

    /**
     * 配置 RequestContextFilter
     *
     * <p>确保 RequestContextHolder 在所有 Web 请求中都能正确获取到 HttpServletRequest，
     * 包括在 AOP 切面拦截 Controller 方法期间。
     *
     * <p>Spring Boot 默认注册此 Filter，但使用 @ConditionalOnMissingBean 以防万一未注册。
     *
     * @return RequestContextFilter 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public RequestContextFilter requestContextFilter() {
        RequestContextFilter filter = new RequestContextFilter();
        filter.setThreadContextInheritable(true);
        return filter;
    }

    /**
     * 配置 ServiceRouteDefaultBeanProcessor
     *
     * <p>将 @ServiceRoute 中声明的 defaultBean 自动设为 @Primary，
     * 使用户注入时无需 @Qualifier 即可获得默认实现
     *
     * <p>注意: BeanFactoryPostProcessor 必须声明为 static 方法，
     * 避免触发 Configuration 类的过早初始化
     *
     * @return ServiceRouteDefaultBeanProcessor 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public static ServiceRouteDefaultBeanProcessor serviceRouteDefaultBeanProcessor() {
        return new ServiceRouteDefaultBeanProcessor();
    }
}

