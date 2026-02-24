---
title: Building Project
description: Running the build to generate validation code.
headline: Documentation
---

# Build your project

After you added Validation to the build and declared constraints in your Protobuf model,
run your Gradle build as usual.

For macOS and Linux:
```bash
./gradlew clean build
```

For Windows:
```powershell
gradlew.bat clean build
```

The Validation Gradle plugin integrates with Spine Compiler, scans your model, and injects
validation checks into the generated JVM code.

The generated sources will appear in the `generated` directory next to the `src` directory.

The `clean` task is optional and is relevant when Protobuf code changes.

## Next step

Continue with [Using the generated validation API](generated-code.md).
