package lightstudio.mmo_plugin.listeners

import lightstudio.mmo_plugin.LightMmo
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.ChatColor

class SkillGuiListener(private val plugin: LightMmo) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        // Check if the clicked inventory is our SkillGui
        val guiTitle = ChatColor.translateAlternateColorCodes('&', plugin.config.getString("gui.title") ?: "My Skills")
        if (event.view.title == guiTitle) {
            event.isCancelled = true // Cancel the event to prevent item movement
        }
    }
}