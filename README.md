# LogCraft

LogCraft is a free, private, local-first log pipeline workbench for Grok, ECS, Logstash, pasted Kafka messages, and safe log sharing inside your JetBrains IDE. It is designed for developers, DevOps engineers, SREs, security engineers, and users of ELK, Logstash, and Kafka.

Use one focused workflow: sample log → Grok → batch testing → ECS and mapping review → Logstash inspection → sensitive-data masking → safe sharing.

## Features

- Test single-line Grok patterns and inspect extracted fields
- Test Grok patterns against multiple lines and calculate the match rate
- Test Java regular expressions and captured groups
- Validate, format, and minify JSON
- Convert ISO-8601 timestamps, epoch seconds, and epoch milliseconds
- Mask IP addresses, email addresses, passwords, tokens, and card-like numbers
- Review field names against common Elastic Common Schema conventions
- Suggest Elasticsearch mappings from JSON samples
- Inspect Logstash configuration for structural issues, exposed secrets, deprecated settings, and unsafe TLS options
- Inspect pasted Kafka messages and simulate a local Grok pipeline

## Privacy

LogCraft processes supplied text locally in the IDE. It does not upload logs, use a hosted backend, require a LogCraft account, include telemetry, or depend on AI for its normal features.

## Installation

Install LogCraft from the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/34344-logcraft) when the current release becomes available.

To install a locally built archive:

1. Run the build described below.
2. In a compatible JetBrains IDE, open **Settings > Plugins**.
3. Select the gear icon and choose **Install Plugin from Disk**.
4. Select `build/distributions/logcraft-1.0.2.zip` and restart the IDE.
5. Open **View > Tool Windows > LogCraft**.

## Build

The lightweight local build requires Java 17 or newer with the `jdk.compiler` module:

```bash
./scripts/build-local.sh
```

For the standard Gradle build, use JDK 25 and Gradle 9 or newer:

```bash
gradle test
gradle verifyPlugin
gradle buildPlugin
```

The plugin archive is created under `build/distributions`.

## License

LogCraft is free and open-source software licensed under the [Apache License 2.0](LICENSE).

Copyright 2026 LogCraft.
