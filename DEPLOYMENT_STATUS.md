# Maven Central 发布状态（新版 Portal）

## ✅ 已完成的配置

### 1. GPG 密钥
- ✅ 密钥对已生成
- ✅ 公钥已分发到 keys.openpgp.org
- ✅ 私钥已备份到 `gpg-private-key-backup.asc`

**密钥信息**：
- 密钥 ID: `C430ABDBB3AC12C2`
- 用户: ducheng <ducheng@users.noreply.github.com>
- 类型: RSA 4096位

### 2. Maven 配置
- ✅ `pom.xml` 已更新为 **新版 Central Portal** 格式
- ✅ `~/.m2/settings.xml` 已添加 OSSRH 配置
- ✅ Group ID: `io.github.ducheng`
- ✅ 部署地址: `https://central.sonatype.com/upload/maven2/`

### 3. settings.xml 配置

```xml
<server>
    <id>ossrh</id>
    <username>token</username>
    <password>在这里填写你的 User Token</password>
</server>
```

**重要**：
- `<username>` 必须是字符串 `token`
- `<password>` 需要从新版 Portal 获取

---

## ⏳ 需要手动完成的步骤

### 步骤 1：注册 Central Portal

访问：https://central.sonatype.com/

使用 GitHub 账号登录（推荐）

### 步骤 2：创建 Namespace

1. 点击左侧菜单 **Namespaces**
2. 点击 **Add Namespace**
3. 输入：`io.github.ducheng`
4. 选择 **GitHub** 验证方式
5. 授权 Portal 访问你的 GitHub
6. 验证通过后，状态变为 **Verified** ✅

### 步骤 3：生成 User Token

1. 访问：https://central.sonatype.com/publish/tokens
2. 点击 **Generate User Token**
3. **立即复制 Token**（只显示一次！）
4. 编辑 `C:\Users\Administrator\.m2\settings.xml`
5. 将 Token 粘贴到 `<password>` 字段

### 步骤 4：部署

```bash
cd D:/codework/strategy-springbooot-starter
mvn clean deploy -P release
```

### 步骤 5：在 Portal 发布

1. 访问：https://central.sonatype.com/publish/deployments
2. 找到你的部署记录
3. 等待状态变为 **VALIDATED**（1-5 分钟）
4. 点击 **Publish** 按钮

---

## 🚀 新版 vs 旧版对比

| 项目 | 旧版（已弃用） | 新版（Current） |
|------|--------------|---------------|
| Portal | oss.sonatype.org | central.sonatype.com |
| 注册方式 | Jira Ticket | GitHub/Google 登录 |
| 认证方式 | Jira 用户名密码 | User Token |
| 部署地址 | s01.oss.sonatype.org | central.sonatype.com |
| 审核时间 | 1-2 个工作日 | 即时验证 |

---

## 📋 新版发布流程

```
1. 注册 Central Portal (central.sonatype.com)
   └─ 使用 GitHub 账号登录

2. 创建 Namespace
   └─ Group ID: io.github.ducheng
   └─ GitHub 验证（自动通过）

3. 生成 User Token
   └─ 访问 /publish/tokens
   └─ 复制 Token 到 settings.xml

4. 部署
   └─ mvn clean deploy -P release

5. 发布到 Maven Central
   └─ 访问 /publish/deployments
   └─ 点击 Publish 按钮
```

---

## 🔍 验证发布

### 1. 检查 Portal 状态

访问：https://central.sonatype.com/publish/deployments

状态应该是：**PUBLISHED** ✅

### 2. 检查 Maven Central

等待 10-30 分钟后：

```bash
curl https://repo1.maven.org/maven2/io/github/ducheng/dynamic-route-spring-boot-starter/
```

或访问：https://mvnrepository.com/artifact/io.github.ducheng/dynamic-route-spring-boot-starter

### 3. 在项目中测试

```xml
<dependency>
    <groupId>io.github.ducheng</groupId>
    <artifactId>dynamic-route-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 📁 项目文件

```
D:/codework/strategy-springbooot-starter/
├── pom.xml                          # ✅ 已更新为新版格式
├── MAVEN_CENTRAL_PORTAL_GUIDE.md    # 📘 新版完整指南
├── QUICK_START.md                   # 🚀 快速开始
├── gpg-key-info.txt                 # 🔑 GPG 密钥信息
├── gpg-private-key-backup.asc       # 💾 GPG 私钥备份
├── deploy-snapshot.sh               # 📦 SNAPSHOT 部署脚本
└── deploy-maven-central.sh          # 📦 RELEASE 部署脚本
```

---

## ⚠️ 重要提示

### 1. User Token 安全

- Token 只在生成时显示一次
- 请立即复制并保存到 settings.xml
- 如果泄露，访问 /publish/tokens 撤销并重新生成

### 2. GPG 私钥备份

备份文件：`gpg-private-key-backup.asc`

请妥善保管，不要上传到 Git！

### 3. settings.xml 配置

确认配置正确：

```xml
<server>
    <id>ossrh</id>
    <username>token</username>  <!-- 固定值 -->
    <password>你的User Token</password>
</server>
```

---

## 🔗 常用链接

- **Central Portal**: https://central.sonatype.com/
- **Namespaces**: https://central.sonatype.com/publish/namespaces
- **User Tokens**: https://central.sonatype.com/publish/tokens
- **Deployments**: https://central.sonatype.com/publish/deployments
- **Search**: https://central.sonatype.com/publish/search
- **官方文档**: https://central.sonatype.org/publish/portal/

---

## 🆘 遇到问题？

### 部署失败

```bash
# 查看详细错误
mvn clean deploy -P release -X

# 常见问题：
# - 401: User Token 配置错误
# - 签名失败: GPG 配置问题
# - 验证失败: pom.xml 配置问题
```

### Token 失效

1. 访问 https://central.sonatype.com/publish/tokens
2. 撤销旧 Token
3. 生成新 Token
4. 更新 settings.xml

### 验证失败

检查 Portal 的 **Deployments** 页面，查看具体错误信息。

---

**更新时间**: 2026-02-21
**适用版本**: Maven Central Portal（新版）
**项目**: strategy-springbooot-starter
**Group ID**: io.github.ducheng
