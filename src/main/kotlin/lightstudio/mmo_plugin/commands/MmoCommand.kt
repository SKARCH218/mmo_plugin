package lightstudio.mmo_plugin.commands

import lightstudio.mmo_plugin.LightMmo
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.Bukkit
import org.bukkit.command.TabCompleter
import lightstudio.mmo_plugin.SkillType
import org.bukkit.ChatColor

class MmoCommand(private val plugin: LightMmo) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        try {
            if (sender !is Player) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("only_player_command") ?: "This command can only be run by a player."))
                return true
            }

            if (args.isNotEmpty()) {
                when (args[0].lowercase()) {
                    "top" -> {
                        if (args.size < 2) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("command_usage.mmo_top") ?: "&cUsage: /mmo top <skill>"))
                            return true
                        }
                        val skill = try {
                            lightstudio.mmo_plugin.SkillType.valueOf(args[1].uppercase())
                        } catch (e: IllegalArgumentException) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("invalid_skill_type") ?: "&cInvalid skill type."))
                            return true
                        }

                        val topPlayersFuture = plugin.databaseManager.execute { conn ->
                            val topPlayers = mutableListOf<Pair<String, Int>>()
                            val query = "SELECT uuid, skill_${skill.name.lowercase()}_level FROM player_skills ORDER BY skill_${skill.name.lowercase()}_level DESC LIMIT ?"
                            conn.prepareStatement(query).use { statement ->
                                statement.setInt(1, 10)
                                statement.executeQuery().use { resultSet ->
                                    while (resultSet.next()) {
                                        val uuid = resultSet.getString("uuid")
                                        val level = resultSet.getInt("skill_${skill.name .lowercase()}_level")
                                        topPlayers.add(Pair(uuid, level))
                                    }
                                }
                            }
                            topPlayers
                        }

                        topPlayersFuture.thenAccept { topPlayers ->
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', (plugin.langConfig.getString("top_players_header") ?: "&b--- Top 10 {skill} Players ---").replace("{skill}", skill.name.lowercase())))
                            topPlayers.forEachIndexed { index, (uuid, level) ->
                                val playerName = Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid)).name ?: "Unknown Player"
                                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', (plugin.langConfig.getString("top_player_entry") ?: "&e{rank}. &f{player} - Level {level}").replace("{rank}", (index + 1).toString()).replace("{player}", playerName).replace("{level}", level.toString())))
                            }
                        }.exceptionally { ex ->
                            plugin.logger.severe("Error getting top players: " + ex.message)
                            ex.printStackTrace()
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("internal_error") ?: "&cAn error occurred while fetching top players."))
                            null
                        }
                        return true
                    }
                    "view" -> {
                        if (!sender.hasPermission("lightmmo.command.mmo.view")) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("no_permission") ?: "&cYou don't have permission to do that."))
                            return true
                        }
                        if (args.size < 2) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("command_usage.mmo_view") ?: "&cUsage: /mmo view <player>"))
                            return true
                        }
                        val targetPlayer = Bukkit.getPlayer(args[1])
                        if (targetPlayer == null) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("player_not_online") ?: "&cPlayer is not online."))
                            return true
                        }
                        plugin.skillGui.open(targetPlayer)
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', (plugin.langConfig.getString("view_gui_success") ?: "&aOpened {player}'s skill GUI.").replace("{player}", targetPlayer.name)))
                        return true
                    }
                    else -> {
                        plugin.skillGui.open(sender)
                        return true
                    }
                }
            } else {
                plugin.skillGui.open(sender)
                return true
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error in MmoCommand: " + e.message)
            e.printStackTrace()
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("internal_error") ?: "&cAn internal error occurred while executing the command."))
            return true
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): MutableList<String>? {
        if (args.size == 1) {
            return mutableListOf("top", "view")
        } else if (args.size == 2 && args[0].lowercase() == "top") {
            return lightstudio.mmo_plugin.SkillType.values().map { it.name.lowercase() }.toMutableList()
        } else if (args.size == 2 && args[0].lowercase() == "view") {
            return Bukkit.getOnlinePlayers().map { it.name }.toMutableList()
        }
        return null
    }
}