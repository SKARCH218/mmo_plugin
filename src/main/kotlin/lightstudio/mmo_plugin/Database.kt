package lightstudio.mmo_plugin

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class Database(private val plugin: LightMmo) {

    var connection: Connection? = null
        private set

    fun connect() {
        val dataFolder = File(plugin.dataFolder, "data.db")
        if (!dataFolder.exists()) {
            dataFolder.parentFile.mkdirs()
        }

        try {
            Class.forName("org.sqlite.JDBC")
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder.absolutePath)
            createTables()
            plugin.logger.info("SQLite DB connected successfully.")
        } catch (e: Exception) {
            plugin.logger.severe("Could not connect to SQLite DB: " + e.message)
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            connection?.close()
            plugin.logger.info("SQLite DB disconnected.")
        } catch (e: Exception) {
            plugin.logger.severe("Error disconnecting SQLite DB: " + e.message)
            e.printStackTrace()
        }
    }

    private fun createTables() {
        try {
            connection?.createStatement()?.use { statement ->
                statement.execute(
                    "CREATE TABLE IF NOT EXISTS player_skills (" +
                            "uuid TEXT PRIMARY KEY NOT NULL," +
                            "skill_mining_level INTEGER DEFAULT 1," +
                            "skill_mining_exp INTEGER DEFAULT 0," +
                            "skill_farming_level INTEGER DEFAULT 1," +
                            "skill_farming_exp INTEGER DEFAULT 0," +
                            "skill_fishing_level INTEGER DEFAULT 1," +
                            "skill_fishing_exp INTEGER DEFAULT 0," +
                            "skill_hunting_level INTEGER DEFAULT 1," +
                            "skill_hunting_exp INTEGER DEFAULT 0," +
                            "skill_gathering_level INTEGER DEFAULT 1," +
                            "skill_gathering_exp INTEGER DEFAULT 0" +
                            ");"
                )
                plugin.logger.info("player_skills table checked/created.")
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error creating tables: " + e.message)
            e.printStackTrace()
        }
    }
}