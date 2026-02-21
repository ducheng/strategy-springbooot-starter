# 🚀 立即部署到 Maven Central

## ✅ 所有配置已完成！

- ✅ GPG 密钥已生成
- ✅ User Token 已配置
- ✅ pom.xml 已更新
- ✅ settings.xml 已配置

---

## 📦 开始部署

### 方式 1：使用部署脚本（推荐）

双击运行：
```
deploy.bat
```

### 方式 2：使用命令行

```bash
cd D:/codework/strategy-springbooot-starter
mvn clean deploy -P release -DskipTests
```

### 方式 3：使用 Git Bash

```bash
cd /d/codework/strategy-springbooot-starter
mvn clean deploy -P release -DskipTests
```

---

## 📋 部署后的步骤

### 1. 访问 Deployments 页面

https://central.sonatype.com/publish/deployments

### 2. 等待验证

- 状态会从 **UPLOADING** → **VALIDATING** → **VALIDATED**
- 通常需要 1-5 分钟

### 3. 点击 Publish

当状态变为 **VALIDATED** 后，点击 **Publish** 按钮

### 4. 等待同步到 Maven Central

- 通常需要 10-30 分钟
- 可以在这里搜索：https://central.sonatype.com/publish/search

---

## ✅ 验证发布成功

### 检查 1：Portal 状态

访问 Deployments 页面，状态应该是 **PUBLISHED** ✅

### 检查 2：Maven Central

```bash
curl https://repo1.maven.org/maven2/io/github/ducheng/dynamic-route-spring-boot-starter/
```

或访问：
https://mvnrepository.com/artifact/io.github.ducheng/dynamic-route-spring-boot-starter

### 检查 3：在项目中使用

```xml
<dependency>
    <groupId>io.github.ducheng</groupId>
    <artifactId>dynamic-route-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 🔍 常见问题

### Q: Maven 命令未找到

**A**: 确保已安装 Maven 并配置到 PATH

下载：https://maven.apache.org/download.cgi

配置环境变量：
```
MAVEN_HOME=C:\path\to\maven
PATH=%PATH%;%MAVEN_HOME%\bin
```

### Q: 部署失败 401

**A**: User Token 配置错误

检查 `C:\Users\Administrator\.m2\settings.xml`：
```xml
<server>
    <id>ossrh</id>
    <username>9GgUm0</username>
    <password>5sQ2t6Uyfp0KZ4JKvQh4ASU0wbk2WvAOa</password>
</server>
```

### Q: GPG 签名失败

**A**: 确保 GPG 已安装

```bash
gpg --version
gpg --list-keys
```

---

## 📊 当前配置信息

```
Group ID:        io.github.ducheng
Artifact ID:     dynamic-route-spring-boot-starter
Version:         1.0.0
GPG Key ID:      C430ABDBB3AC12C2
User Token:      9GgUm0 / 5sQ2t6Uyfp0KZ4JKvQh4ASU0wbk2WvAOa
```

---

## 🔗 快速链接

- **Central Portal**: https://central.sonatype.com/
- **Deployments**: https://central.sonatype.com/publish/deployments
- **Namespaces**: https://central.sonatype.com/publish/namespaces
- **User Tokens**: https://central.sonatype.com/publish/tokens

---

**准备好了吗？双击 `deploy.bat` 开始部署！** 🚀
