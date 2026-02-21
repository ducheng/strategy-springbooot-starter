# 发布到 Maven 中央仓库完整指南

## 前置条件

1. 已有 GitHub 账号
2. 项目代码已推送到 GitHub

---

## 步骤 1：注册 Sonatype 并创建项目

### 1.1 注册 Jira 账号

访问：https://issues.sonatype.org/secure/Signup!default.jspa

### 1.2 创建新项目 Ticket

访问：https://issues.sonatype.org/secure/CreateIssue.jspa

**填写以下信息**：
- **Project**: Community Support - Open Source Project Repository Hosting (OSSRH)
- **Issue Type**: New Project
- **Summary**: strategy-springbooot-starter - Dynamic Route Spring Boot Starter
- **Group Id**: `io.github.ducheng`
- **Project URL**: https://github.com/ducheng/strategy-springbooot-starter
- **SCM URL**: https://github.com/ducheng/strategy-springbooot-starter.git
- **Username (for oss.sonatype.org)**: ducheng

**Group Id 选择建议**：
- 推荐使用 `io.github.你的用户名`（无需域名验证）
- 或者使用自己的域名（需要 DNS 验证）

### 1.3 等待审核

通常 1-2 个工作日，审核通过后 Ticket 状态变为 **RESOLVED**

审核人员会回复类似：
> Repository created. Your groupId io.github.ducheng is ready.

---

## 步骤 2：生成 GPG 密钥

### 2.1 生成密钥对

```bash
gpg --gen-key
```

**按提示输入**：
- Real name: `ducheng`
- Email address: 你的邮箱
- Passphrase: 设置一个密码（记住这个密码！）

### 2.2 查看密钥

```bash
gpg --list-keys
```

输出类似：
```
pub   rsa4096 2024-02-21 [SC]
      ABC123DEF456...
uid   [ultimate] ducheng <your-email@example.com>
sub   rsa4096 2024-02-21 [E]
```

记下 `ABC123DEF456...` 这个密钥 ID（前 8 位即可）

### 2.3 分发公钥

```bash
# 方法1：分发到 MIT keyserver（推荐）
gpg --keyserver keys.openpgp.org --send-keys 你的密钥ID

# 方法2：或分发到 Ubuntu keyserver
gpg --keyserver keyserver.ubuntu.com --send-keys 你的密钥ID
```

例如：
```bash
gpg --keyserver keys.openpgp.org --send-keys ABC123DE
```

### 2.4 验证公钥分发

```bash
gpg --keyserver keys.openpgp.org --recv-keys 你的密钥ID
```

---

## 步骤 3：配置 Maven

### 3.1 配置 settings.xml

编辑 `~/.m2/settings.xml`（Windows: `C:\Users\你的用户名\.m2\settings.xml`）：

```xml
<settings>
    <servers>
        <server>
            <id>ossrh</id>
            <username>你的 Sonatype Jira 用户名</username>
            <password>你的 Sonatype Jira 密码</password>
        </server>
    </servers>

    <profiles>
        <profile>
            <id>ossrh</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <gpg.executable>gpg</gpg.executable>
                <gpg.passphrase>你的GPG密码</gpg.passphrase>
            </properties>
        </profile>
    </profiles>
</settings>
```

**重要**：
- `ossrh` 的用户名和密码是 Sonatype Jira 账号的登录凭证
- `gpg.passphrase` 是生成 GPG 密钥时设置的密码

---

## 步骤 4：部署到 Maven Central

### 4.1 先部署 SNAPSHOT 版本（测试）

```bash
cd D:/codework/strategy-springbooot-starter

# 修改版本为 SNAPSHOT
# 编辑 pom.xml，将 <version>1.0.0</version> 改为 <version>1.0.1-SNAPSHOT</version>

# 部署
mvn clean deploy
```

