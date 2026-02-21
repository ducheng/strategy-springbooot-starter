@echo off
REM Maven 便捷启动脚本
REM 使用方法: mvn.bat [maven参数]

set JAVA_HOME=C:\Program Files (x86)\Java\jdk-1.8
set MAVEN_HOME=D:\Tools\apache-maven-3.9.12
set PATH=%MAVEN_HOME%\bin;%JAVA_HOME%\bin;%PATH%

D:\Tools\apache-maven-3.9.12\bin\mvn.cmd %*
