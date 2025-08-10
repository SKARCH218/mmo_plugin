package lightstudio.mmo_plugin

import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture
import org.bukkit.ChatColor
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration

enum class SkillType {
    MINING,
    FARMING,
    FISHING,
    HUNTING,
    GATHERING
}

class SkillManager(val plugin: LightMmo, private val maxLevel: Int, private val rewardConfig: FileConfiguration) {

    fun addExp(player: Player, skill: SkillType, amount: Int) {
        val uuid = player.uniqueId.toString()

        getLevel(uuid, skill).thenCompose { currentLevel ->
            // If maxLevel is set (not 0) and currentLevel is already at maxLevel, do not add exp
            if (maxLevel != 0 && currentLevel >= maxLevel) {
                return@thenCompose CompletableFuture.completedFuture(null)
            }

            getExp(uuid, skill).thenCompose { currentExp ->
                var newLevel = currentLevel
                var newExp = currentExp + amount
                var skillReachedMax = false // Flag to track if skill reached max level in this call

                // Level up check
                var requiredExp = calculateRequiredExp(newLevel)
                while (newExp >= requiredExp) {
                    newLevel++
                    newExp -= requiredExp

                    // Check if max level is reached
                    if (maxLevel != 0 && newLevel >= maxLevel) {
                        newLevel = maxLevel
                        newExp = 0 // No more exp after max level
                        skillReachedMax = true // Set flag
                        // Max level title message
                        player.sendTitle(
                            ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("skill_max_level_title") ?: "&b&lSKILL MASTERED!"),
                            ChatColor.translateAlternateColorCodes('&', (plugin.langConfig.getString("skill_max_level") ?: "&e{skill} Max Level Reached!").replace("{skill}", plugin.langConfig.getString("skills.${skill.name.lowercase()}") ?: skill.name.lowercase().capitalize())),
                            10, 70, 20
                        )
                        // Max level sound and particles (optional, can be same as level up)
                        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.15f, 1f)
                        player.spawnParticle(Particle.EXPLOSION, player.location, 50, 0.5, 0.5, 0.5, 0.1)

                        // Execute on_skill_max_level_commands
                        val skillMaxLevelCommands = rewardConfig.getStringList("on_skill_max_level_commands")
                        skillMaxLevelCommands.forEach { cmd ->
                            val finalCmd = cmd.replace("{player}", player.name).replace("{skill}", skill.name.lowercase().capitalize())
                            plugin.server.dispatchCommand(plugin.server.consoleSender, finalCmd)
                        }
                        break // Stop leveling up
                    }

                    // Level up title message
                    player.sendTitle(
                        ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("levelup_title") ?: "&a&lLEVEL UP!"),
                        ChatColor.translateAlternateColorCodes('&', (plugin.langConfig.getString("levelup") ?: "&e{skill} Level {level}").replace("{skill}", plugin.langConfig.getString("skills.${skill.name.lowercase()}") ?: skill.name.lowercase().capitalize()).replace("{level}", newLevel.toString())),
                        10, 70, 20
                    )
                    // Level up sound and particles
                    player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.15f, 1f)
                    player.spawnParticle(Particle.EXPLOSION, player.location, 50, 0.5, 0.5, 0.5, 0.1)

                    // Execute level up commands
                    val commands = rewardConfig.getStringList("skills.${skill.name.lowercase()}.rewards.$newLevel")
                    commands.forEach { cmd ->
                        val finalCmd = cmd.replace("{player}", player.name)
                        plugin.server.dispatchCommand(plugin.server.consoleSender, finalCmd)
                    }

