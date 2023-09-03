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
package net.doridian.foxbukkit.lua.compiler;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.luajc.JavaGen;

import java.util.HashMap;
import java.util.Map;

public class JavaLoader extends ClassLoader {

    private Map<String,byte[]> unloaded = new HashMap<>();

    public JavaLoader(ClassLoader parent) {
        super(parent);
    }

    public LuaFunction load(Prototype p, String classname, String filename, LuaValue env) {
        return load(new JavaGen(p, classname, filename, false), env);
    }

    public LuaFunction load(JavaGen jg, LuaValue env) {
        include(jg);
        return load(jg.classname, env);
    }

    public LuaFunction load(String classname, LuaValue env) {
        try {
            LuaFunction v = (LuaFunction)loadClass(classname).newInstance();
            v.initupvalue1(env);
            return v;
        } catch ( Exception e ) {
            e.printStackTrace();
            throw new IllegalStateException("bad class gen: "+e);
        }
    }

    public void include(JavaGen jg) {
        unloaded.put(jg.classname, jg.bytecode);
        for(int i = 0, n = ((jg.inners != null) ? jg.inners.length : 0); i < n; i++) {
            include(jg.inners[i]);
        }
    }

    public Class findClass(String classname) throws ClassNotFoundException {
        byte[] bytes = unloaded.get(classname);
        if (bytes != null) {
            return defineClass(classname, bytes, 0, bytes.length);
        }
        return super.findClass(classname);
    }

}