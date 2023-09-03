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

import org.bukkit.event.*;
import org.bukkit.plugin.EventExecutor;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.ArrayList;
import java.util.List;

public class EventManager implements EventExecutor {
    private final LuaState luaState;
    private final List<LuaListener> listeners = new ArrayList<>();

    public EventManager(LuaState luaState) {
        this.luaState = luaState;
    }

    private class LuaListener implements Listener {
        private final LuaValue function;

        public LuaListener(final LuaValue function) {
            this.function = function;
        }

        public synchronized void run(final Event event) {
            final LuaValue ret;
            synchronized (luaState.luaLock) {
                ret = function.call(CoerceJavaToLua.coerce(event));
            }
            // Return true/nonboolean for continue, false for cancel
            if(ret != null && ret.isboolean() && event instanceof Cancellable) {
                boolean retB = ((LuaBoolean)ret).booleanValue();
                ((Cancellable)event).setCancelled(!retB);
            }
        }

        public void unregister() {
            EventManager.this.unregister(this);
        }
    }

    @Override
    public void execute(Listener listener, Event event) throws EventException {
        if(!(listener instanceof LuaListener)) {
            return;
        }

        LuaListener luaListener = (LuaListener)listener;
        luaListener.run(event);
    }

    public Listener register(final Class<? extends Event> eventClass, final EventPriority eventPriority, final boolean ignoreCancelled, final LuaValue function) {
        final LuaListener listener = new LuaListener(function);
        luaState.plugin.getServer().getPluginManager().registerEvent(eventClass, listener, eventPriority, this, luaState.plugin, ignoreCancelled);
        synchronized (listeners) {
            listeners.add(listener);
        }
        return listener;
    }

    public void unregister(LuaListener l) {
        synchronized (listeners) {
            HandlerList.unregisterAll(l);
            listeners.remove(l);
        }
    }

    public void unregisterAll() {
        synchronized (listeners) {
            for (LuaListener l : listeners) {
                HandlerList.unregisterAll(l);
            }
            listeners.clear();
        }
    }
}
