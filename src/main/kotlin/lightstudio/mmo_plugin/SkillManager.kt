package lightstudio.mmo_plugin

import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture
import org.bukkit.ChatColor
import org.bukkit.Particle
import org.bukkit.Sound

enum class SkillType {
    MINING,
    FARMING,
    FISHING,
    HUNTING,
    GATHERING
}

class SkillManager(private val plugin: LightMmo) {

    fun addExp(player: Player, skill: SkillType, amount: Int) {
        val uuid = player.uniqueId.toString()

        getLevel(uuid, skill).thenCompose { currentLevel ->
            getExp(uuid, skill).thenCompose { currentExp ->
                var newLevel = currentLevel
                var newExp = currentExp + amount

                // Level up check
                var requiredExp = calculateRequiredExp(newLevel)
                while (newExp >= requiredExp) {
                    newLevel++
                    newExp -= requiredExp

                    // Level up title message
                    player.sendTitle(
                        ChatColor.translateAlternateColorCodes('&', plugin.langConfig.getString("levelup_title") ?: "&a&lLEVEL UP!"),
                        ChatColor.translateAlternateColorCodes('&', (plugin.langConfig.getString("levelup") ?: "&e{skill} Level {level}").replace("{skill}", plugin.langConfig.getString("skills.${skill.name.lowercase()}") ?: skill.name.lowercase().capitalize()).replace("{level}", newLevel.toString())),
                        10, 70, 20
                    )
                    // Level up sound and particles
                    player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                    player.spawnParticle(Particle.EXPLOSION, player.location, 50, 0.5, 0.5, 0.5, 0.1)

                    // Execute level up commands
                    val commands = plugin.config.getStringList("skills.${skill.name.lowercase()}.rewards.$newLevel")
                    commands.forEach { cmd ->
                        val finalCmd = cmd.replace("{player}", player.name)
                        plugin.server.dispatchCommand(plugin.server.consoleSender, finalCmd)
                    }

                    requiredExp = calculateRequiredExp(newLevel)
                }
                setPlayerSkill(uuid, skill, newLevel, newExp)
            }
        }.thenAccept { _ ->
            // Exp gain action bar message
            player.sendActionBar(ChatColor.translateAlternateColorCodes('&', (plugin.langConfig.getString("exp_gain") ?: "&a+{amount} {skill} Exp").replace("{amount}", amount.toString()).replace("{skill}", plugin.langConfig.getString("skills.${skill.name.lowercase()}") ?: skill.name.lowercase().capitalize())))
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
        val formula = plugin.config.getString("default-exp-formula", "100 * level")!!
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
                plugin.databaseManager.execute { conn ->
                    val query = "INSERT OR REPLACE INTO player_skills (uuid, skill_${skillType.name.lowercase()}_level, skill_${skillType.name.lowercase()}_exp) VALUES (?, ?, ?)"
                    conn.prepareStatement(query).use { statement ->
                        statement.setString(1, uuid)
                        statement.setInt(2, data.level)
                        statement.setInt(3, data.exp)
                        statement.executeUpdate()
                    }
                    plugin.skillCache.invalidate(uuid, skillType)
                }
            }
        }
        return CompletableFuture.allOf(*futures.toTypedArray()).thenApply { Unit }
    }
}