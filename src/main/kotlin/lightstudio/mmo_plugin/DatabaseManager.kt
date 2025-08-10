package lightstudio.mmo_plugin

import java.sql.Connection
import java.sql.SQLException
import java.util.concurrent.Executors
import java.util.concurrent.CompletableFuture

class DatabaseManager(private val plugin: LightMmo, private val database: Database) {

    private val executor = Executors.newSingleThreadExecutor()

    fun <T> execute(task: (Connection) -> T): CompletableFuture<T> {
        return CompletableFuture.supplyAsync({
            database.connection?.let { conn ->
                try {
                    task(conn)
                } catch (e: Exception) {
                    plugin.logger.severe("Error executing database task: " + e.message)
                    e.printStackTrace()
                    throw e
                }
            } ?: throw IllegalStateException("Database connection is not available.")
        }, executor)
    }

    fun loadPlayerSkillDataSync(uuid: String, skillType: SkillType): PlayerSkillData? {
        return try {
            database.connection?.let { conn ->
                val query = "SELECT skill_${skillType.name.lowercase()}_level, skill_${skillType.name.lowercase()}_exp FROM player_skills WHERE uuid = ?"
                conn.prepareStatement(query).use { statement ->
                    statement.setString(1, uuid)
                    statement.executeQuery().use { resultSet ->
                        if (resultSet.next()) {
                            val level = resultSet.getInt("skill_${skillType.name.lowercase()}_level")
                            val exp = resultSet.getInt("skill_${skillType.name.lowercase()}_exp")
                            PlayerSkillData(level, exp)
                        } else {
                            null
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            plugin.logger.severe("Error loading player skill data synchronously: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    fun shutdown() {
        executor.shutdown()
    }
}