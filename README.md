# Perry
__This project is for research and study purposes__

JVM-based open-source server application for KMS version 1.2.31.

This project is derived from OdinMS and incorporates source code from a commercial game server. Contributions from OdinMS authors and numerous individual developers have been integrated into the original source code.

Perry is focused on providing an authentic gameplay experience. It excludes monetization, events, and donations, offering a pure, old-fashioned gaming experience. Registered trademarks and donation-related elements have been removed from the source code. Additionally, modern development APIs and libraries such as Kotlin, Dokka, and Ktor have been implemented.

(Note: You must obtain the client files independently, as they are copyright-protected and not included in this project.)
### Web API Documentation
Please check out dedicated documentations written in [GitBook](https://amc-2.gitbook.io/perry/)
## Getting started
### Requirements
- Java 11 (recommended; for Java 1.8, you need to change the target version and recompile)
- SQL database:
   - MySQL, MariaDB
   - MS SQL Server
   - PostgreSQL
   - SQLite (Memory database not supported)
   - DBaaS (e.g., Amazon RDS, Azure Database)
   - For a complete list, refer to [Exposed](https://github.com/Jetbrains/Exposed)
- Operating System that supports Java 11
- Refer to the tested environment list below
### Preparation
Following instructions required to run Perry.
 - Configure SQL database server in somewhere. (MySQL, PostgresSQL, MariaDB, MSSQL... whatever you want. Or simply you can use DBaaS service.)
 - Dump WZ in Client into XML (you might know what it is...).
 - Create a settings.json file using settings.json.sample as a template (remove the .sample extension).
 - Ensure the wz and scripts folders, along with the settings.json file, are located in the same directory as Perry or Perry.bat in the bin folder.
 - After the initial server startup, you can modify the location of the wz or scripts folders using the settings.json file.
### Production
Download pre-compiled files from the Releases section.

Alternatively, download the nightly version, which is compiled with every commit.

- Follow the instructions above and run `Perry` or `Perry.bat` in the `bin` folder using your preferred shell.
### Development
#### Run
```shell
$ ./gradlew run
```
#### Distributions
In Zip file:
```shell
$ ./gradlew distZip
```
In Tar file:
```shell
$ ./gradlew distTar
```
#### Generate documentation files
```shell
$ ./gradlew DokkaHtml
```
## Todo
- [ ] Complete JavaDoc documentation using Dokka
- [ ] Implement Web API support
- [ ] Resolve issue with NPCs not reacting when accepting quests
- [ ] Separate Login, World, and Channel servers for independent operation
- [ ] Additional features to be added...
## Download
 - Nightly (or SNAPSHOT) build download [here](https://nightly.link/andrewcell/Perry/workflows/gradle/main).
## Tested Environments
#### JVM
 - AdoptOpenJDK 11 (OpenJ9)
 - Trava OpenJDK 11 
#### Operating Systems
 - Windows 10
 - Fedora 34
#### Database
- Microsoft SQL Server 2019 Developer Edition (15.0) installed on RHEL8 - mssql
- Azure Database (SQL Server 12.0) - mssql
- Yugabyte (PostgreSQL-compatible) - postgresql
- PostgreSQL 14.0 installed on Windows Server 2012 R2 - postgresql
- MariaDB 10.6 installed on Windows Server 2022 - mysql
