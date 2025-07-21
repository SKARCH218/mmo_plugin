package lightstudio.mmo_plugin

import java.sql.Connection
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

    fun shutdown() {
        executor.shutdown()
    }
}