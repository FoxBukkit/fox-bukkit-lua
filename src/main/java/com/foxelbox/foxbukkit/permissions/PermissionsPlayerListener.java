package com.foxelbox.foxbukkit.permissions;

import com.foxelbox.foxbukkit.core.Utils;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftHumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PermissionsPlayerListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        if(player instanceof CraftHumanEntity) {
            final CraftHumanEntity craftPlayer = (CraftHumanEntity)player;
            Utils.setPrivateValue(CraftHumanEntity.class, craftPlayer, "perm", new FoxBukkitPermissibleBase(player));
        } else {
            player.kickPlayer("You != CraftHumanEntity");
        }
    }
}
