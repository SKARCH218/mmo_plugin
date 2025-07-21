package lightstudio.mmo_plugin.commands

import lightstudio.mmo_plugin.LightMmo
import lightstudio.mmo_plugin.SkillType
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.ChatColor

class MmoAdminCommand(private val plugin: LightMmo) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        try {
            if (args.isEmpty()) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("command_usage.mmo_admin_main") ?: "&cUsage: /mmoadmin <set|setguititle|reload>"))
                return true
            }

            when (args[0].lowercase()) {
                "set" -> {
                    if (args.size < 4) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("command_usage.mmo_admin_set") ?: "&cUsage: /mmoadmin set <player> <skill> <level>"))
                        return true
                    }

                    val player = Bukkit.getPlayer(args[1])
                    if (player == null) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("player_not_found") ?: "&cPlayer not found."))
                        return true
                    }

                    val skill = try {
                        SkillType.valueOf(args[2].uppercase())
                    } catch (e: IllegalArgumentException) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("invalid_skill_type") ?: "&cInvalid skill type."))
                        return true
                    }

                    val level = try {
                        args[3].toInt()
                    } catch (e: NumberFormatException) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("level_must_be_number") ?: "&cLevel must be a number."))
                        return true
                    }

                    plugin.skillManager.setLevel(player.uniqueId.toString(), skill, level).thenAccept { _ ->
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', (plugin.langConfig.getString("successfully_set_level") ?: "&aSuccessfully set {player}'s {skill} level to {level}.").replace("{player}", player.name).replace("{skill}", skill.name.lowercase()).replace("{level}", level.toString())))
                    }.exceptionally { ex ->
                        plugin.logger.severe("Error setting level: " + ex.message)
                        ex.printStackTrace()
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("internal_error") ?: "&cAn error occurred while setting the level."))
                        null
                    }
                }
                "setguititle" -> {
                    if (args.size < 2) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("command_usage.mmo_admin_setguititle") ?: "&cUsage: /mmoadmin setguititle <title>"))
                        return true
                    }
                    val newTitle = args.drop(1).joinToString(" ")
                    plugin.config.set("gui.title", newTitle)
                    plugin.saveConfig()
                    plugin.reloadPluginConfig()
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', (plugin.langConfig.getString("gui_title_set") ?: "&aGUI title set to: {title}").replace("{title}", newTitle)))
                }
                "reload" -> {
                    plugin.reloadPluginConfig()
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("plugin_reloaded") ?: "&aPlugin configuration reloaded."))
                }
                else -> {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("unknown_subcommand") ?: "&cUnknown subcommand."))
                }
            }
            return true
        } catch (e: Exception) {
            plugin.logger.severe("Error in MmoAdminCommand: " + e.message)
            e.printStackTrace()
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("internal_error") ?: "&cAn internal error occurred while executing the command."))
            return true
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): MutableList<String>? {
        if (args.size == 1) {
            return mutableListOf("set", "setguititle", "reload")
        } else if (args.size == 2 && args[0].lowercase() == "set") {
            return Bukkit.getOnlinePlayers().map { it.name }.toMutableList()
        } else if (args.size == 3 && args[0].lowercase() == "set") {
            return lightstudio.mmo_plugin.SkillType.values().map { it.name.lowercase() }.toMutableList()
        } else if (args.size == 4 && args[0].lowercase() == "set") {
            return mutableListOf("1", "10", "100") // Example levels
        }
        return null
    }
}