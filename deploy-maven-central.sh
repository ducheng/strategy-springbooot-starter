#!/bin/bash

# Maven Central 部署脚本
# 使用前请确保已完成以下步骤：
# 1. 注册 Sonatype Jira 账号
# 2. 创建项目 Ticket 并等待审核通过
# 3. 生成 GPG 密钥并分发公钥
# 4. 配置 ~/.m2/settings.xml

set -e

echo "=========================================="
echo "  Maven Central 部署助手"
echo "=========================================="
echo ""

# 检查当前版本
CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "当前版本: $CURRENT_VERSION"

if [[ $CURRENT_VERSION == *"-SNAPSHOT" ]]; then
    echo ""
    echo "检测到 SNAPSHOT 版本，将部署到 SNAPSHOT 仓库"
    echo ""
    read -p "是否继续部署 SNAPSHOT? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "取消部署"
        exit 1
    fi

    echo ""
    echo "开始部署 SNAPSHOT..."
    mvn clean deploy
else
    echo ""
    echo "检测到 RELEASE 版本，将部署到 Maven Central"
    echo ""
    read -p "是否继续部署 RELEASE? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "取消部署"
        exit 1
    fi

    echo ""
    echo "开始部署 RELEASE（包含 GPG 签名）..."
    mvn clean deploy -P release

    echo ""
    echo "=========================================="
    echo "  部署完成！"
    echo "=========================================="
    echo ""
    echo "后续步骤："
    echo "1. 访问 https://s01.oss.sonatype.org/ 查看状态"
    echo "2. 等待 10 分钟 - 2 小时同步到 Maven Central"
    echo "3. 验证发布："
    echo "   https://repo1.maven.org/maven2/io/github/ducheng/dynamic-route-spring-boot-starter/"
    echo ""
fi
