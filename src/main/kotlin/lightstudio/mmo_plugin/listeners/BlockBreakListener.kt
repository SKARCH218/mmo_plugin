package lightstudio.mmo_plugin.listeners

import lightstudio.mmo_plugin.LightMmo
import lightstudio.mmo_plugin.SkillType
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

class BlockBreakListener(private val plugin: LightMmo) : Listener {

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        try {
            val exp = plugin.config.getInt("exp_gain.mining.block_break.${event.block.type.name}", 0)
            if (exp > 0) {
                plugin.skillManager.addExp(event.player, SkillType.MINING, exp)
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error in BlockBreakListener: " + e.message)
            e.printStackTrace()
        }
    }
}