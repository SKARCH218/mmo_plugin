package lightstudio.mmo_plugin.listeners

import dev.lone.itemsadder.api.CustomBlock
import dev.lone.itemsadder.api.CustomStack
import lightstudio.mmo_plugin.SkillManager
import lightstudio.mmo_plugin.SkillType
import org.bukkit.Material
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.player.PlayerInteractEvent

class FarmingListener(private val skillManager: SkillManager) : Listener {

    private val customPots by lazy { skillManager.plugin.config.getStringList("custom-pots") }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onCropHarvest(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block
        val location = block.location

        if (!skillManager.plugin.isPluginEnabledInWorld(player.world.name)) return

        val customBlock = CustomBlock.byAlreadyPlaced(block)
        if (customBlock != null) {
            skillManager.plugin.server.scheduler.runTask(skillManager.plugin, Runnable {
                if (location.block.type == Material.AIR) {
                    val cropId = customBlock.getNamespacedID()
                    val exp = skillManager.plugin.getExpForFarming("crop_harvest", cropId)
                    if (exp > 0) {
                        skillManager.addExp(player, SkillType.FARMING, exp)
                    }
                }
            })
            return
        }

        val ageable = block.blockData as? Ageable
        if (ageable != null && ageable.maximumAge != ageable.age) {
            return
        }

        val cropType = block.type.name
        val exp = skillManager.plugin.getExpForFarming("crop_harvest", cropType)
        if (exp > 0) {
            skillManager.addExp(player, SkillType.FARMING, exp)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlantSeed(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val player = event.player
        val item = event.item ?: return
        val block = event.clickedBlock ?: return
        val hand = event.hand ?: return

        if (!skillManager.plugin.isPluginEnabledInWorld(player.world.name)) return

        val customItem = CustomStack.byItemStack(item)
        val customBlock = CustomBlock.byAlreadyPlaced(block)

        if (customItem != null && customBlock != null && customPots.contains(customBlock.getNamespacedID())) {
            val initialAmount = item.amount

            skillManager.plugin.server.scheduler.runTask(skillManager.plugin, Runnable {
                val currentItem = player.inventory.getItem(hand)
                val finalAmount = currentItem?.amount ?: 0

                if (finalAmount < initialAmount) {
                    val seedId = customItem.getNamespacedID()
                    val exp = skillManager.plugin.getExpForFarming("plant_seed", seedId)
                    if (exp > 0) {
                        skillManager.addExp(player, SkillType.FARMING, exp)
                    }
                }
            })
            return
        }

        if (block.type == Material.FARMLAND) {
            val seedType = item.type.name
            val exp = skillManager.plugin.getExpForFarming("plant_seed", seedType)
            if (exp > 0) {
                skillManager.addExp(player, SkillType.FARMING, exp)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onTillLand(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val player = event.player
        val item = event.item ?: return
        val block = event.clickedBlock ?: return

        if (!skillManager.plugin.isPluginEnabledInWorld(player.world.name)) return

        if (item.type.name.endsWith("_HOE") && (block.type == Material.DIRT || block.type == Material.GRASS_BLOCK)) {
            val exp = skillManager.plugin.getExpForFarming("till_land", "")
            if (exp > 0) {
                skillManager.addExp(player, SkillType.FARMING, exp)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBreedAnimal(event: EntityBreedEvent) {
        val breeder = event.breeder as? Player ?: return
        val entityType = event.entity.type.name

        if (!skillManager.plugin.isPluginEnabledInWorld(breeder.world.name)) return

        val exp = skillManager.plugin.getExpForFarming("breed_animal", entityType)
        if (exp > 0) {
            skillManager.addExp(breeder, SkillType.FARMING, exp)
        }
    }
}
