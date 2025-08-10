package lightstudio.mmo_plugin.expansion

import lightstudio.mmo_plugin.LightMmo
import lightstudio.mmo_plugin.SkillType
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer

class PlaceholderAPIExpansion(private val plugin: LightMmo) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "lightmmo"
    override fun getAuthor(): String = "LightStudio"
    override fun getVersion(): String = "1.0.0"
    override fun persist(): Boolean = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (player == null) return null

        val args = params.split("_")
        if (args.size < 2) return null

        val skillName = args[0].uppercase()
        val type = args[1].lowercase()

        val skill = try {
            SkillType.valueOf(skillName)
        } catch (e: IllegalArgumentException) {
            return "Invalid Skill"
        }

        val playerData = plugin.skillCache.getPlayerSkillData(player.uniqueId, skill)

        return when (type) {
            "level" -> playerData.level.toString()
            "exp" -> playerData.exp.toString()
            "max_exp" -> plugin.skillManager.calculateRequiredExp(playerData.level).toString()
            "percent" -> {
                val maxExp = plugin.skillManager.calculateRequiredExp(playerData.level)
                if (maxExp > 0) {
                    String.format("%.2f", (playerData.exp.toDouble() / maxExp) * 100)
                } else {
                    "100.00"
                }
            }
            else -> null
        }
    }
}