# 新版 Maven Central Portal 发布指南（2024+）

## 新版 vs 旧版对比

| 项目 | 旧版（已弃用） | 新版（Current） |
|------|--------------|---------------|
| **Portal 地址** | https://oss.sonatype.org/ | https://central.sonatype.com/ |
| **创建项目** | Jira Ticket | 直接在 Portal 创建 |
| **认证方式** | Jira 用户名密码 | User Token |
| **部署地址** | s01.oss.sonatype.org | central.sonatype.com |
| **审核时间** | 1-2 个工作日 | 即时或几分钟 |
| **发布方式** | Staging 仓库 | 直接上传 |

---

## 步骤 1：注册并登录 Central Portal

### 1.1 访问新 Portal

访问：https://central.sonatype.com/

### 1.2 登录

使用以下方式之一登录：
- GitHub 账号（推荐）
- Google 账号
- 其他支持的单点登录

**注意**：不再需要单独注册 Jira 账号！

---

## 步骤 2：创建 Namespace（命名空间）

### 2.1 添加 Namespace

1. 登录后，点击左侧菜单 **Namespaces**
2. 点击 **Add Namespace**
3. 输入你的 Group ID：`io.github.ducheng`
4. 点击 **Verify**

### 2.2 验证所有权

选择验证方式：

#### 方式 A：GitHub 验证（推荐）

如果 Group ID 是 `io.github.你的用户名`：

1. 选择 **GitHub** 验证
2. 授权 Central Portal 访问你的 GitHub
3. 自动验证通过 ✅

#### 方式 B：DNS 验证

如果使用自己的域名：

1. 添加 TXT 记录到你的 DNS
2. 等待 DNS 传播
3. 点击验证

### 2.3 创建完成

验证成功后，Namespace 状态变为 **Verified** ✅

---

## 步骤 3：生成 User Token

### 3.1 访问 Tokens 页面

1. 点击左侧菜单 **Tokens**
2. 或者直接访问：https://central.sonatype.com/publish/tokens

### 3.2 生成新 Token

1. 点击 **Generate User Token**
2. 输入 Token 名称（可选）：`ducheng-token`
3. 点击 **Generate**
4. **立即复制 Token！**（只显示一次）

Token 格式类似：
```
U_${Base64字符串}
```

例如：
```
U_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### 3.3 配置到 Maven settings.xml

编辑 `C:\Users\Administrator\.m2\settings.xml`：

```xml
<server>
    <id>ossrh</id>
    <username>token</username>
    <password>粘贴你的User Token</password>
</server>
```

**重要**：
- `<username>` 必须是字符串 `token`（固定值）
- `<password>` 是你刚生成的 User Token

---

## 步骤 4：部署到 Maven Central

### 4.1 确认 pom.xml 配置

新版 pom.xml 关键配置：

```xml
<distributionManagement>
    <repository>
        <id>ossrh</id>
        <url>https://central.sonatype.com/upload/maven2/</url>
    </repository>
</distributionManagement>
```

### 4.2 部署命令

```bash
cd D:/codework/strategy-springbooot-starter

# 部署（包含 GPG 签名）
mvn clean deploy -P release
```

或者使用辅助脚本：

```bash
bash deploy-maven-central.sh
```

### 4.3 等待验证

部署成功后：

1. 访问 https://central.sonatype.com/publish/deployments
2. 找到你的部署记录
3. 等待状态变为 **VALIDATED**（通常 1-5 分钟）
4. 点击 **Publish** 按钮

### 4.4 搜索已发布的组件

访问 https://central.sonatype.com/publish/search，搜索：
```
io.github.ducheng
```

---

## 步骤 5：验证发布成功

### 5.1 检查 Maven Central

等待 10-30 分钟后访问：

```
https://repo1.maven.org/maven2/io/github/ducheng/dynamic-route-spring-boot-starter/
```

或搜索：
```
https://mvnrepository.com/artifact/io.github.ducheng/dynamic-route-spring-boot-starter
```

### 5.2 在项目中使用

```xml
<dependency>
    <groupId>io.github.ducheng</groupId>
    <artifactId>dynamic-route-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 常见问题

### Q1: User Token 泄露了怎么办？

**解决**：
1. 访问 https://central.sonatype.com/publish/tokens
2. 找到泄露的 Token
3. 点击 **Revoke** 撤销
4. 生成新的 Token 并更新 settings.xml

### Q2: 部署时返回 401 Unauthorized

**原因**：Token 配置错误

**检查**：
```xml
<!-- 确认 username 是 "token" -->
<username>token</username>

<!-- 确认 password 是完整的 User Token -->
<password>U_xxxxxxxxxxxxxxx</password>
```

### Q3: 部署成功但找不到组件

**原因**：可能需要手动发布

**解决**：
1. 访问 https://central.sonatype.com/publish/deployments
2. 找到你的部署
3. 如果状态是 **VALIDATED**，点击 **Publish**
4. 如果状态是 **FAILED**，查看错误信息并修复

### Q4: 验证失败

**常见错误**：

| 错误 | 原因 | 解决方案 |
|------|------|---------|
| Missing Signature | 没有 GPG 签名 | 使用 `-P release` 参数 |
| Invalid Javadoc | Javadoc 错误 | 添加 `<doclint>none</doclint>` |
| Missing POM | POM 文件缺失 | 检查 pom.xml 配置 |

---

## 完整流程总结

```
1. 注册 Central Portal (central.sonatype.com)
   └─ 使用 GitHub 账号登录

2. 创建 Namespace
   └─ Group ID: io.github.ducheng
   └─ 使用 GitHub 验证

3. 生成 User Token
   └─ 访问 /publish/tokens
   └─ 复制 Token 到 settings.xml

4. 部署
   └─ mvn clean deploy -P release

5. 发布
   └─ 访问 /publish/deployments
   └─ 点击 Publish 按钮

6. 验证
   └─ 等待 10-30 分钟
   └─ 搜索 mvnrepository.com
```

---

## 快速命令参考

```bash
# 1. 部署
mvn clean deploy -P release

# 2. 跳过测试（加快部署）
mvn clean deploy -DskipTests -P release

# 3. 只部署不签名（测试用）
mvn clean deploy

# 4. 验证 GPG 密钥
gpg --list-keys

# 5. 检查已部署的组件
curl https://repo1.maven.org/maven2/io/github/ducheng/dynamic-route-spring-boot-starter/
```

---

## 参考链接

- **Central Portal**: https://central.sonatype.com/
- **官方文档**: https://central.sonatype.org/publish/portal/
- **发布指南**: https://central.sonatype.org/publish/publish-guide/
- **User Token**: https://central.sonatype.com/publish/tokens
- **部署状态**: https://central.sonatype.com/publish/deployments

---

## 当前项目信息

```
Group ID: io.github.ducheng
Artifact ID: dynamic-route-spring-boot-starter
Version: 1.0.0
GPG Key ID: C430ABDBB3AC12C2
```

---

**更新时间**: 2026-02-21
**适用版本**: Maven Central Portal (新版)
