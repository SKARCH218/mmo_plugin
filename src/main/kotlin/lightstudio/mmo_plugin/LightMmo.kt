package lightstudio.mmo_plugin

import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.bukkit.event.HandlerList
import org.bukkit.command.CommandMap
import org.bukkit.command.Command
import org.bukkit.command.SimpleCommandMap
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class LightMmo : JavaPlugin() {

    lateinit var database: Database
        private set
    lateinit var databaseManager: DatabaseManager
        private set
    lateinit var skillCache: SkillCache
        private set
    lateinit var skillManager: SkillManager
        private set
    lateinit var skillGui: lightstudio.mmo_plugin.gui.SkillGui
        private set
    lateinit var langConfig: FileConfiguration
        private set

    override fun onEnable() {
        try {
            // Configuration files
            saveDefaultConfig()
            loadLanguageConfig()
            saveResource("exp.yml", false)

            // Database
            database = Database(this)
            database.connect()
            databaseManager = DatabaseManager(this, database)

            // Skill Cache
            skillCache = SkillCache(this)

            // Skill Manager
            skillManager = SkillManager(this)

            // Skill GUI
            skillGui = lightstudio.mmo_plugin.gui.SkillGui(this)

            // Register Listeners
            registerListeners()

            // Register Commands
            registerCommands()

            // Register PlaceholderAPI Expansion
            if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
                lightstudio.mmo_plugin.expansion.PlaceholderAPIExpansion(this).register()
                logger.info("PlaceholderAPI expansion registered.")
            } else {
                logger.warning("PlaceholderAPI not found. Placeholder expansion will not be registered.")
            }
            logger.info("LightMMO plugin enabled.")
        } catch (e: Exception) {
            logger.severe("Error enabling LightMMO plugin: " + e.message)
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {
        try {
            logger.info("Saving all online player data before disabling...")
            val saveFutures = server.onlinePlayers.map { player ->
                skillManager.saveAllPlayerSkills(player.uniqueId.toString())
                    .exceptionally { ex ->
                        logger.severe("Error saving skill data for player ${player.name} during shutdown: ${ex.message}")
                        ex.printStackTrace()
                        null
                    }
            }
            // Wait for all save operations to complete (with a timeout to prevent indefinite hang)
            CompletableFuture.allOf(*saveFutures.toTypedArray()).get(30, TimeUnit.SECONDS)
            logger.info("All online player data saved.")

            // Plugin shutdown logic
            if (::databaseManager.isInitialized) databaseManager.shutdown()
            if (::database.isInitialized) database.disconnect()
            if (::skillCache.isInitialized) skillCache.invalidateAll()

            unregisterListeners()
            unregisterCommands()

            logger.info("LightMMO plugin disabled.")
        } catch (e: Exception) {
            logger.severe("Error disabling LightMMO plugin: " + e.message)
            e.printStackTrace()
        }
    }

    private fun unregisterListeners() {
        HandlerList.unregisterAll(this)
    }

    private fun unregisterCommands() {
        val commandMapField = server.javaClass.getDeclaredField("commandMap")
        commandMapField.isAccessible = true
        val commandMap = commandMapField.get(server) as CommandMap

        val knownCommandsField = SimpleCommandMap::class.java.getDeclaredField("knownCommands")
        knownCommandsField.isAccessible = true
        val knownCommands = knownCommandsField.get(commandMap) as MutableMap<String, Command>

        listOf("mmo", "mmoadmin").forEach { cmd ->
            knownCommands.remove(cmd)
            knownCommands.remove("lightmmo:$cmd") // Remove plugin-specific alias
        }
    }

    fun reloadPluginConfig() {
        try {
            // Unregister existing listeners and commands
            unregisterListeners()
            unregisterCommands()

            // Reload config files
            reloadConfig()
            saveDefaultConfig()
            loadLanguageConfig()
            saveResource("exp.yml", false)

            // Invalidate cache
            if (::skillCache.isInitialized) skillCache.invalidateAll()

            // Re-register listeners and commands
            registerListeners()
            registerCommands()

            logger.info("LightMMO plugin configuration reloaded.")
        } catch (e: Exception) {
            logger.severe("Error reloading LightMMO plugin configuration: " + e.message)
            e.printStackTrace()
        }
    }

    private fun registerListeners() {
        server.pluginManager.registerEvents(lightstudio.mmo_plugin.listeners.BlockBreakListener(this), this)
        server.pluginManager.registerEvents(lightstudio.mmo_plugin.listeners.FishingListener(this), this)
        server.pluginManager.registerEvents(lightstudio.mmo_plugin.listeners.HuntingListener(this), this)
        server.pluginManager.registerEvents(lightstudio.mmo_plugin.listeners.GatheringListener(this), this)
        server.pluginManager.registerEvents(lightstudio.mmo_plugin.listeners.FarmingListener(this), this)
        server.pluginManager.registerEvents(lightstudio.mmo_plugin.listeners.PlayerConnectionListener(this), this)
        server.pluginManager.registerEvents(lightstudio.mmo_plugin.listeners.SkillGuiListener(this), this)
    }

    private fun registerCommands() {
        getCommand("mmo")?.setExecutor(lightstudio.mmo_plugin.commands.MmoCommand(this))
        getCommand("mmo")?.tabCompleter = lightstudio.mmo_plugin.commands.MmoCommand(this)
        getCommand("mmoadmin")?.setExecutor(lightstudio.mmo_plugin.commands.MmoAdminCommand(this))
        getCommand("mmoadmin")?.tabCompleter = lightstudio.mmo_plugin.commands.MmoAdminCommand(this)
    }

    private fun loadLanguageConfig() {
        val langFileName = "lang.yml"
        val langFile = File(dataFolder, langFileName)

        // Always save the resource to ensure it's present in the data folder
        saveResource(langFileName, false)

        langConfig = YamlConfiguration.loadConfiguration(langFile)

        // If the language file is still empty after loading, log a warning.
        if (langConfig.getKeys(false).isEmpty()) {
            logger.warning("Language file '$langFileName' is empty or could not be loaded. Please check your language file configuration.")
            if (!langFile.exists()) {
                logger.severe("Language file '$langFileName' does not exist in data folder: ${langFile.absolutePath}")
            } else if (langFile.length() == 0L) {
                logger.severe("Language file '$langFileName' exists but is empty: ${langFile.absolutePath}")
            }
        }
    }
}
