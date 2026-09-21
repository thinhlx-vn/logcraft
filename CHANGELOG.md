# Changelog

## 1.0.2 — release metadata

- Clarified LogCraft's positioning as a private, local-first log pipeline workbench.
- Fixed the local source build to package the repository's Apache License 2.0 file.

## 1.0.1 — compatibility fix

- Fixed binary compatibility with IntelliJ Platform 2025.2–2026.2 after
  `ContentFactory` changed from a class to an interface.
- Added a reproducible Docker verification pipeline using Gradle 9.7.1 and
  JDK 25, while retaining Java 17 plugin bytecode compatibility.
- Added the explicit JUnit Platform launcher required by Gradle 9 test workers.

## 1.0.0 — free release candidate

- Added Grok compilation and named field extraction.
- Added Java regex match/group inspection.
- Added JSON formatting and minification.
- Added ISO/epoch timestamp conversion.
- Added local masking for common sensitive values.
- Added batch Grok testing with match-rate reporting.
- Added ECS field review and Elasticsearch mapping suggestions.
- Added Logstash configuration heuristic inspection.
- Added pasted Kafka message inspection and local event simulation.
- Added JetBrains tool window with ten utilities.
- Removed the runtime JSON dependency and added an offline reproducible plugin builder.
- Released the source code under the Apache License 2.0.
