package lightstudio.mmo_plugin.listeners

import lightstudio.mmo_plugin.LightMmo
import lightstudio.mmo_plugin.SkillType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.Material
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Player

class FarmingListener(private val plugin: LightMmo) : Listener {

    private fun isWorldEnabled(worldName: String): Boolean {
        val enabledWorlds = plugin.config.getStringList("enabled-worlds")
        return enabledWorlds.isEmpty() || worldName in enabledWorlds
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        try {
            if (!isWorldEnabled(event.player.world.name)) return

            // 농작물 수확 시 경험치 지급 (예: 밀, 당근, 감자, 사탕무)
            if (event.block.blockData is Ageable) {
                val ageable = event.block.blockData as Ageable
                if (ageable.age == ageable.maximumAge) {
                    val exp = plugin.config.getInt("exp_gain.farming.crop_harvest.${event.block.type.name}", 0)
                    if (exp > 0) {
                        plugin.skillManager.addExp(event.player, SkillType.FARMING, exp)
                    }
                }
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error in FarmingListener (BlockBreak): " + e.message)
            e.printStackTrace()
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        try {
            if (!isWorldEnabled(event.player.world.name)) return

            val player = event.player
            val clickedBlock = event.clickedBlock ?: return

            // 씨앗 심기 경험치
            if (event.action.name == "RIGHT_CLICK_BLOCK" && clickedBlock.type == Material.FARMLAND) {
                val itemInHand = event.item ?: return
                val exp = plugin.config.getInt("exp_gain.farming.plant_seed.${itemInHand.type.name}", 0)
                if (exp > 0) {
                    plugin.skillManager.addExp(player, SkillType.FARMING, exp)
                }
            }

            // 밭 갈기 경험치
            if (event.action.name == "RIGHT_CLICK_BLOCK" &&
                (clickedBlock.type == Material.DIRT || clickedBlock.type == Material.GRASS_BLOCK) &&
                event.item?.type?.name?.endsWith("_HOE") == true) {
                val exp = plugin.config.getInt("exp_gain.farming.till_land", 0)
                if (exp > 0) {
                    plugin.skillManager.addExp(player, SkillType.FARMING, exp)
                }
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error in FarmingListener (PlayerInteract): " + e.message)
            e.printStackTrace()
        }
    }

    @EventHandler
    fun onEntityBreed(event: EntityBreedEvent) {
        try {
            val player = event.breeder as? Player ?: return
            if (!isWorldEnabled(player.world.name)) return

            val exp = plugin.config.getInt("exp_gain.farming.breed_animal.${event.entity.type.name}", 0)
            if (exp > 0) {
                plugin.skillManager.addExp(player, SkillType.FARMING, exp)
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error in FarmingListener (EntityBreed): " + e.message)
            e.printStackTrace()
        }
    }
}