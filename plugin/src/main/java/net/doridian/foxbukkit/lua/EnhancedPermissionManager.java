/*
 * foxbukkit-lua-plugin - ${project.description}
 * Copyright © ${year} Doridian (git@doridian.net)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.doridian.foxbukkit.lua;

import com.foxelbox.foxbukkit.permissions.FoxBukkitPermissionHandler;
import com.foxelbox.foxbukkit.permissions.FoxBukkitPermissions;
import org.bukkit.command.CommandSender;
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

    public void setGroup(Player ply, String group) {
        setGroup(ply.getUniqueId(), group);
    }

    public void setGroup(UUID uuid, String group) {
        handler.setGroup(uuid, group);
    }

    public int getImmunityLevel(Player ply) {
        return handler.getImmunityLevel(ply);
    }

    public int getImmunityLevel(CommandSender commandSender) { return 9999; }

    public int getImmunityLevel(UUID uuid) {
        return handler.getImmunityLevel(uuid);
    }

    public int getImmunityLevel(String group) {
        return handler.getImmunityLevel(group);
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
