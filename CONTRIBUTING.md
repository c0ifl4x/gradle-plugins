# Contributing to Gradle plugins

Thank you for your interest in contributing to this project. Please follow the guidelines below to keep the codebase
clean and consistent.

## Development Setup

- Java 21 or newer is required to build and test the plugins.
- Use the included Gradle Wrapper (`./gradlew` on Unix, `gradlew.bat` on Windows).

## Building, testing & publishing

### Run tests

Execute all unit and functional tests across all plugin modules:

```bash
./gradlew check
```

### Publish to local Maven repository

To test plugin artifacts locally in another Gradle project before releasing:

```bash
./gradlew publishToMavenLocal
```

### Publish to Reposilite

Versions ending with `-SNAPSHOT` are automatically routed to `/snapshots`, while release versions are published to
`/releases`:

```bash
REPOSILITE_USER=<user> REPOSILITE_TOKEN=<token> ./gradlew publish
```

## Adding a new plugin

1. Create a new directory for the plugin module in the repository root (e.g., `my-new-plugin/`).
2. Register the module in `settings.gradle.kts`.
3. Configure ```plugins { `kotlin-dsl` `maven-publish` }```, the module `version`, and the `gradlePlugin { ... }` block
   inside the module's `build.gradle.kts`.
4. Avoid using internal Gradle APIs (`org.gradle.internal.*`) and ensure all tasks remain compatible with Gradle
   configuration cache.

## Coding & testing standards

- Every plugin must include unit tests (`ProjectBuilder`) and functional tests (`GradleRunner` with `@TempDir`).
- Organize test methods using `// given`, `// when`, and `// then` comments written in lowercase English without
  trailing periods.
- Run `./gradlew check` and ensure all tests pass without deprecation warnings before submitting a pull request.

## Pull Request Process

1. Fork the repository and create a feature branch from `master`.
2. Commit your changes with clear, descriptive messages.
3. Open a Pull Request describing the motivation and changes introduced.

## License

By contributing to this repository, you agree that your contributions will be licensed under the **Apache License 2.0**.
