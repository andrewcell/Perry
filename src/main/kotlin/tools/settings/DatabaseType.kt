package tools.settings

enum class DatabaseType(val value: String) {
    MySQL("mysql"), MSSQL("mssql"), SQLite("sqlite"), Oracle("oracle"), H2("h2"), PostgreSQL("postgresql")
}