# Kotlin Language Server with Bazel Support

This is a fork of [kotlin-language-server](https://github.com/fwcd/kotlin-language-server) and [kotlin-debug-adapter](https://github.com/fwcd/kotlin-debug-adapter) with Bazel support added to it. It works in tandem with a [VSCode extension](https://github.com/smocherla-brex/bazel-kotlin-vscode-extension).


![Icon](Icon128.png)

Only VSCode currently is supported/tested, but it may be possible to support other editors with additional configuration.

## How this differs from the original

This fork only focuses on Bazel support and making sure it works reliably. As a result
- I had to remove any existing Gradle/Maven support as supporting all of them at the same time with the other changes was challenging.
- This does not track all source files in the workspace by default for performance considerations. Only directories which are partially "synced"
 with the vscode extension are tracked and compiled to improve performance. Additionally, there's a "lazy" compilation mode where only files that are open
 will be compiled and tracked rather than everything in the transitive closure of the packages that are synced.
- Adapt many of the existing test cases and add a few ones to work with the Bazel implementation.
- Revamped symbol indexing where we compile only file and use pre-computed outputs from Bazel to compute the global symbol index quickly for all the packages in the transitive closure. This makes quick fixes and completions quite a bit more useful and complete.
- Attempt to compile files in batch rather than do it serially as it was done originally in the LSP presumably because there were errors from the TopDownAnalyzer
- This has support for Go-to-definition and hover using pure source jars instead of decompiling (which is removed entirely).


This fork additionally also includes the [Kotlin Debug Adapter](https://github.com/fwcd/kotlin-debug-adapter) as a gradle module with the following changes:
- It uses source file -> JVM names mapping based on Bazel build outputs generated by an aspect
- It builds the bazel binary target, computes the runtime classpath for it with `bazel cquery` and then supplies that to JDI

The remaining core functionality with the LSP API and the usage of the compiler/storage is mostly the same.

## Requirements
- Use [rules_kotlin](https://github.com/bazelbuild/rules_kotlin) and [rules_jvm_external](https://github.com/bazel-contrib/rules_jvm_external) for external dependencies with `fetch_sources = True` so that source jars are available.
- Tested with Kotlin 1.9/Java 11 and Java 17 should also work.
- Tested with Bazel 6 and Bazel 7, but bzlmod not supported yet. Debugging is only supported with Bazel 7 as of now

## License

This project is a fork of [kotlin-language-server](https://github.com/fwcd/kotlin-language-server) by George Fraser, fwcd.

Modifications and extensions are Copyright (c) 2025 Sridhar Mocherla and also licensed under the MIT License.
