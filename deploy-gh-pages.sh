#!/bin/bash

# GitHub Pages 部署脚本
# 将 page 目录部署到 gh-pages 分支

set -e

echo "开始部署到 GitHub Pages..."

# 检查是否有未提交的更改
if [[ -n $(git status --porcelain) ]]; then
    echo "警告：存在未提交的更改，请先提交后再部署"
    git status
    exit 1
fi

# 部署 page 目录到 gh-pages 分支
echo "正在部署 /page 目录到 gh-pages 分支..."
git subtree push --prefix page origin gh-pages

echo "✅ 部署完成！"
echo ""
echo "接下来请在 GitHub 仓库中："
echo "1. 进入 Settings > Pages"
echo "2. Source 选择 gh-pages 分支，/ (root) 目录"
echo "3. 等待几分钟后访问你的 GitHub Pages 地址"
