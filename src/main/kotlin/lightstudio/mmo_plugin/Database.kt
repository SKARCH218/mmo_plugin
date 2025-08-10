package lightstudio.mmo_plugin

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class Database(private val plugin: LightMmo) {

    var connection: Connection? = null
        private set

    fun connect() {
        val dbType = plugin.config.getString("database.type", "SQLite")?.uppercase() ?: "SQLITE"

        try {
            when (dbType) {
                "SQLITE" -> connectSQLite()
                "MYSQL" -> connectMySQL()
                else -> {
                    plugin.logger.severe("Unsupported database type: $dbType. Defaulting to SQLite.")
                    connectSQLite()
                }
            }
            createTables()
        } catch (e: Exception) {
            plugin.logger.severe("Database connection failed: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun connectSQLite() {
        val dataFolder = File(plugin.dataFolder, "data.db")
        if (!dataFolder.exists()) {
            dataFolder.parentFile.mkdirs()
        }
        Class.forName("org.sqlite.JDBC")
        connection = DriverManager.getConnection("jdbc:sqlite:${dataFolder.absolutePath}")
        plugin.logger.info("SQLite DB connected successfully.")
    }

    private fun connectMySQL() {
        val host = plugin.config.getString("database.mysql.host", "localhost")
        val port = plugin.config.getInt("database.mysql.port", 3306)
        val database = plugin.config.getString("database.mysql.database", "mmo_plugin")
        val username = plugin.config.getString("database.mysql.username", "root")
        val password = plugin.config.getString("database.mysql.password", "")

        val url = "jdbc:mysql://$host:$port/$database?autoReconnect=true&useSSL=false"

        Class.forName("com.mysql.cj.jdbc.Driver")
        connection = DriverManager.getConnection(url, username, password)
        plugin.logger.info("MySQL DB connected successfully.")
    }

    fun disconnect() {
        try {
            connection?.close()
            plugin.logger.info("Database disconnected.")
        } catch (e: SQLException) {
            plugin.logger.severe("Error disconnecting database: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun createTables() {
        val query = """
        CREATE TABLE IF NOT EXISTS player_skills (
            uuid VARCHAR(36) PRIMARY KEY NOT NULL,
            skill_mining_level INT DEFAULT 1,
            skill_mining_exp INT DEFAULT 0,
            skill_farming_level INT DEFAULT 1,
            skill_farming_exp INT DEFAULT 0,
            skill_fishing_level INT DEFAULT 1,
            skill_fishing_exp INT DEFAULT 0,
            skill_hunting_level INT DEFAULT 1,
            skill_hunting_exp INT DEFAULT 0,
            skill_gathering_level INT DEFAULT 1,
            skill_gathering_exp INT DEFAULT 0
        );
        """
        try {
            connection?.createStatement()?.use { statement ->
                statement.execute(query)
                plugin.logger.info("player_skills table checked/created.")
            }
        } catch (e: SQLException) {
            plugin.logger.severe("Error creating tables: ${e.message}")
            e.printStackTrace()
        }
    }
}
