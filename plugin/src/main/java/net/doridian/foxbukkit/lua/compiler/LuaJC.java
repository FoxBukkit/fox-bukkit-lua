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

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;

import java.io.IOException;
import java.util.HashMap;

public class LuaJC implements Globals.Loader {
    private static final ClassLoader parentClassLoader = LuaJC.class.getClassLoader();

    private static final HashMap<String, LuaJC> compilers = new HashMap<>();

    private String root;
    private int rootLen;

    public static synchronized void install(Globals G, String root) {
        LuaJC compiler = compilers.get(root);
        if(compiler == null) {
            compiler = new LuaJC(root);
            compilers.put(root, compiler);
        }
        G.loader = compiler;
    }

    protected LuaJC(String root) {
        this.root = root;
        this.rootLen = root.length();
    }

    @Override
    public LuaFunction load(Prototype p, String name, LuaValue globals) throws IOException {
        String luaname = toStandardLuaFileName(name);
        if(luaname.startsWith(root)) {
            luaname = luaname.substring(rootLen + 1);
        }
        String classname = "luamod." + toStandardJavaClassName(luaname);
        JavaLoader loader = new JavaLoader(parentClassLoader);
        return loader.load(p, classname, luaname, globals);
    }

    public static String toStandardJavaClassName(String luachunkname) {
        String stub = toStub(luachunkname);
        StringBuilder classname = new StringBuilder();
        for (int i = 0, n = stub.length(); i < n; ++i) {
            final char c = stub.charAt(i);
            classname.append((((i == 0) && Character.isJavaIdentifierStart(c)) || ((i > 0) && Character.isJavaIdentifierPart(c))) ? c : '_');
        }
        return classname.toString();
    }

    private static String toStandardLuaFileName(String luachunkname) {
        String filename = toStub(luachunkname).replace('.', '/') + ".lua";
        return filename.startsWith("@") ? filename.substring(1) : filename;
    }

    private static String toStub(String s) {
        return s.endsWith(".lua") ? s.substring(0, s.length() - 4) : s;
    }
}