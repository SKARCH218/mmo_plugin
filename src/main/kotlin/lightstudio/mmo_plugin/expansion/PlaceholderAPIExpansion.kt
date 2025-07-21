package lightstudio.mmo_plugin.expansion

import lightstudio.mmo_plugin.LightMmo
import lightstudio.mmo_plugin.SkillType
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player

class PlaceholderAPIExpansion(private val plugin: LightMmo) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "lightmmo"
    override fun getAuthor(): String = "LightStudio"
    override fun getVersion(): String = "1.0.0"

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) return null

        val args = params.split("_")
        if (args.size < 2) return null

        val type = args[0]
        val skillName = args[1].uppercase()

        val skill = try {
            SkillType.valueOf(skillName)
        } catch (e: IllegalArgumentException) {
            return null
        }

        return when (type) {
            "level" -> plugin.skillCache.cache.getIfPresent(Pair(player.uniqueId.toString(), skill))?.level?.toString()
            "exp" -> plugin.skillCache.cache.getIfPresent(Pair(player.uniqueId.toString(), skill))?.exp?.toString()
            else -> null
        }
    }
}