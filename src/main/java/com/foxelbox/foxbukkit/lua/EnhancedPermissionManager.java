package com.foxelbox.foxbukkit.lua;

import com.foxelbox.foxbukkit.permissions.FoxBukkitPermissionHandler;
import com.foxelbox.foxbukkit.permissions.FoxBukkitPermissions;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class EnhancedPermissionManager {
    private final FoxBukkitPermissionHandler handler;
    //private final LuaState luaState;

    public EnhancedPermissionManager(LuaState luaState, Plugin enhancedPermissionPlugin) {
        try {
            handler = ((FoxBukkitPermissions)enhancedPermissionPlugin).getHandler();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //this.luaState = luaState;
    }

    public int getImmunityLevel(Player ply) {
        return handler.getImmunityLevel(ply);
    }

    public int getImmunityLevel(UUID uuid) {
        return handler.getImmunityLevel(uuid);
    }

    public String getGroup(Player ply) {
        return handler.getGroup(ply);
    }

    public String getGroup(UUID uuid) {
        return handler.getGroup(uuid);
    }

    public boolean isAvailable() {
        return true;
    }
}