                    requiredExp = calculateRequiredExp(newLevel)
                }
                setPlayerSkill(uuid, skill, newLevel, newExp).thenAccept {
                    // Check if all skills are maxed after this skill levels up
                    checkAllSkillsMaxed(player)

                    // Only send action bar message if skill did not reach max level in this call
                    if (!skillReachedMax) {
                        player.sendActionBar(ChatColor.translateAlternateColorCodes('&', (plugin.langConfig.getString("exp_gain") ?: "&a+{amount} {skill} Exp").replace("{amount}", amount.toString()).replace("{skill}", plugin.langConfig.getString("skills.${skill.name.lowercase()}") ?: skill.name.lowercase().capitalize())))
                    }
                }
            }
        }
    }

    private fun checkAllSkillsMaxed(player: Player) {
        val uuid = player.uniqueId.toString()
        val allSkillsMaxedFuture = CompletableFuture.allOf(
            *SkillType.values().map { skillType ->
                getLevel(uuid, skillType).thenApply { level ->
                    maxLevel != 0 && level >= maxLevel
                }
            }.toTypedArray()
        ).thenApply {
            SkillType.values().all { skillType ->
                getLevel(uuid, skillType).join() >= maxLevel
            }
        }

        allSkillsMaxedFuture.thenAccept { allMaxed ->
            if (allMaxed) {
                // All skills maxed title message
                player.sendTitle(
                    ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("all_skills_max_level_title") ?: "&6&lALL SKILLS MASTERED!"),
                    ChatColor.translateAlternateColorCodes('&', (plugin.langConfig.getString("all_skills_max_level") ?: "&eAll skills mastered!").replace("{player}", player.name)),
                    10, 70, 20
                )
                // All skills maxed sound and particles (optional)
                player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.15f, 1f)
                player.spawnParticle(Particle.EXPLOSION, player.location, 100, 1.0, 1.0, 1.0, 0.1)

                val allSkillsMaxLevelCommands = rewardConfig.getStringList("on_all_skills_max_level_commands")
                allSkillsMaxLevelCommands.forEach { cmd ->
                    val finalCmd = cmd.replace("{player}", player.name)
                    plugin.server.dispatchCommand(plugin.server.consoleSender, finalCmd)
                }
            }
        }
    }

    private fun setPlayerSkill(uuid: String, skill: SkillType, level: Int, exp: Int): CompletableFuture<Unit> {
        return plugin.databaseManager.execute { conn ->
            val query = "INSERT OR REPLACE INTO player_skills (uuid, skill_${skill.name.lowercase()}_level, skill_${skill.name.lowercase()}_exp) VALUES (?, ?, ?)"
            conn.prepareStatement(query).use { statement ->
                statement.setString(1, uuid)
                statement.setInt(2, level)
                statement.setInt(3, exp)
                statement.executeUpdate()
            }
        }.thenApply {
            plugin.skillCache.invalidate(uuid, skill)
            Unit
        }
    }

    fun calculateRequiredExp(level: Int): Int {
        val formula = rewardConfig.getString("default-exp-formula", "100 * level")!!
        val expression = net.objecthunter.exp4j.ExpressionBuilder(formula)
            .variables("level")
            .build()
            .setVariable("level", level.toDouble())
        return expression.evaluate().toInt()
    }

    fun getLevel(uuid: String, skill: SkillType): CompletableFuture<Int> {
        return plugin.skillCache.get(uuid, skill).thenApply { it.level }
    }

    fun getExp(uuid: String, skill: SkillType): CompletableFuture<Int> {
        return plugin.skillCache.get(uuid, skill).thenApply { it.exp }
    }

    fun setLevel(uuid: String, skill: SkillType, level: Int): CompletableFuture<Unit> {
        return getExp(uuid, skill).thenCompose { currentExp ->
            setPlayerSkill(uuid, skill, level, currentExp)
        }
    }

    fun setExp(uuid: String, skill: SkillType, exp: Int): CompletableFuture<Unit> {
        return getLevel(uuid, skill).thenCompose { currentLevel ->
            setPlayerSkill(uuid, skill, currentLevel, exp)
        }
    }

    fun saveAllPlayerSkills(uuid: String): CompletableFuture<Unit> {
        val futures = SkillType.values().map { skillType ->
            plugin.skillCache.get(uuid, skillType).thenCompose { data ->
                setPlayerSkill(uuid, skillType, data.level, data.exp)
            }
        }
        return CompletableFuture.allOf(*futures.toTypedArray()).thenApply { Unit }
    }
}