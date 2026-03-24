@echo off
chcp 65001 > nul
echo ==========================================
echo iCookie Windows 真机测试脚本
echo ==========================================
echo.

REM 检查 Flutter 环境
flutter --version > nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: Flutter 未安装或未添加到 PATH
    exit /b 1
)

echo 📱 检查连接的设备...
flutter devices

echo.
echo ==========================================
echo 构建 Release APK...
echo ==========================================
flutter build apk --release

if errorlevel 1 (
    echo ❌ 构建失败
    exit /b 1
)

echo.
echo ✅ 构建成功!
echo APK 位置: build\app\outputs\flutter-apk\app-release.apk
echo.

REM 检查 adb
adb devices > nul 2>&1
if errorlevel 1 (
    echo ⚠️ 警告: adb 未找到，请手动安装 APK
    echo 请将 APK 复制到手机并安装
) else (
    echo 📲 尝试安装到设备...
    adb install -r build\app\outputs\flutter-apk\app-release.apk
    
    if errorlevel 1 (
        echo ⚠️ 自动安装失败，请手动安装
    ) else (
        echo ✅ 安装成功!
    )
)

echo.
echo ==========================================
echo 测试准备完成!
echo ==========================================
echo.
echo 请按照 docs\DEVICE_TEST_CHECKLIST.md 进行手动测试
echo.
pause
