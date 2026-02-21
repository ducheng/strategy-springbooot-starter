@echo off
REM Maven Central 部署脚本（Windows）

echo ==========================================
echo   部署到 Maven Central
echo ==========================================
echo.

cd /d "%~dp0"

echo 当前目录: %CD%
echo.

echo 检查 Maven 是否安装...
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Maven 命令
    echo 请确保 Maven 已安装并配置到 PATH 环境变量
    echo.
    echo 或者直接运行以下命令：
    echo   mvn clean deploy -P release
    echo.
    pause
    exit /b 1
)

echo 开始部署...
echo.

mvn clean deploy -P release -DskipTests

echo.
echo ==========================================
if %errorlevel% equ 0 (
    echo   部署成功！
    echo.
    echo 后续步骤：
    echo 1. 访问 https://central.sonatype.com/publish/deployments
    echo 2. 等待状态变为 VALIDATED
    echo 3. 点击 Publish 按钮
) else (
    echo   部署失败！
    echo.
    echo 请检查错误信息并修复
)
echo ==========================================
echo.
pause
