To support the "Thinking" toggle, embeddings, and other custom features, this project requires some manual build steps for native components.

## Prerequisites

### 1. Vector Database Support
Before assembling the app, you must build the native libraries needed for storing embeddings:
```bash
./build_sqlite_vec.sh
```

## Key Files
- [build_sqlite_vec.sh](file:///home/syu/AndroidStudioProjects/charchat/build_sqlite_vec.sh): Script to build native SQLite vector support.
- [app/build.gradle.kts](file:///home/syu/AndroidStudioProjects/charchat/app/build.gradle.kts): Project build configuration.
- [local.properties](file:///home/syu/AndroidStudioProjects/charchat/local.properties): Stores local configuration.
