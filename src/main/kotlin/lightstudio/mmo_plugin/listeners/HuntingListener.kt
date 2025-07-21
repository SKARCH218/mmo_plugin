package lightstudio.mmo_plugin.listeners

import lightstudio.mmo_plugin.LightMmo
import lightstudio.mmo_plugin.SkillType
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent

class HuntingListener(private val plugin: LightMmo) : Listener {

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        try {
            val killer = event.entity.killer ?: return

            if (event.entity.type != EntityType.PLAYER) {
                val exp = plugin.config.getInt("exp_gain.hunting.entity_kill.${event.entity.type.name}", 0)
                if (exp > 0) {
                    plugin.skillManager.addExp(killer, SkillType.HUNTING, exp)
                }
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error in HuntingListener: " + e.message)
            e.printStackTrace()
        }
    }
}