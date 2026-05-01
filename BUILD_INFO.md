To support the "Thinking" toggle, embeddings, and other custom features, this project requires some manual build steps for native components.

## Prerequisites

### 1. Vector Database Support
Before assembling the app, you must build the native libraries needed for storing embeddings:
```bash
./build_sqlite_vec.sh
```

## Patched LiteRT-LM (Thinking Toggle)

Since the full build scripts for the library are not public, we use a patching script to splice our local Kotlin changes into the official Google Maven AAR.

1.  **Clone the library**:
    ```bash
    git clone https://github.com/driedpampas/LiteRT-LM
    ```
2.  **Run the patch script**:
    Navigate to the library root and run:
    ```bash
    ./patch_aar.sh 0.10.2
    ```
    This will generate `litert-lm-custom.aar` in the `dist/` subdirectory of the library root.

## Project Linking

To use the patched library in this project:

1.  **Configure `local.properties`**:
    Add the `litertlm.dir` key pointing to your local clone of the library:
    ```properties
    litertlm.dir=/path/to/your/LiteRT-LM
    ```
2.  **Build the project**:
    The `app/build.gradle.kts` is configured to automatically pick up `dist/litert-lm-custom.aar` if the directory is specified and the file exists. If not, it falls back to the official Maven version.

## Key Files
- [patch_aar.sh](file:///home/syu/Documents/GitHub/LiteRT-LM/patch_aar.sh): The script that performs the AAR patching.
- [app/build.gradle.kts](file:///home/syu/AndroidStudioProjects/charchat/app/build.gradle.kts): Contains the conditional logic to load the custom AAR.
- [local.properties](file:///home/syu/AndroidStudioProjects/charchat/local.properties): Stores the path to the local library.
