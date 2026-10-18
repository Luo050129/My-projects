@echo off
chcp 65001 >nul
echo ============================================
echo   君山点评 (junshan-dianping) 一键启动脚本
echo ============================================
echo [1/3] 检查 Redis (6380) ...
netstat -an | findstr ":6380" | findstr "LISTENING" >nul
if %errorlevel%==0 (
  echo       Redis 已在运行，跳过
) else (
  echo       启动便携版 Redis 8.10.1 (6380)
  start "redis8" "C:\Users\28025\.workbuddy\redis8\Redis-8.10.1-Windows-x64-msys2\redis-server.exe" --port 6380 --save "" --appendonly no
  timeout /t 2 /nobreak >nul
)

echo [2/3] 检查 MySQL (3306) ...
netstat -an | findstr ":3306" | findstr "LISTENING" >nul
if %errorlevel%==0 (
  echo       MySQL 已在运行
) else (
  echo       [警告] MySQL 未运行，请先启动 MySQL80 服务
)

echo [3/3] 启动 Spring Boot (8081) ...
echo       提示：若店铺查询提示"店铺不存在"，请先执行 tools\preheat_redis.py 预热缓存
cd /d "%~dp0"
"C:\Program Files\Java\jre1.8.0_503\bin\java.exe" -jar target\junshan-dianping-0.0.1-SNAPSHOT.jar --server.port=8081

pause
