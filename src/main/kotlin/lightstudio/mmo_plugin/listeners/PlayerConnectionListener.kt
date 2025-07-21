package lightstudio.mmo_plugin.listeners

import lightstudio.mmo_plugin.LightMmo
import lightstudio.mmo_plugin.SkillType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerConnectionListener(private val plugin: LightMmo) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        // 플레이어 접속 시 모든 스킬 데이터를 캐시에 로드 (SkillCache의 get()이 로드 처리)
        SkillType.values().forEach { skillType ->
            plugin.skillManager.getLevel(player.uniqueId.toString(), skillType)
            plugin.skillManager.getExp(player.uniqueId.toString(), skillType)
        }
        plugin.logger.info("Player ${player.name} skill data loaded into cache.")
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        // 플레이어 종료 시 모든 스킬 데이터를 DB에 저장하고 캐시에서 제거
        plugin.skillManager.saveAllPlayerSkills(player.uniqueId.toString())
            .thenRun { plugin.logger.info("Player ${player.name} skill data saved and removed from cache.") }
            .exceptionally { ex ->
                plugin.logger.severe("Error saving skill data for player ${player.name}: ${ex.message}")
                ex.printStackTrace()
                null
            }
    }
}