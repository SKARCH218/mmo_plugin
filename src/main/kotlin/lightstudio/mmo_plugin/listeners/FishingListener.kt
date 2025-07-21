package lightstudio.mmo_plugin.listeners

import lightstudio.mmo_plugin.LightMmo
import lightstudio.mmo_plugin.SkillType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent

class FishingListener(private val plugin: LightMmo) : Listener {

    @EventHandler
    fun onPlayerFish(event: PlayerFishEvent) {
        try {
            when (event.state) {
                PlayerFishEvent.State.CAUGHT_FISH -> {
                    val exp = plugin.config.getInt("exp_gain.fishing.catch", 0)
                    if (exp > 0) {
                        plugin.skillManager.addExp(event.player, SkillType.FISHING, exp)
                    }
                }
                PlayerFishEvent.State.CAUGHT_ENTITY -> {
                    // 보물 또는 쓰레기 낚시 (아이템 종류에 따라 구분 가능)
                    val caughtItem = event.caught as? org.bukkit.entity.Item ?: return
                    val itemType = caughtItem.itemStack.type

                    if (itemType.isItem) { // 간단하게 아이템이면 보물로 간주
                        val exp = plugin.config.getInt("exp_gain.fishing.catch_treasure", 0)
                        if (exp > 0) {
                            plugin.skillManager.addExp(event.player, SkillType.FISHING, exp)
                        }
                    } else { // 그 외는 쓰레기로 간주
                        val exp = plugin.config.getInt("exp_gain.fishing.catch_junk", 0)
                        if (exp > 0) {
                            plugin.skillManager.addExp(event.player, SkillType.FISHING, exp)
                        }
                    }
                }
                else -> {}
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error in FishingListener: " + e.message)
            e.printStackTrace()
        }
    }
}