#!/bin/bash
# build.sh - FongMi WebHome SDK 编译脚本
# 编译 Java -> class -> dex -> jar
# 输出: webhome.jar (可被 fongmi/catvod 壳 DexClassLoader 加载)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
ROOT_DIR="$SCRIPT_DIR"
SDK_DIR="$ROOT_DIR/sdk"
SRC_DIR="$SDK_DIR/src/main/java"
OUT_DIR="$SDK_DIR/build/classes"
DEX_DIR="$SDK_DIR/build/dex"
BUILD_DIR="$SDK_DIR/build"
ASSETS_SRC="$SDK_DIR/src/main/assets"

# Android SDK
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
if [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "ERROR: ANDROID_SDK_ROOT or ANDROID_HOME not set"
    exit 1
fi

# 优先用 android-34，备用 android-33
PLATFORM=""
BT=""
for ver in "android-34" "android-33" "android-30" "android-29"; do
    if [ -f "$ANDROID_SDK_ROOT/platforms/$ver/android.jar" ]; then
        PLATFORM="$ANDROID_SDK_ROOT/platforms/$ver/android.jar"
        break
    fi
done

# 选 build-tools
for ver in "34.0.0" "33.0.2" "32.0.0" "30.0.0"; do
    if [ -x "$ANDROID_SDK_ROOT/build-tools/$ver/d8" ]; then
        BT="$ANDROID_SDK_ROOT/build-tools/$ver"
        break
    fi
done

if [ -z "$PLATFORM" ]; then
    echo "ERROR: android.jar not found"
    echo "Run: sdkmanager \"platforms;android-34\""
    exit 1
fi

if [ -z "$BT" ]; then
    echo "ERROR: build-tools not found (no d8)"
    echo "Run: sdkmanager \"build-tools;34.0.0\""
    exit 1
fi

echo "Using PLATFORM=$PLATFORM"
echo "Using BT=$BT"
echo "Using SRC_DIR=$SRC_DIR"

echo ""
echo "=== 1. 编译 Java 源码 ==="
mkdir -p "$OUT_DIR" "$DEX_DIR" "$BUILD_DIR/libs"
rm -rf "$OUT_DIR"/* "$DEX_DIR"/* 2>/dev/null || true

find "$SRC_DIR" -name "*.java" | sort > "$BUILD_DIR/sources.txt"
echo "Source files:"
cat "$BUILD_DIR/sources.txt"

javac -cp "$PLATFORM" -d "$OUT_DIR" @"$BUILD_DIR/sources.txt"

echo ""
echo "=== 2. 删除 stub Spider.class (壳自带真正的 Spider) ==="
rm -f "$OUT_DIR/com/github/catvod/crawler/Spider.class"
rm -f "$OUT_DIR/com/github/catvod/crawler/Spider\$*.class"
echo "Removed Spider stub class(es)"

echo ""
echo "=== 3. 编译后的 class 文件 ==="
find "$OUT_DIR" -name "*.class" | sort > "$BUILD_DIR/classes.txt"
cat "$BUILD_DIR/classes.txt"

echo ""
echo "=== 4. 转换为 dex ==="
$BT/d8 --release --lib "$PLATFORM" --min-api 24 --output "$DEX_DIR" @"$BUILD_DIR/classes.txt"
ls -la "$DEX_DIR"

if [ ! -f "$DEX_DIR/classes.dex" ]; then
    echo "!!! ERROR: classes.dex not generated"
    exit 1
fi

echo ""
echo "=== 5. 打包 webhome.jar (含 dex) ==="
mkdir -p "$BUILD_DIR/META-INF"
printf "Manifest-Version: 1.0\nCreated-By: FongMi WebHome SDK Build 1.0\n" > "$BUILD_DIR/META-INF/MANIFEST.MF"

cd "$BUILD_DIR"
rm -f webhome.jar
jar cfm webhome.jar META-INF/MANIFEST.MF -C dex .
cd "$ROOT_DIR"

cp "$BUILD_DIR/webhome.jar" "$BUILD_DIR/libs/webhome.jar"

echo ""
echo "=== 6. 打包 webhome-assets.jar (fmsdk.js + demo html) ==="
if [ -d "$ASSETS_SRC" ] && [ -n "$(ls -A "$ASSETS_SRC" 2>/dev/null)" ]; then
    cd "$ASSETS_SRC"
    rm -f "$BUILD_DIR/libs/webhome-assets.jar"
    jar cf "$BUILD_DIR/libs/webhome-assets.jar" .
    cd "$ROOT_DIR"
    echo "Created webhome-assets.jar"
fi

echo ""
echo "=== 7. 验证 jar 内容 ==="
echo "--- webhome.jar ---"
unzip -l "$BUILD_DIR/libs/webhome.jar"
echo ""
echo "--- webhome-assets.jar (if exists) ---"
if [ -f "$BUILD_DIR/libs/webhome-assets.jar" ]; then
    unzip -l "$BUILD_DIR/libs/webhome-assets.jar"
fi

echo ""
echo "=== 8. 验证 dex 内容 ==="
$BT/dexdump "$DEX_DIR/classes.dex" 2>/dev/null | grep "Class descriptor" | head -30

echo ""
echo "=== 9. 输出 ==="
ls -la "$BUILD_DIR/libs/"
echo ""
echo "Build OK"
echo "  webhome.jar         -> $BUILD_DIR/libs/webhome.jar"
echo "  webhome-assets.jar  -> $BUILD_DIR/libs/webhome-assets.jar"
