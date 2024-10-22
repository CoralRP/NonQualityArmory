package me.zombie_striker.qg.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class BulletproofTask extends BukkitRunnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack chestPlate = player.getInventory().getChestplate();

            if (chestPlate == null || chestPlate.getType().isAir()) continue;
            if (chestPlate.getType() != Material.DIAMOND_CHESTPLATE) continue;

            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 0));
        }
    }
}
