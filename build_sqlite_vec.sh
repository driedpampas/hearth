set -e

# Check for ANDROID_NDK_HOME
if [ -z "$ANDROID_NDK_HOME" ]; then
    echo "Error: ANDROID_NDK_HOME is not set. Please set it to the path of your Android NDK."
    exit 1
fi

echo "Using NDK at: $ANDROID_NDK_HOME"

# Set API Level and build paths
API_LEVEL=24
SQLITE_VEC_VERSION="v0.1.9"
DOWNLOAD_DIR="sqlite_vec_src"
OUTPUT_DIR="app/src/main/jniLibs"
HOST_TAG="linux-x86_64" # Assuming linux host

# Toolchain paths
TOOLCHAIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/$HOST_TAG"
SYSROOT="$TOOLCHAIN/sysroot"

# Create directories
mkdir -p "$DOWNLOAD_DIR"

# Download sqlite-vec amalgamation
echo "Downloading sqlite-vec amalgamation (version $SQLITE_VEC_VERSION)..."
curl -L -s -o "$DOWNLOAD_DIR/sqlite-vec.zip" "https://github.com/asg017/sqlite-vec/releases/download/$SQLITE_VEC_VERSION/sqlite-vec-${SQLITE_VEC_VERSION#v}-amalgamation.zip"
unzip -q -j "$DOWNLOAD_DIR/sqlite-vec.zip" -d "$DOWNLOAD_DIR"

# Download SQLite amalgamation
echo "Downloading SQLite amalgamation..."
curl -L -s -o "$DOWNLOAD_DIR/sqlite.zip" "https://www.sqlite.org/2024/sqlite-amalgamation-3460000.zip"
unzip -q -j "$DOWNLOAD_DIR/sqlite.zip" -d "$DOWNLOAD_DIR"

# Architectures to build
ARCHS=("arm64-v8a" "armeabi-v7a" "x86" "x86_64")

# Map ABI to Clang target
declare -A TARGETS=(
    ["arm64-v8a"]="aarch64-linux-android"
    ["armeabi-v7a"]="armv7a-linux-androideabi"
    ["x86"]="i686-linux-android"
    ["x86_64"]="x86_64-linux-android"
)

# Build loop
for ABI in "${ARCHS[@]}"; do
    echo "Building for $ABI..."
    
    TARGET="${TARGETS[$ABI]}"
    CC="$TOOLCHAIN/bin/${TARGET}${API_LEVEL}-clang"
    
    # Base CFLAGS
    CFLAGS="-O3 -fPIC -shared --sysroot=$SYSROOT"
    
    # Add NEON support for ARM64 only (aarch64 required for some intrinsics)
    if [ "$ABI" == "arm64-v8a" ]; then
        CFLAGS="$CFLAGS -DSQLITE_VEC_ENABLE_NEON"
    fi
    
    # Create target ABI directory
    mkdir -p "$OUTPUT_DIR/$ABI"
    
    # Compile
    $CC $CFLAGS -o "$OUTPUT_DIR/$ABI/libsqlite_vec.so" "$DOWNLOAD_DIR/sqlite-vec.c"
    
    echo "Successfully built $OUTPUT_DIR/$ABI/libsqlite_vec.so"
done

# Cleanup
echo "Cleaning up source files..."
rm -rf "$DOWNLOAD_DIR"

echo "Build complete. All libraries moved to $OUTPUT_DIR"
