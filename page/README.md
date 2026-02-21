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

### 安装 Starter

```bash
# 克隆项目
git clone <repository-url>

# 安装到本地仓库
cd strategy-springbooot-starter
mvn clean install
```

### 在项目中使用

添加依赖到 `pom.xml`：

```xml
<dependency>
    <groupId>com.strategy</groupId>
    <artifactId>dynamic-route-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 应用场景

- **灰度发布**：逐步将流量从旧系统切换到新系统
- **多租户系统**：不同租户使用不同的实现
- **A/B 测试**：根据用户特征进行测试
- **功能开关**：动态控制新功能的启用

## 版本信息

- **Starter 版本**: 1.0.0
- **Spring Boot**: 2.6.15
- **JDK**: 1.8+
