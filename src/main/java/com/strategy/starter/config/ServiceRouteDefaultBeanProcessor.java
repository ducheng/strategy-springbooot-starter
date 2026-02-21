package com.strategy.starter.config;

import com.strategy.starter.annotation.ServiceRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 自动将 @ServiceRoute 中指定的 defaultBean 标记为 @Primary
 *
 * <p>通过 BeanFactoryPostProcessor 在 Bean 实例化之前扫描所有 Controller，
 * 找出 @ServiceRoute 注解中声明的 defaultBean，并将其 BeanDefinition 设为 primary，
 * 使得用户注入接口时无需 @Qualifier 即可获得默认实现。
 *
 * @author Strategy Starter
 * @since 1.0.0
 */
public class ServiceRouteDefaultBeanProcessor implements BeanFactoryPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(ServiceRouteDefaultBeanProcessor.class);

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Set<String> defaultBeanNames = new LinkedHashSet<>();

        // 遍历所有 BeanDefinition，找到 Controller 类
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
            String beanClassName = bd.getBeanClassName();
            if (beanClassName == null) {
                continue;
            }

            Class<?> beanClass;
            try {
                beanClass = ClassUtils.forName(beanClassName, beanFactory.getBeanClassLoader());
            } catch (ClassNotFoundException e) {
                log.debug("无法加载类: {}, 跳过", beanClassName);
                continue;
            }

            // 使用 AnnotationUtils 支持元注解（如 @RestController 内含 @Controller）
            if (AnnotationUtils.findAnnotation(beanClass, Controller.class) == null) {
                continue;
            }

            // 扫描该 Controller 的所有方法
            for (Method method : beanClass.getDeclaredMethods()) {
                ServiceRoute serviceRoute = AnnotationUtils.findAnnotation(method, ServiceRoute.class);
                if (serviceRoute == null) {
                    continue;
                }

                String defaultBean = serviceRoute.defaultBean();
                if (StringUtils.hasText(defaultBean)) {
                    defaultBeanNames.add(defaultBean);
                    log.debug("发现 @ServiceRoute defaultBean: [{}], 来自 {}.{}()",
                            defaultBean, beanClass.getSimpleName(), method.getName());
                }
            }
        }

        // 按接口类型检测冲突：同一接口不应有多个 defaultBean
        Map<Class<?>, String> interfacePrimaryMap = new HashMap<>();

        // 将收集到的 defaultBean 标记为 primary
        for (String defaultBeanName : defaultBeanNames) {
            if (!beanFactory.containsBeanDefinition(defaultBeanName)) {
                log.warn("@ServiceRoute 中声明的 defaultBean [{}] 在容器中不存在，忽略", defaultBeanName);
                continue;
            }

            BeanDefinition bd = beanFactory.getBeanDefinition(defaultBeanName);
            if (bd.isPrimary()) {
                log.debug("Bean [{}] 已经是 primary，跳过", defaultBeanName);
                continue;
            }

            // 冲突检测：检查该 Bean 实现的接口是否已有其他 primary Bean
            try {
                Class<?> beanClass = ClassUtils.forName(bd.getBeanClassName(), beanFactory.getBeanClassLoader());
                for (Class<?> iface : beanClass.getInterfaces()) {
                    String existing = interfacePrimaryMap.get(iface);
                    if (existing != null && !existing.equals(defaultBeanName)) {
                        log.warn("接口 [{}] 存在多个 @ServiceRoute defaultBean: [{}] 和 [{}]，" +
                                "将以最后扫描到的 [{}] 为准", iface.getSimpleName(), existing, defaultBeanName, defaultBeanName);
                    }
                    interfacePrimaryMap.put(iface, defaultBeanName);
                }
            } catch (ClassNotFoundException e) {
                log.debug("冲突检测时无法加载类: {}", bd.getBeanClassName());
            }

            bd.setPrimary(true);
            log.info("【ServiceRoute】已将 defaultBean [{}] 设置为 @Primary", defaultBeanName);
        }
    }
}
