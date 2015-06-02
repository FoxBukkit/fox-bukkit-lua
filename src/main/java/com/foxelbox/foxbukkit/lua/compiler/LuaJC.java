package com.foxelbox.foxbukkit.lua.compiler;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;

import java.io.IOException;

public class LuaJC implements Globals.Loader {

    public static final LuaJC instance = new LuaJC();

    private static final ClassLoader parentClassLoader = LuaJC.class.getClassLoader();

    public static void install(Globals G) {
        G.loader = instance;
    }

    protected LuaJC() { }

    @Override
    public LuaFunction load(Prototype p, String name, LuaValue globals) throws IOException {
        String luaname = toStandardLuaFileName(name);
        String classname = toStandardJavaClassName(luaname);
        JavaLoader loader = new JavaLoader(parentClassLoader);
        return loader.load(p, classname, luaname, globals);
    }

    private static String toStandardJavaClassName(String luachunkname) {
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