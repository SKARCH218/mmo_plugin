package lightstudio.mmo_plugin.listeners

import lightstudio.mmo_plugin.LightMmo
import lightstudio.mmo_plugin.SkillType
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.configuration.file.FileConfiguration

class GatheringListener(private val plugin: LightMmo, private val expConfig: FileConfiguration) : Listener {

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        try {
            val worldName = event.player.world.name
            val enabledWorlds = plugin.config.getStringList("enabled-worlds")
            if (enabledWorlds.isNotEmpty() && worldName !in enabledWorlds) {
                return
            }

            val exp = expConfig.getInt("exp_gain.gathering.block_break.${event.block.type.name}", 0)
            if (exp > 0) {
                plugin.skillManager.addExp(event.player, SkillType.GATHERING, exp)
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error in GatheringListener: " + e.message)
            e.printStackTrace()
        }
    }
}