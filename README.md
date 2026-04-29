# Perry

__This project is for research and study purposes__

JVM-based open-source server application for KMS version 1.2.31.

This project is derived from OdinMS and incorporates source code from a commercial game server. Contributions from OdinMS authors and numerous individual developers have been integrated into the original source code.

Perry is focused on providing an authentic gameplay experience. It excludes monetization, events, and donations, offering a pure, old-fashioned gaming experience. Registered trademarks and donation-related elements have been removed from the source code. Additionally, modern development APIs and libraries such as Kotlin, Dokka, and Ktor have been implemented.

> [!IMPORTANT]
> You must obtain the client files independently, as they are copyright-protected and not included in this project.

## Tech Stack

- **Language:** Kotlin
- **Frameworks:** 
  - [Ktor](https://ktor.io/) (Web API)
  - [Exposed](https://github.com/JetBrains/Exposed) (ORM)
  - [Netty](https://netty.io/) (Networking)
- **Database Support:** MySQL, MariaDB, MS SQL Server, PostgreSQL, SQLite, H2
- **Build System:** Gradle (Kotlin DSL)
- **Documentation:** [Dokka](https://kotlinlang.org/docs/dokka-introduction.html)
- **Logging:** Logback / Kotlin-logging

## Getting Started

### Requirements

- **Java Development Kit (JDK) 11** or higher.
- **SQL Database:** One of the supported databases (MySQL, PostgreSQL, MariaDB, MSSQL, etc.).
- **Operating System:** Any OS that supports Java 11 (Windows, Linux, macOS).

### Preparation

1.  **Configure Database:** Set up a database server of your choice.
2.  **WZ Files:** Convert your client WZ files into XML format.
3.  **Settings:** Create a `settings.json` file by copying `settings.json.sample` and modifying it with your database credentials and paths.
    ```bash
    cp settings.json.sample settings.json
    ```
4.  **Directory Structure:** Ensure the `wz/`, `scripts/`, and `settings.json` are in the project root or the same directory as the executable.

## Commands & Scripts

### Development

Run the server directly:
```bash
./gradlew run
```

Run tests:
```bash
./gradlew test
```

### Build & Distribution

Create distribution packages (Zip or Tar):
```bash
./gradlew distZip
./gradlew distTar
```

The outputs will be located in `build/distributions/`.

### Documentation

Generate HTML documentation using Dokka:
```bash
./gradlew DokkaHtml
```

### Entry Points

- **Server Main:** `MainKt` (in `src/main/kotlin/main.kt`)
  - Supports `download` subcommand for fetching remote assets (WZ/Scripts).
- **Web API:** `WebApiApplication` (in `src/main/kotlin/webapi/WebApiApplication.kt`)

## Configuration (settings.json)

Key configuration areas in `settings.json`:
- `database`: Connection details (host, port, user, password, type).
- `worlds`: Server rates (exp, meso, drop) and channel counts.
- `wzPath`: Path to the XML WZ data.
- `webApi`: Toggle and port for the Ktor-based Web API.
- `logging`: Directory and retention policy for logs.

### Environment Variables
- `configPath`: (Optional) System property to specify a custom path for `settings.json`. Default is `./settings.json`.

## Project Structure

```text
.
├── build.gradle.kts      # Gradle build configuration
├── settings.json         # Main configuration (you must create this)
├── src/main/kotlin       # Core server logic (Kotlin)
├── src/main/java         # Legacy or utility code (Java)
├── src/main/resources    # Static resources and web assets
├── src/test              # Unit and integration tests
├── scripts/              # Game scripts (NPC, Portal, Reactor, etc.)
├── wz/                   # Game data in XML format
└── logs/                 # Server logs
```

## Web API Documentation
Refer to the [GitBook documentation](https://amc-2.gitbook.io/perry/) for detailed API information.

## Roadmap / TODO
- [ ] Complete JavaDoc documentation using Dokka
- [ ] Fully implement Web API support
- [ ] Resolve NPC interaction issues during quest acceptance
- [ ] Separate Login, World, and Channel servers for independent operation

## License
Refer to the `LICENSE` file for full details.

---
## Tested Environments
- **JVM:** AdoptOpenJDK 11 (OpenJ9), Trava OpenJDK 11
- **OS:** Windows 10, Fedora 34
- **DB:** MSSQL 2019, Azure SQL, Yugabyte, PostgreSQL 14, MariaDB 10.6
