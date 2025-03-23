# Kotlin Language Server with Bazel Support

This is a fork of [kotlin-language-server](https://github.com/fwcd/kotlin-language-server) with Bazel support added to it. It works in tandem with a vscode extension.


![Icon](Icon128.png)

Only VSCode currently is supported/tested, but it may be possible to support other editors with additional configuration.

## How this differs from the original

This fork only focuses on Bazel support and making sure it works reliably. As a result
- I had to remove any existing Gradle/Maven support as supporting all of them at the same time with the other changes was challenging.
- This does not track all source files in the workspace by default for performance considerations. Only directories which are partially "synced"
 with the vscode extension are tracked and compiled to improve performance.
- This has support for Go-to-definition and hover using pure source jars instead of decompiling (which is removed entirely).


## License

This project is a fork of [kotlin-language-server](https://github.com/fwcd/kotlin-language-server) by George Fraser, fwcd.

Modifications and extensions are Copyright (c) 2025 Sridhar Mocherla and also licensed under the MIT License.
