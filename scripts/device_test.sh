#!/bin/bash
# iCookie 真机测试脚本
# 用法: ./device_test.sh [device_id]

echo "=========================================="
echo "iCookie 真机测试脚本"
echo "=========================================="
echo ""

# 检查 Flutter 环境
if ! command -v flutter &> /dev/null; then
    echo "❌ 错误: Flutter 未安装"
    exit 1
fi

# 检查设备连接
echo "📱 检查连接的设备..."
flutter devices

echo ""
echo "=========================================="
echo "构建 Release APK..."
echo "=========================================="
flutter build apk --release

if [ $? -ne 0 ]; then
    echo "❌ 构建失败"
    exit 1
fi

echo ""
echo "✅ 构建成功!"
echo "APK 位置: build/app/outputs/flutter-apk/app-release.apk"
echo ""

# 如果有设备 ID，尝试安装
if [ -n "$1" ]; then
    echo "📲 安装到设备 $1..."
    adb -s $1 install -r build/app/outputs/flutter-apk/app-release.apk
else
    echo "📲 尝试安装到默认设备..."
    adb install -r build/app/outputs/flutter-apk/app-release.apk
fi

echo ""
echo "=========================================="
echo "安装完成!"
echo "=========================================="
echo ""
echo "请按照 docs/DEVICE_TEST_CHECKLIST.md 进行手动测试"
