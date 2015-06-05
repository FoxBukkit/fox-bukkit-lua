--[[

    This file is part of FoxBukkitLua.

    FoxBukkitLua is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FoxBukkitLua is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FoxBukkitLua.  If not, see <http://www.gnu.org/licenses/>.

]]

local next = next
local type = type

local bukkitServer = require("Server"):getBukkitServer()

local Location = bindClass("org.bukkit.Location")
local UUID = bindClass("java.util.UUID")

local Class = bindClass("java.lang.Class")
local isAssignableFrom = Class.isAssignableFrom

local serializers = {
    [bindClass("org.bukkit.entity.Player")] = {
        serialize = function(v)
            return v:getUniqueId()
        end,
        unserialize = function(v)
            return bukkitServer:getPlayer(v)
        end
    },
    [UUID] = {
        serialize = function(v)
            return v:toString()
        end,
        unserialize = function(v)
            return UUID:fromString(v)
        end     
    },
    [Location] = {
        serialize = function(v)
            return {
                x = v:getX(),
                y = v:getY(),
                z = v:getZ(),
                yaw = v:getYaw(),
                pitch = v:getPitch(),
                world = v:getWorld():getName()
            }
        end,
        unserialize = function(v)
            local c = luajava.new(Location, bukkitServer:getWorld(v.world), v.x, v.y, v.z)
            c:setYaw(v.yaw)
            c:setPitch(v.pitch)
            return c
        end
    }
}

local function findSerializer(cls)
    if type(cls) == "string" then
        cls = bindClass(cls)
    end
    local serializer = serializers[cls]
    if serializer then
        return serializer, cls
    end
    for tryClass, serializer in next, serializers do
        if isAssignableFrom(tryClass, cls) then
            serializers[cls] = serializer
            return serializer, tryClass
        end
    end   
end

local __SERIALIZABLE = {
    number = true,
    boolean = true,
    string = true,
    userdata = true,
    table = true
}

local __INDENT = "\t"

local function serialize(stream, v, indent)
    local t = type(v)
    local ret
    if t == "number" then
        stream:write(tostring(v))
    elseif t == "boolean" then
        stream:write(v and "true" or "false")
    elseif t == "string" then
        stream:write(string.format("%q", v))
    elseif t == "userdata" then
        t = v:getClass()
        local serializer = findSerializer(t)
        if serializer then
            stream:write("Persister.__findSerializer(")
            serialize(stream, getClassName(t), indent)
            stream:write(").unserialize(")
            serialize(stream, serializer.serialize(v), indent)
            stream:write(")")
        end
    elseif t == "table" then
        if not next(v) then
            return
        end

        local result = {}
        local newIndent = indent .. __INDENT
        stream:write("{\n")
        stream:write(indent)
        local isFirst = true
        for k, kv in next, v do
            if __SERIALIZABLE[type(k)] and __SERIALIZABLE[type(kv)] and
                (type(kv) ~= "table" or next(kv)) and
                (type(k) ~= "table" or next(k))
            then
                if isFirst then
                    isFirst = false
                else
                    stream:write(",\n")
                    stream:write(indent)
                end
                stream:write("[")
                serialize(stream, k, newIndent)
                stream:write("] = ")
                serialize(stream, kv, newIndent)
            end
        end
        stream:write("\n")
        stream:write(indent:sub(2))
        stream:write("}")
    end
end

local persistDir = __LUA_STATE:getModuleDir() .. "/storage/"
luajava.new(bindClass("java.io.File"), persistDir):mkdirs()

local function getPersistFile(hash)
    return persistDir .. hash .. ".lua"
end

local function savePersist(hash, tbl)
    local stream = io.open(getPersistFile(hash), "w")
    stream:write("local Persister = require(\"Persister\")\nreturn ")
    serialize(stream, tbl, __INDENT)
    stream:close()
end

local function loadPersist(hash)
    local stream = io.open(getPersistFile(hash), "r")
    if not stream then
        return {}
    end
    local contents = stream:read("*a")
    stream:close()
    contents = load(contents)
    return contents and contents() or {}
end

local _persist_mt = {
    __index = function(tbl, index)
        if index == "__save" then
            return rawget(tbl, "save")
        end
        return rawget(tbl, "value")[index]
    end,
    __newindex = function(tbl, index, value)
        rawget(tbl, "value")[index] = value
        rawget(tbl, "save")(tbl)
    end,
    __metatable = false
}

return {
    get = function(self, hash)
        local tbl = {
            value = loadPersist(hash),
            save = function(tbl)
                return savePersist(hash, rawget(tbl, "value"))
            end
        }
        return setmetatable(tbl, _persist_mt)
    end,
    __findSerializer = findSerializer
}