SNAPSHOT 会部署到：
```
https://s01.oss.sonatype.org/content/repositories/snapshots/io/github/ducheng/dynamic-route-spring-boot-starter/
```

### 4.2 部署 RELEASE 版本

```bash
# 确保版本号不含 -SNAPSHOT
# <version>1.0.1</version>

# 使用 release profile 进行部署
mvn clean deploy -P release
```

这会：
1. 编译项目
2. 生成源码 jar 和 javadoc jar
3. 使用 GPG 签名所有文件
4. 部署到 OSSRH staging 仓库
5. 自动 release 到 Maven Central

---

## 步骤 5：验证和同步

### 5.1 检查 Staging 仓库

访问：https://s01.oss.sonatype.org/

使用你的 Jira 账号登录，在 **Staging Repositories** 中查看你的构件。

如果 `autoReleaseAfterClose=true`，会自动 release。

### 5.2 等待同步到 Maven Central

成功 release 后，需要等待 **10 分钟 - 2 小时** 同步到 Maven Central。

### 5.3 验证发布

访问以下地址验证：
```
https://repo1.maven.org/maven2/io/github/ducheng/dynamic-route-spring-boot-starter/
```

或搜索：
```
https://mvnrepository.com/artifact/io.github.ducheng/dynamic-route-spring-boot-starter
```

---

## 步骤 6：在项目中使用

发布成功后，其他开发者可以这样使用：

```xml
<dependency>
    <groupId>io.github.ducheng</groupId>
    <artifactId>dynamic-route-spring-boot-starter</artifactId>
    <version>1.0.1</version>
</dependency>
```

---

## 常见问题

### Q1: GPG 签名失败

**错误**：`gpg: signing failed: Inappropriate ioctl for device`

**解决**：
```bash
# 在 Maven 命令中添加
mvn clean deploy -P release -Dgpg.passphrase=你的密码
```

或在 `settings.xml` 中添加：
```xml
<gpg.executable>gpg</gpg.executable>
<gpg.passphrase>你的密码</gpg.passphrase>
```

### Q2: 401 Unauthorized

**原因**：用户名或密码错误

**解决**：
- 检查 `settings.xml` 中的 `ossrh` 配置
- 确认使用的是 Sonatype Jira 账号，不是 GitHub 账号

### Q3: 构件未通过验证

**原因**：缺少 Javadoc 或源码 jar

**解决**：
- 确保 `pom.xml` 包含 `maven-source-plugin` 和 `maven-javadoc-plugin`
- 运行 `mvn clean deploy` 重新部署

### Q4: 同步到 Maven Central 很慢

**正常情况**：首次发布需要 2-4 小时，后续发布 10-30 分钟

**查询状态**：
```bash
curl https://repo1.maven.org/maven2/io/github/ducheng/dynamic-route-spring-boot-starter/1.0.1/
```

---

## 快速命令参考

```bash
# 1. 生成 GPG 密钥
gpg --gen-key
gpg --list-keys
gpg --keyserver keys.openpgp.org --send-keys 密钥ID

# 2. 部署 SNAPSHOT（测试）
mvn clean deploy

# 3. 部署 RELEASE（正式）
mvn clean deploy -P release

# 4. 验证 GPG 签名
gpg --verify target/*.asc

# 5. 查看已部署的构件
# 访问 https://s01.oss.sonatype.org/
```

---

## 发布新版本

```bash
# 1. 更新版本号
vim pom.xml  # 修改 <version>1.0.2</version>

# 2. 提交代码
git add .
git commit -m "Release 1.0.2"
git tag v1.0.2
git push && git push --tags

# 3. 部署
mvn clean deploy -P release
```

---

## 参考资料

- [Sonatype OSSRH Guide](https://central.sonatype.org/publish/publish-guide/)
- [Maven Central Repository](https://mvnrepository.com/)
- [Apache Maven Deploy Plugin](https://maven.apache.org/plugins/maven-deploy-plugin/)
