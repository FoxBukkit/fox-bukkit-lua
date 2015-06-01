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
package com.foxelbox.foxbukkit.core;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.UUID;

public class Utils {
    public static UUID CONSOLE_UUID = UUID.nameUUIDFromBytes("[CONSOLE]".getBytes());

    public static UUID getCommandSenderUUID(CommandSender commandSender) {
        if(commandSender instanceof Player)
            return ((Player) commandSender).getUniqueId();
        if(commandSender instanceof ConsoleCommandSender)
            return CONSOLE_UUID;
        return UUID.nameUUIDFromBytes(("[CSUUID:" + commandSender.getClass().getName() + "]").getBytes());
    }

    public static String getCommandSenderDisplayName(CommandSender commandSender) {
        if(commandSender instanceof Player)
            return ((Player) commandSender).getDisplayName();
        return commandSender.getName();
    }

    public static <T, E> void setPrivateValue(Class<? super T> instanceclass, T instance, String field, E value) {
        try
        {
            Field field_modifiers = Field.class.getDeclaredField("modifiers");
            field_modifiers.setAccessible(true);

            Field f = instanceclass.getDeclaredField(field);
            int modifiers = field_modifiers.getInt(f);

            if ((modifiers & Modifier.FINAL) != 0)
                field_modifiers.setInt(f, modifiers & ~Modifier.FINAL);

            f.setAccessible(true);
            f.set(instance, value);
        }
        catch (Exception e) {
            System.err.println("Could not set field \"" + field + "\" of class \"" + instanceclass.getCanonicalName() + "\" because \"" + e.getMessage() + "\"");
        }
    }
}
