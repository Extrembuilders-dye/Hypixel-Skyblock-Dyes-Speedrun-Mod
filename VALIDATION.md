# Validation status

Static project validation completed in the build workspace:

- 13 Java source files present.
- `fabric.mod.json` and mixin JSON parse successfully.
- Exactly 16 distinct vanilla Dye item constants are tracked.
- Minecraft target: 26.2.
- Fabric Loader target: 0.19.3.
- Fabric Loom target: 1.17-SNAPSHOT.
- Fabric API target: 0.160.0+26.2.
- Mod/API protocol version: 1.0.0, matching Dye Community Bot V8 defaults.
- API paths for linking, pending run retrieval, start, state, finish and abort are implemented.
- No Discord bot token is embedded in the project.

The workspace currently only provides Java 21 and no Gradle installation, while Minecraft 26.2 development targets Java 25. Therefore an actual Loom compilation was not performed here. The included GitHub Actions workflow performs the real Java-25/Gradle-9.5.1 build and should be used before distributing the mod JAR.
