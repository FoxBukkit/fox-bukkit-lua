/**
 * This file is part of FoxBukkit.
 *
 * FoxBukkit is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FoxBukkit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FoxBukkit.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.foxelbox.foxbukkit.permissions;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public class FoxBukkitPermissibleBase extends PermissibleBase {
	private Permissible parent = this;
	private CommandSender parentC = null;
	private ServerOperator opable = null;

	private FoxBukkitPermissionHandler handler;
	private void __init() {
		handler = FoxBukkitPermissionHandler.instance;
	}
	private void __init_end() {
		if(this.parent == null) return;

		if(this.parent instanceof CommandSender) {
			this.parentC = (CommandSender)parent;
		}
		
		recalculatePermissions();
	}
	
	public FoxBukkitPermissibleBase(Permissible parent) {
		super(parent);

		__init();
		
		this.parent = parent;
		
		__init_end();
	}
	
	public FoxBukkitPermissibleBase(ServerOperator opable) {
		super(opable);

		__init();
		
        this.opable = opable;

		if(opable instanceof Permissible) {
			this.parent = (Permissible)opable;
		}
		
		__init_end();
    }

	@Override
	public boolean isOp() {
		if(opable != null)
			return opable.isOp();
		else if(parent != null)
			return parent.isOp();
		else
			return false;
	}

	@Override
	public void setOp(boolean arg0) {
		if(opable != null)
			opable.setOp(arg0);
		else if(parent != null)
			parent.setOp(arg0);
		
		recalculatePermissions();
	}

    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
    	recalculatePermissions();
    	return null;
    }

    public PermissionAttachment addAttachment(Plugin plugin) {
    	recalculatePermissions();
    	return null;
    }

    public void removeAttachment(PermissionAttachment attachment) {
    	recalculatePermissions();
    }
    
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
    	recalculatePermissions();
        return null;
    }

    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
    	recalculatePermissions();
    	return null;
    }

	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions() {
		return new HashSet<>();
	}

	@Override
	public boolean hasPermission(String arg0) {
		if(this.parentC instanceof Player) {
			return handler.has((Player)this.parentC, arg0.toLowerCase());
		} else if(this.parentC instanceof ConsoleCommandSender) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean hasPermission(Permission arg0) {
		return hasPermission(arg0.getName());
	}

	@Override
	public boolean isPermissionSet(String arg0) {
		return true;
	}

	@Override
	public boolean isPermissionSet(Permission arg0) {
		return true;
	}

	@Override
	public void recalculatePermissions() {

	}
}
