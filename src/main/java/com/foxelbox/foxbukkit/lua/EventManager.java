package com.foxelbox.foxbukkit.lua;

import com.foxelbox.foxbukkit.core.FoxBukkit;
import org.bukkit.event.*;
import org.bukkit.plugin.EventExecutor;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaUserdata;
import org.luaj.vm2.LuaValue;

public class EventManager {
    protected static int lID = 0;

    private final LuaThread luaThread;

    public EventManager(LuaThread luaThread) {
        this.luaThread = luaThread;
    }

    public Listener register(final Class<? extends Event> eventClass, final EventPriority eventPriority, final boolean b, final LuaFunction function) {
        final Listener l = new Listener() {
            private int id = lID++;
            @Override
            public int hashCode() {
                return id;
            }
        };

        FoxBukkit.instance.getServer().getPluginManager().registerEvent(eventClass, l, eventPriority, new EventExecutor() {
            @Override
            public void execute(Listener listener, final Event event) throws EventException {
                if(listener != l) {
                    return;
                }

                luaThread.invoke(new Runnable() {
                    @Override
                    public void run() {
                        LuaValue ret = function.call(new LuaUserdata(event));
                        // Return true/nonboolean for continue, false for cancel
                        if(ret != null && ret.isboolean() && event instanceof Cancellable) {
                            boolean retB = ((LuaBoolean)ret).booleanValue();
                            ((Cancellable)event).setCancelled(!retB);
                        }
                    }
                });
            }
        }, FoxBukkit.instance, b);

        return l;
    }

    public void unregister(Listener l) {
        HandlerList.unregisterAll(l);
    }
}
