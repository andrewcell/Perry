# Perry
JVM based open source implemented server application for KMS version 1.2.31

Based on OdinMS and source code from commercial-purpose game server. OdinMS authors, and so many individual developers contributed to original source code.

Perry focused on Original game play experience. No-Money things, No-Events, No-Donation. Just play a game in old-fashioned version. Remove registered trademarks, Donation event things in source code. Also, implement modern development APIs, Libraries. (Kotlin, Dokka, Ktor...)

(You need to obtain Client for yourself. This is copyright protected files, and I don't want more trouble.)
## Getting started
### Requirements
 - Java 11 (recommended. For 1.8, you need to change target version and recompile.)
 - SQL database
   - MySQL, MariaDB
   - MS SQL Server
   - PostgresSQL
   - SQLite (Not support Memory database)
   - You also can use DBaaS (like Amazon RDS, Azure Database...)
   - And more..? (See [Exposed](https://github.com/Jetbrains/Exposed) for full list.)
 - Operating System that support Java 11.
 - Check out tested environment list below.
### Preparation
Following instructions required to run Perry.
 - Configure SQL database server in somewhere. (MySQL, PostgresSQL, MariaDB, MSSQL... whatever you want. Or simply you can use DBaaS service.)
 - Dump WZ in Client into XML (you might know what it is...).
 - Download PerryScripts, extract and rename to scripts 
 - Create settings.json using settings.json.sample. (Of course, remove .sample to actual file.)
 - Above 2 folders (wz, scripts) and 1 file (settings.json) must be located with Perry or Perry.bat in bin folder.
 - After first start of server, you can change location of wz or scripts folder using settings.json.
### Production
Download pre-compiled files from Releases section.

OR, Download the nightly version which is being compiled when every commit.

- Follow instruction above, run a Perry or Perry.bat in bin folder with your favorite shell.
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
 - [ ] Full JavaDoc documentation (Dokka)
 - [ ] Web API support
 - [ ] Fix No NPC reaction when accept quest
 - [ ] Separation of Login-World-Channel servers (To make run separately)
 - [ ] More is coming...
## Download
 - Nightly (or SNAPSHOT) build download [here](https://nightly.link/andrewcell/Perry/workflows/gradle/master).
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
