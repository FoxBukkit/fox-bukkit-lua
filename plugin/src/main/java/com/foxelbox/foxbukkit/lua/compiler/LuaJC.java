/**
 * This file is part of FoxBukkitLua.
 *
 * FoxBukkitLua is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FoxBukkitLua is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FoxBukkitLua.  If not, see <http://www.gnu.org/licenses/>.
 */
/*******************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package com.foxelbox.foxbukkit.lua.compiler;

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
        String classname = toStandardJavaClassName(luaname);
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
        return "lua." + classname.toString();
    }

    private static String toStandardLuaFileName(String luachunkname) {
        String filename = toStub(luachunkname).replace('.', '/') + ".lua";
        return filename.startsWith("@") ? filename.substring(1) : filename;
    }

    private static String toStub(String s) {
        return s.endsWith(".lua") ? s.substring(0, s.length() - 4) : s;
    }
}