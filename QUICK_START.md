# Maven Central 发布快速参考

## ✅ 已完成

- ✅ GPG 密钥生成（ID: C430ABDBB3AC12C2）
- ✅ 公钥分发到 keys.openpgp.org
- ✅ pom.xml 更新为新版 Portal 格式
- ✅ settings.xml 添加 OSSRH 配置

---

## 🚀 接下来 5 步发布到 Maven Central

### 1️⃣ 注册并登录

访问：https://central.sonatype.com/

使用 GitHub 账号登录

### 2️⃣ 创建 Namespace

1. 点击 **Namespaces** → **Add Namespace**
2. 输入：`io.github.ducheng`
3. 选择 **GitHub** 验证
4. 点击 **Verify**

### 3️⃣ 生成 User Token

1. 点击 **Tokens** → **Generate User Token**
2. 复制 Token（格式：`U_xxx...`）
3. 编辑 `C:\Users\Administrator\.m2\settings.xml`：

```xml
<server>
    <id>ossrh</id>
    <username>token</username>
    <password>粘贴User Token</password>
</server>
```

### 4️⃣ 部署

```bash
cd D:/codework/strategy-springbooot-starter
mvn clean deploy -P release
```

### 5️⃣ 发布

1. 访问 https://central.sonatype.com/publish/deployments
2. 等待状态变为 **VALIDATED**
3. 点击 **Publish** 按钮
4. 等待 10-30 分钟同步到 Maven Central

---

## 📋 验证发布成功

访问以下地址验证：

```bash
# Maven Central
https://repo1.maven.org/maven2/io/github/ducheng/dynamic-route-spring-boot-starter/

# 或搜索
https://mvnrepository.com/search?q=io.github.ducheng
```

---

## 📁 项目信息

```
Group ID:        io.github.ducheng
Artifact ID:     dynamic-route-spring-boot-starter
Version:         1.0.0
GPG Key ID:      C430ABDBB3AC12C2
```

---

## 🔗 重要链接

- **Central Portal**: https://central.sonatype.com/
- **User Tokens**: https://central.sonatype.com/publish/tokens
- **Deployments**: https://central.sonatype.com/publish/deployments
- **Search**: https://central.sonatype.com/publish/search

---

## 📚 完整文档

详细指南：`MAVEN_CENTRAL_PORTAL_GUIDE.md`
