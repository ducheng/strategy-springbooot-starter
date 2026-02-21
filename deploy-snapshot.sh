#!/bin/bash

# SNAPSHOT 版本部署脚本
# 用于测试部署到 OSSRH

set -e

echo "=========================================="
echo "  部署 SNAPSHOT 到 OSSRH"
echo "=========================================="
echo ""

# 检查当前版本
CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "当前版本: $CURRENT_VERSION"

if [[ $CURRENT_VERSION != *"-SNAPSHOT" ]]; then
    echo ""
    echo "⚠️  当前版本不是 SNAPSHOT"
    echo "请先修改 pom.xml，将版本改为例如：1.0.1-SNAPSHOT"
    echo ""
    exit 1
fi

echo ""
echo "开始部署..."
echo ""

mvn clean deploy

echo ""
echo "=========================================="
echo "  部署完成！"
echo "=========================================="
echo ""
echo "SNAPSHOT 仓库地址："
echo "https://s01.oss.sonatype.org/content/repositories/snapshots/io/github/ducheng/dynamic-route-spring-boot-starter/"
echo ""
echo "验证命令："
echo "curl https://s01.oss.sonatype.org/content/repositories/snapshots/io/github/ducheng/dynamic-route-spring-boot-starter/"
echo ""
