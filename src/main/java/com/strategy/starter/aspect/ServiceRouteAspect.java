package com.strategy.starter.aspect;

import com.strategy.starter.annotation.ServiceRoute;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 动态路由切面
 *
 * <p>拦截标注了 @ServiceRoute 的方法，根据SpEL表达式决定是否路由到目标Bean
 *
 * @author Strategy Starter
 * @since 1.0.0
 */
@Aspect
public class ServiceRouteAspect implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(ServiceRouteAspect.class);

    /**
     * SpEL 表达式解析器
     */
    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 参数名发现器
     */
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * SpEL 表达式缓存
     */
    private final ConcurrentHashMap<String, Expression> expressionCache = new ConcurrentHashMap<>(64);

    /**
     * 正则表达式缓存（避免重复编译）
     */
    private static final ConcurrentHashMap<String, Pattern> regexCache = new ConcurrentHashMap<>(32);

    /**
     * Spring 应用上下文
     */
    private ApplicationContext applicationContext;

    /**
     * HttpServletRequest 的 ObjectProvider（延迟获取，请求作用域安全）
     */
    private ObjectProvider<HttpServletRequest> requestProvider;

    /**
     * 设置 HttpServletRequest 的 ObjectProvider
     *
     * @param requestProvider 请求对象的 ObjectProvider
     */
    public void setRequestProvider(ObjectProvider<HttpServletRequest> requestProvider) {
        this.requestProvider = requestProvider;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 环绕通知 - 处理动态路由逻辑
     */
    @Around("@annotation(serviceRoute)")
    public Object around(ProceedingJoinPoint joinPoint, ServiceRoute serviceRoute) throws Throwable {
        // 获取目标方法
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method controllerMethod = signature.getMethod();

        // 处理桥接方法（处理泛型等情况）
        if (controllerMethod.getDeclaringClass().isInterface()) {
            try {
                controllerMethod = joinPoint.getTarget().getClass().getDeclaredMethod(
                    signature.getName(),
                    controllerMethod.getParameterTypes()
                );
            } catch (NoSuchMethodException e) {
                controllerMethod = AopUtils.getMostSpecificMethod(controllerMethod, joinPoint.getTarget().getClass());
            }
        }

        // 解析 SpEL 表达式
        boolean shouldRoute = evaluateExpression(
            serviceRoute.condition(),
            controllerMethod,
            joinPoint.getArgs(),
            joinPoint.getTarget()
        );

        // 确定目标 Bean 名称
        String targetBeanName = shouldRoute ? serviceRoute.targetBean() : serviceRoute.defaultBean();

        // 如果没有指定目标 Bean，则执行原方法
        if (!StringUtils.hasText(targetBeanName)) {
            return joinPoint.proceed();
        }

        // 路由到目标Bean
        return routeToTargetBean(targetBeanName, controllerMethod, joinPoint.getArgs());
    }

    /**
     * 在目标Bean的接口中查找与Controller方法参数兼容的服务方法
     *
     * <p>匹配规则（按优先级排序）：
     * <ol>
     *   <li>参数数量完全相同且类型兼容 → 最高优先</li>
     *   <li>参数数量少于Controller但类型兼容 → 次优先（取前N个参数）</li>
     *   <li>返回类型必须与Controller方法返回类型兼容</li>
     *   <li>多个候选时，选择参数数量最多的（最具体的匹配）</li>
     * </ol>
     *
     * @param targetBean       目标Bean实例
     * @param controllerMethod Controller方法
     * @return 匹配的服务方法，找不到返回null
     */
    private Method findCompatibleServiceMethod(Object targetBean, Method controllerMethod) {
        Class<?>[] controllerParamTypes = controllerMethod.getParameterTypes();
        Class<?> controllerReturnType = controllerMethod.getReturnType();

        Method bestMatch = null;
        int bestScore = -1;

        // 遍历目标Bean的所有接口
        for (Class<?> iface : targetBean.getClass().getInterfaces()) {
            for (Method method : iface.getMethods()) {
                int serviceParamCount = method.getParameterCount();

                // Service方法参数不能多于Controller方法参数
                if (serviceParamCount > controllerParamTypes.length) {
                    continue;
                }

                // 跳过无参方法（当Controller方法有参数时，0参数方法不应被匹配）
                if (serviceParamCount == 0 && controllerParamTypes.length > 0) {
                    continue;
                }

                // 检查返回类型兼容性（Service返回类型必须能赋值给Controller返回类型）
                Class<?> serviceReturnType = method.getReturnType();
                if (controllerReturnType != void.class && serviceReturnType != void.class) {
                    if (!controllerReturnType.isAssignableFrom(serviceReturnType)) {
                        log.debug("跳过方法 {}.{}，返回类型不兼容: {} -> {}",
                            iface.getSimpleName(), method.getName(),
                            serviceReturnType.getSimpleName(), controllerReturnType.getSimpleName());
                        continue;
                    }
                }

                // 检查前N个参数类型是否兼容
                Class<?>[] serviceParamTypes = method.getParameterTypes();
                boolean paramsMatch = true;
                for (int i = 0; i < serviceParamCount; i++) {
                    if (!isTypeCompatible(controllerParamTypes[i], serviceParamTypes[i])) {
                        paramsMatch = false;
                        break;
                    }
                }

                if (!paramsMatch) {
                    continue;
                }

                // 计算匹配得分：参数数量越多越好，完全匹配额外加分
                int score = serviceParamCount * 10;
                if (serviceParamCount == controllerParamTypes.length) {
                    score += 100;  // 参数数量完全一致，最高优先级
                }

                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = method;
                    log.debug("找到更优匹配: {}.{}, 参数数量: {}, 得分: {}",
                        iface.getSimpleName(), method.getName(), serviceParamCount, score);
                }
            }
        }

        if (bestMatch != null) {
            log.debug("最终选择服务方法: {}, 参数数量: {}, 得分: {}",
                bestMatch.getName(), bestMatch.getParameterCount(), bestScore);
        }

        return bestMatch;
    }

    /**
     * 评估 SpEL 表达式
     */
    private boolean evaluateExpression(String condition, Method method, Object[] args, Object target) {
        if (!StringUtils.hasText(condition)) {
            return false;
        }

        try {
            Expression expression = getExpression(condition);
            EvaluationContext context = createEvaluationContext(method, args, target);

            // 安全检查：确保 request 变量可用
            if (condition.contains("#request")) {
                Object requestVar = context.lookupVariable("request");
                if (requestVar == null) {
                    log.warn("SpEL表达式需要 #request 变量，但当前上下文中该变量为 null，直接返回 false");
                    return false;
                }
            }

            Object result = expression.getValue(context);

            if (result == null) {
                return false;
            }
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            if (result instanceof String) {
                return Boolean.parseBoolean((String) result);
            }
            if (result instanceof Number) {
                return ((Number) result).intValue() != 0;
            }

            log.warn("SpEL表达式 [{}] 返回非布尔值: {} (类型: {}), 将作为 false 处理",
                condition, result, result.getClass().getName());
            return false;

        } catch (Exception e) {
            log.error("SpEL表达式解析失败: {}, 错误: {}", condition, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取编译后的 Expression 对象（支持缓存）
     */
    private Expression getExpression(String condition) {
        Expression expression = expressionCache.get(condition);
        if (expression == null) {
            expression = parser.parseExpression(condition);
            expressionCache.put(condition, expression);
        }
        return expression;
    }

    /**
     * 创建 SpEL 评估上下文
     */
    private EvaluationContext createEvaluationContext(Method method, Object[] args, Object target) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setRootObject(target);

        // 注册方法参数
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        context.setVariable("args", args);
        context.setVariable("target", target);
        context.setVariable("method", method);

        // 设置 #request 变量（HTTP请求对象）
        HttpServletRequest request = resolveHttpServletRequest(args);
        if (request != null) {
            context.setVariable("request", request);
        } else {
            log.debug("当前上下文中无法获取 HttpServletRequest");
        }

        // 注册正则表达式相关的 SpEL 辅助函数
        registerRegexFunctions(context, request);

        return context;
    }

    /**
     * 多策略解析 HttpServletRequest
     *
     * <p>按以下优先级获取 HttpServletRequest：
     * <ol>
     *   <li>从 ObjectProvider 获取（最可靠，Spring 注入的请求作用域代理）</li>
     *   <li>从 RequestContextHolder 获取（标准方式）</li>
     *   <li>从方法参数中查找 HttpServletRequest 类型的参数（兜底方式）</li>
     * </ol>
     *
     * @param args 方法参数数组
     * @return HttpServletRequest 实例，没有则返回 null
     */
    private HttpServletRequest resolveHttpServletRequest(Object[] args) {
        // 策略1: 通过 ObjectProvider 获取（最可靠）
        if (requestProvider != null) {
            try {
                HttpServletRequest request = requestProvider.getIfAvailable();
                if (request != null) {
                    return request;
                }
            } catch (Exception e) {
                log.debug("从 ObjectProvider 获取 HttpServletRequest 失败: {}", e.getMessage());
            }
        }

        // 策略2: 通过 RequestContextHolder 获取
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            log.debug("从 RequestContextHolder 获取 HttpServletRequest 失败: {}", e.getMessage());
        }

        // 策略3: 从方法参数中查找 HttpServletRequest
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof HttpServletRequest) {
                    log.debug("从方法参数中获取到 HttpServletRequest");
                    return (HttpServletRequest) arg;
                }
            }
        }

        return null;
    }

    /**
     * 注册正则表达式相关的 SpEL 辅助函数
     *
     * <p>注册以下函数到 SpEL 上下文中：
     * <ul>
     *   <li>{@code #regex(value, pattern)} - 通用正则匹配</li>
     *   <li>{@code #matchHeader(request, headerName, pattern)} - 匹配请求头</li>
     *   <li>{@code #matchParam(request, paramName, pattern)} - 匹配请求参数</li>
     * </ul>
     *
     * <p>同时注册了简化变量 {@code #headerMatches} 和 {@code #paramMatches}，
     * 调用时不需要手动传递 request 参数.
     */
    private void registerRegexFunctions(StandardEvaluationContext context, HttpServletRequest request) {
        try {
            // #regex(value, pattern) - 通用正则匹配
            context.registerFunction("regex",
                ServiceRouteAspect.class.getDeclaredMethod("regex", String.class, String.class));

            // #matchHeader(request, headerName, pattern) - 匹配请求头
            context.registerFunction("matchHeader",
                ServiceRouteAspect.class.getDeclaredMethod("matchHeader",
                    HttpServletRequest.class, String.class, String.class));

            // #matchParam(request, paramName, pattern) - 匹配请求参数
            context.registerFunction("matchParam",
                ServiceRouteAspect.class.getDeclaredMethod("matchParam",
                    HttpServletRequest.class, String.class, String.class));

            // 注册简化包装变量，调用时无需手动传递 #request
            if (request != null) {
                context.setVariable("headerMatches", new HeaderMatchFunction(request));
                context.setVariable("paramMatches", new ParamMatchFunction(request));
            }
        } catch (NoSuchMethodException e) {
            log.error("注册 SpEL 正则函数失败", e);
        }
    }

    /**
     * 通用正则匹配函数（供 SpEL 调用）
     *
     * <p>用法: {@code #regex(#someValue, 'pattern')}
     *
     * @param value   待匹配的字符串，null 则返回 false
     * @param pattern 正则表达式
     * @return 是否匹配
     */
    public static boolean regex(String value, String pattern) {
        if (value == null || pattern == null) {
            return false;
        }
        Pattern compiled = regexCache.computeIfAbsent(pattern, Pattern::compile);
        return compiled.matcher(value).matches();
    }

    /**
     * 匹配 HTTP 请求头（供 SpEL 调用）
     *
     * <p>用法: {@code #matchHeader(#request, 'headerName', 'pattern')}
     *
     * @param request    HTTP 请求对象
     * @param headerName 请求头名称
     * @param pattern    正则表达式
     * @return 是否匹配
     */
    public static boolean matchHeader(HttpServletRequest request, String headerName, String pattern) {
        if (request == null || headerName == null) {
            return false;
        }
        String value = request.getHeader(headerName);
        return regex(value, pattern);
    }

    /**
     * 匹配 HTTP 请求参数（供 SpEL 调用）
     *
     * <p>用法: {@code #matchParam(#request, 'paramName', 'pattern')}
     *
     * @param request   HTTP 请求对象
     * @param paramName 请求参数名称
     * @param pattern   正则表达式
     * @return 是否匹配
     */
    public static boolean matchParam(HttpServletRequest request, String paramName, String pattern) {
        if (request == null || paramName == null) {
            return false;
        }
        String value = request.getParameter(paramName);
        return regex(value, pattern);
    }

    /**
     * Header 正则匹配的简化包装
     *
     * <p>通过 {@code #headerMatches.match('name', 'pattern')} 调用，不需要传递 request
     */
    public static class HeaderMatchFunction {
        private final HttpServletRequest request;

        public HeaderMatchFunction(HttpServletRequest request) {
            this.request = request;
        }

        public boolean match(String headerName, String pattern) {
            return matchHeader(request, headerName, pattern);
        }
    }

    /**
     * 请求参数正则匹配的简化包装
     *
     * <p>通过 {@code #paramMatches.match('name', 'pattern')} 调用，不需要传递 request
     */
    public static class ParamMatchFunction {
        private final HttpServletRequest request;

        public ParamMatchFunction(HttpServletRequest request) {
            this.request = request;
        }

        public boolean match(String paramName, String pattern) {
            return matchParam(request, paramName, pattern);
        }
    }

    /**
     * 路由到目标Bean并执行方法
     *
     * <p>方法解析策略：
     * <ol>
     *   <li>先按方法名+参数类型精确匹配</li>
     *   <li>若精确匹配失败，在目标Bean的接口中查找参数类型兼容的方法</li>
     * </ol>
     */
    private Object routeToTargetBean(String targetBeanName, Method controllerMethod, Object[] args)
            throws Throwable {

        Assert.hasText(targetBeanName, "目标Bean名称不能为空");
        Object targetBean = applicationContext.getBean(targetBeanName);

        // 策略1: 按方法名精确匹配（Controller方法名与Service方法名相同的情况）
        Method targetMethod = findMatchingMethod(targetBean.getClass(), controllerMethod);
        Object[] targetArgs = args;

        // 策略2: 在目标Bean的接口中查找参数类型兼容的方法
        if (targetMethod == null) {
            Method compatibleMethod = findCompatibleServiceMethod(targetBean, controllerMethod);
            if (compatibleMethod != null) {
                // 在目标Bean类中找到该接口方法的具体实现
                targetMethod = findMatchingMethod(targetBean.getClass(), compatibleMethod);
                if (targetMethod == null) {
                    targetMethod = compatibleMethod;
                }
                // 只传递Service方法需要的参数（取Controller参数的前N个）
                int serviceParamCount = compatibleMethod.getParameterCount();
                if (serviceParamCount < args.length) {
                    targetArgs = new Object[serviceParamCount];
                    System.arraycopy(args, 0, targetArgs, 0, serviceParamCount);
                }
            }
        }

        if (targetMethod == null) {
            log.error("【动态路由】找不到匹配方法。目标Bean: [{}], Bean类型: [{}], Controller方法: [{}], 参数类型: {}",
                targetBeanName,
                targetBean.getClass().getName(),
                controllerMethod.getName(),
                java.util.Arrays.toString(controllerMethod.getParameterTypes()));
            throw new IllegalStateException(
                String.format("在Bean [%s] 中找不到与Controller方法 [%s] 兼容的服务方法。Bean类型: %s",
                    targetBeanName,
                    controllerMethod.getName(),
                    targetBean.getClass().getName())
            );
        }

        if (!targetMethod.isAccessible()) {
            targetMethod.setAccessible(true);
        }

        log.info("【动态路由】正在路由到Bean: [{}], 方法: [{}], 参数: {}",
            targetBeanName, targetMethod.getName(), java.util.Arrays.toString(targetArgs));

        try {
            return targetMethod.invoke(targetBean, targetArgs);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException != null) {
                throw targetException;
            }
            throw e;
        } catch (Exception e) {
            log.error("反射调用目标方法失败: Bean=[{}], Method=[{}], Error={}",
                targetBeanName, targetMethod.getName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 在目标类中查找与源方法签名一致的方法
     */
    private Method findMatchingMethod(Class<?> targetClass, Method sourceMethod) {
        String methodName = sourceMethod.getName();
        Class<?>[] parameterTypes = sourceMethod.getParameterTypes();

        // 先尝试精确匹配
        try {
            return targetClass.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            // 精确匹配失败，尝试查找声明的方法
        }

        // 遍历所有声明的方法
        for (Method method : targetClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)
                && isParameterTypesMatch(method.getParameterTypes(), parameterTypes)) {
                return method;
            }
        }

        // 尝试从父类/接口查找
        Class<?> superClass = targetClass.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            Method method = findMatchingMethod(superClass, sourceMethod);
            if (method != null) {
                return method;
            }
        }

        return null;
    }

    /**
     * 判断参数类型是否匹配
     */
    private boolean isParameterTypesMatch(Class<?>[] targetTypes, Class<?>[] sourceTypes) {
        if (targetTypes.length != sourceTypes.length) {
            return false;
        }

        for (int i = 0; i < targetTypes.length; i++) {
            if (!isTypeCompatible(sourceTypes[i], targetTypes[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * 判断类型是否兼容
     */
    private boolean isTypeCompatible(Class<?> sourceType, Class<?> targetType) {
        if (sourceType.equals(targetType)) {
            return true;
        }

        if (isPrimitiveWrapperPair(sourceType, targetType)) {
            return true;
        }

        return targetType.isAssignableFrom(sourceType);
    }

    /**
     * 判断是否是基本类型和包装类型对
     */
    private boolean isPrimitiveWrapperPair(Class<?> type1, Class<?> type2) {
        return (type1 == int.class && type2 == Integer.class)
            || (type1 == Integer.class && type2 == int.class)
            || (type1 == long.class && type2 == Long.class)
            || (type1 == Long.class && type2 == long.class)
            || (type1 == double.class && type2 == Double.class)
            || (type1 == Double.class && type2 == double.class)
            || (type1 == float.class && type2 == Float.class)
            || (type1 == Float.class && type2 == float.class)
            || (type1 == boolean.class && type2 == Boolean.class)
            || (type1 == Boolean.class && type2 == boolean.class)
            || (type1 == char.class && type2 == Character.class)
            || (type1 == Character.class && type2 == char.class)
            || (type1 == byte.class && type2 == Byte.class)
            || (type1 == Byte.class && type2 == byte.class)
            || (type1 == short.class && type2 == Short.class)
            || (type1 == Short.class && type2 == short.class);
    }
}
