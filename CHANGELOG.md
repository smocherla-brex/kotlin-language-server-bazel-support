# Change Log
All notable changes to the language server will be documented in this file.

Check [Keep a Changelog](http://keepachangelog.com/) for recommendations on how to structure this file.

## v1.6.3-bazel
- Support bzlmod (#48) (Sridhar Mocherla)

## v1.6.2-bazel
- Index JDK symbols (#46) (Sridhar Mocherla)

## v1.6.1-bazel
- Fix query for global symbols and do indexing in background (#44) (Sridhar Mocherla)
- Fix bug which limited symbols being returned (Sridhar Mocherla)

## v1.6.0-bazel
- Update README (Sridhar Mocherla)
- Bump version (#43) (Sridhar Mocherla)
- Pick atleast one file for symbol indexing (#42) (Sridhar Mocherla)
- Adapt symbol indexing to Bazel (#41) (Sridhar Mocherla)

## v1.5.3-bazel
- Propagate lazy compilation config to SourceFiles (Sridhar Mocherla)

## v1.5.2-bazel
- Initialize lazy compilation config on startup (#39) (Sridhar Mocherla)

## v1.5.1-bazel
- Disable lazy compilation by default (#38) (Sridhar Mocherla)

## v1.5.0-bazel
- Handle lazy compilation to support Go-to (#37) (Sridhar Mocherla)
- Compile files lazily to improve performance (#36) (Sridhar Mocherla)

## v1.4.0-bazel
- Update REAMDE (#35) (Sridhar Mocherla)
- Fix source mapping from bytecode to source location (#34) (Sridhar Mocherla)
- Use target classpath resolver in debug adapter (#33) (Sridhar Mocherla)

## v1.3.18-bazel
- Handle very large classpath (#32) (Sridhar Mocherla)
- Support passing additional args to main class (#30) (Sridhar Mocherla)

## v1.3.17-bazel
- Bump version (Sridhar Mocherla)
- Release adapter in workflow (#29) (Sridhar Mocherla)
- Fix source file mapping (#28) (Sridhar Mocherla)
- Delete unused/unnecessary file added in tests (Sridhar Mocherla)

## v1.3.16-bazel
- Bump version (#24) (Sridhar Mocherla)
- fix: Pass java source files correctly to the Kotlin compiler (#23) (Sridhar Mocherla)
- Remove hardcoded classpath entry and some more logging (#22) (Sridhar Mocherla)
- Resolve JVM names more accurately (#21) (Sridhar Mocherla)
- Update proto with jvm_class_names (#20) (Sridhar Mocherla)
- Ignore unknown fields while parsing protojson (#19) (Sridhar Mocherla)

## v1.3.15-bazel
- Bump version (Sridhar Mocherla)
- Handle NPE when location is not found in source jar (#18) (Sridhar Mocherla)
- Run bazel build in debug adapter on launch (#17) (Sridhar Mocherla)
- Update debug adapter with new configuration (#16) (Sridhar Mocherla)
- Add debug adapter with bazel support (#15) (Sridhar Mocherla)
- Bump com.jaredsburrows.license from 0.8.42 to 0.9.8 (#10) (dependabot[bot])
- Bump org.gradle.toolchains.foojay-resolver-convention (#11) (dependabot[bot])
- Remove some unecessary files (Sridhar Mocherla)


## [1.3.13]
- Bump Kotlin to 2.1.0

## [1.3.12]
- Make code generation for Java interop opt-in (#585)

## [1.3.11]
- Fix crash when a Maven repository is missing (#584)

## [1.3.10]
- Update LSP4J to 0.21.2
- Increase maximum length in class path cache (#532)
- Fix some bugs

## [1.3.9]
- Improve source file exclusion logic

## [1.3.8]
- Add optional inlay hints (#498)
- Provide infix function completions (#521)
- Support Gradle 8.5 and Kotlin 1.9.20 (#537)
- Make .kts and .gradle.kts language features opt-in (#536)
Thanks to @ElamC, @themkat, @chenrui333, @calamont, @ks-korovina and @daplf for
your contributions!

## [1.3.7]
- Fix definition lookup for external sources
- Fix binding context query for other definition lookups

## [1.3.6]
- Update to Gradle 8.3 and Kotlin 1.9.10

## [1.3.5]
- Fix push trigger in Docker CI workflow

## [1.3.4]
- Cache dependencies
- Modernize the Docker image

## [1.3.3]
- Minor bug fixes

## [1.3.2]
- Update to Kotlin 1.8.10 and Gradle 8
- Backtick soft keywords in imports (#416, #455)
- Improve syntax highlighting w.r.t strings in type annotations (#405)
- Add support for variable renaming from declaration sites (#399)
- Add support for SAM with receiver annotations (#394)
- Implement document highlight (#393)
- Add override/implement (#359)
Special thanks to @themkat, @RenFraser and all other contributors for the great work!

## [1.3.1]
- Add support for run/debug code lenses
- Add definition lookup support for JDT symbols
- Add quick fix for implementing abstract functions
- Add experimental JDT.LS integration

## [1.3.0]
- Bump to Kotlin 1.6
- Support JDK 17
- Add incremental indexing
- Improve logged Gradle errors

## [1.2.0]
- Support for renaming
- Improved semantic highlighting for enum members
- Improved region code folding

## [1.1.2]
- Support for semantic highlighting
- Improved source jar lookup support
- Improved Kotlin DSL support

## [1.1.1]
- Exclude too long symbol names from index for now

## [1.1.0]
- Provide ServerInfo via LSP's initialize
- Make Gradle class path resolver slightly more robust

## [1.0.0]
- Symbol indexing
- Code completion for unimported members
- Improved progress bars

## [0.9.0]
- More detailed initialization progress
- Improved completion icons

## [0.8.3]
- Lazier file tree walking while looking for resolvers

## [0.8.2]
- Minor tweaks

## [0.8.1]
- Minor fixes

## [0.8.0]
- Tagged deprecation/unused variable warnings
- Kotlin 1.4.20 support
- New formatter (ktfmt)
- Java 11+ is now required

## [0.7.1]

## [0.7.0]
- Improve completion list sorting
- Fix bug that occurred when project path contained whitespace

## [0.6.0]
- Add package completion
- Support Java imports
- Provide tree-structured document symbols
- Improve lint scheduling
- Fix formatOnSave and some other bugs

## [0.5.2]
- Fix Docker image tag

## [0.5.1]
- Fix Docker deployment and codeblock grammar

## [0.5.0]
- Add Docker support
- Add support for TCP transport

## [0.4.0]
- Add support for Kotlin DSL buildscripts
- Add support for non-Maven/Gradle projects
- Improve Java-to-Kotlin converter
- Use the Field icon instead of Property icon in completion lists
- Add experimental support for multiplatform projects
- Introduce the 'kls' URI scheme

## [0.3.0]
- Improve trailing lambda completions
- Provide completions for generic extension methods

## [0.2.9]
- Include a grammar ZIP in the release assets

## [0.2.8]
- Include grammars distribution in release

## [0.2.7]
- Improve release naming

## [0.2.6]
- Remove version postfix from ZIP distributions

## [0.2.5]
- Move editor extensions into separate repositories

## [0.2.0]
- Rewrite Java-to-Kotlin converter from scratch
- Implement Kotlin formatter
- Add keyword completions
- Make completions more reliable

## [0.1.13]
- Kotlin 1.3.11 support
- Detailed completion signatures for overloaded methods
- Updated to VSCode ^1.30.2
- Improved dependency resolution logs

## [0.1.12]
- Kotlin 1.2.70 support

## [0.1.11]
- Improved keyword syntax highlighting

## [0.1.10]
- More compact distribution
- Security fixes
- Fixed decompiler

## [0.1.6]
- Bugfixes related to Gradle dependency resolution in combination with Android projects

## [0.1.5]
- Bugfixes related to Kotlin stdlib resolution through Gradle

## [0.1.4]
- Async language server operations

## [0.1.3]
- Java to Kotlin converter

## [0.1.2]
- First stable build

## [0.1.1]
- Migrated to the Gradle build tool

## [0.1.0]
- Initial release
