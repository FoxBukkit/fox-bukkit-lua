package com.foxelbox.foxbukkit.lua;

import com.foxelbox.foxbukkit.core.FoxBukkit;
import org.bukkit.event.*;
import org.bukkit.plugin.EventExecutor;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

public class EventManager implements EventExecutor {
    protected static int lID = 0;

    private final LuaThread luaThread;

    public EventManager(LuaThread luaThread) {
        this.luaThread = luaThread;
    }

    private class LuaEventInvoker extends LuaThread.Invoker {
        private Event event;
        private final LuaFunction function;

        public LuaEventInvoker setEvent(Event event) {
            this.event = event;
            return this;
        }

        public LuaEventInvoker(LuaThread luaThread, LuaFunction function) {
            super(luaThread);
            this.function = function;
        }

        @Override
        protected LuaValue invoke() {
            return function.call(CoerceJavaToLua.coerce(event));
        }
    }

    private class LuaListener implements Listener {
        private final int id = lID++;
        private final LuaEventInvoker invoker;

        public LuaListener(final LuaFunction function) {
            this.invoker = new LuaEventInvoker(luaThread, function);
        }

        public void run(final Event event) {
            invoker.reset();
            invoker.setEvent(event);
            final LuaValue ret = invoker.getResult();

            // Return true/nonboolean for continue, false for cancel
            if(ret != null && ret.isboolean() && event instanceof Cancellable) {
                boolean retB = ((LuaBoolean)ret).booleanValue();
                ((Cancellable)event).setCancelled(!retB);
            }
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LuaListener that = (LuaListener) o;

            return id == that.id;

        }
    }

    @Override
    public void execute(Listener listener, Event event) throws EventException {
        if(!(listener instanceof LuaListener))
            return;

        LuaListener luaListener = (LuaListener)listener;
        luaListener.run(event);
    }

    public Listener register(final Class<? extends Event> eventClass, final EventPriority eventPriority, final boolean b, final LuaFunction function) {
        final Listener listener = new LuaListener(function);
        FoxBukkit.instance.getServer().getPluginManager().registerEvent(eventClass, listener, eventPriority, this, FoxBukkit.instance, b);
        return listener;
    }

    public void unregister(Listener l) {
        HandlerList.unregisterAll(l);
    }
}
