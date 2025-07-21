package lightstudio.mmo_plugin.gui

import lightstudio.mmo_plugin.LightMmo
import lightstudio.mmo_plugin.SkillType
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.ChatColor
import java.util.concurrent.CompletableFuture

class SkillGui(private val plugin: LightMmo) {

    fun open(player: Player) {
        val guiTitle = ChatColor.translateAlternateColorCodes('&', plugin.config.getString("gui.title") ?: "My Skills")
        val guiSize = plugin.config.getInt("gui.size", 9)
        val inv: Inventory = Bukkit.createInventory(null, guiSize, guiTitle)

        val futures = mutableListOf<CompletableFuture<Void>>()

        SkillType.values().forEach { skillType ->
            val skillConfigPath = "gui.items.${skillType.name.lowercase()}"
            val materialName = plugin.config.getString("$skillConfigPath.material") ?: skillType.name.uppercase()
            val material = if (plugin.config.getBoolean("gui.items-all-structure-void", false)) {
                Material.STRUCTURE_VOID
            } else {
                Material.getMaterial(materialName) ?: Material.BARRIER
            }
            val displayName = ChatColor.translateAlternateColorCodes('&', plugin.config.getString("$skillConfigPath.display_name") ?: skillType.name)
            val loreConfig = plugin.config.getStringList("$skillConfigPath.lore") ?: emptyList()
            val slot = plugin.config.getInt("$skillConfigPath.slot")

            val future = plugin.skillManager.getLevel(player.uniqueId.toString(), skillType)
                .thenCombine(plugin.skillManager.getExp(player.uniqueId.toString(), skillType)) { skillLevel, skillExp ->
                    val requiredExp = plugin.skillManager.calculateRequiredExp(skillLevel)
                    val remainingExp = requiredExp - skillExp

                    val lore = loreConfig.map { line ->
                        ChatColor.translateAlternateColorCodes('&', line
                            .replace("{level}", skillLevel.toString())
                            .replace("{exp}", skillExp.toString())
                            .replace("{required_exp}", requiredExp.toString())
                            .replace("{remaining_exp}", remainingExp.toString()))
                    }

                    val item = ItemStack(material)
                    item.itemMeta = item.itemMeta?.apply {
                        setDisplayName(displayName)
                        setLore(lore)
                    }
                    inv.setItem(slot, item)
                }
            futures.add(future.thenApply { null })
        }

        CompletableFuture.allOf(*futures.toTypedArray()).thenRun { Bukkit.getScheduler().runTask(plugin, Runnable { player.openInventory(inv) }) }
    }
}