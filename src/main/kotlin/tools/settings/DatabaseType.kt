package tools.settings

/**
 * DatabaseType is an enumeration that represents the different types of databases that can be used in the application.
 * Each enum value corresponds to a specific type of database (e.g., MySQL, MSSQL, SQLite, Oracle, H2, PostgreSQL).
 * This enum is typically used in the context of configuring a database connection, where the type of the database needs to be specified.
 *
 * @property value The string representation of the database type.
 */
enum class DatabaseType(val value: String) {
    MySQL("mysql"), // Represents a MySQL database
    MSSQL("mssql"), // Represents a Microsoft SQL Server database
    SQLite("sqlite"), // Represents a SQLite database
    Oracle("oracle"), // Represents an Oracle database
    H2("h2"), // Represents a H2 database
    PostgreSQL("postgresql") // Represents a PostgreSQL database
}