--[[

    foxbukkit-lua-lua - ${project.description}
    Copyright © ${year} Doridian (git@doridian.net)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

]]
local next = next
local type = type
local rawget = rawget

local Configuration = require("Configuration")
local devMode = Configuration:get("development", "0")
local _HUMAN_READABLE = (devMode == "1")
local _GZIP_COMPRESS = (devMode ~= "1")

local bukkitServer = require("Server"):getBukkitServer()

local Location = bindClass("org.bukkit.Location")
local UUID = bindClass("java.util.UUID")

local StringBuilder = bindClass("java.lang.StringBuilder")
local Class = bindClass("java.lang.Class")
local isAssignableFrom = Class.isAssignableFrom

local Scanner = bindClass("java.util.Scanner")
local File = bindClass("java.io.File")
local FileInputStream = bindClass("java.io.FileInputStream")
local FileOutputStream = bindClass("java.io.FileOutputStream")
local GZIPInputStream = bindClass("java.util.zip.GZIPInputStream")
local GZIPOutputStream = bindClass("java.util.zip.GZIPOutputStream")

local serializers = {
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
                world = v:getWorld() and v:getWorld():getName() or "world"
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
    if t == "number" then
        stream:append(tostring(v))
    elseif t == "boolean" then
        stream:append(v and "true" or "false")
    elseif t == "string" then
        stream:append(string.format("%q", v))
    elseif t == "userdata" then
        t = v:getClass()
        local serializer = findSerializer(t)
        if serializer then
            stream:append("u(")
            serialize(stream, getClassName(t), indent)
            stream:append(",")
            serialize(stream, serializer.serialize(v), indent)
            stream:append(")")
        end
    elseif t == "table" then
        local newIndent = indent .. __INDENT

        local isFirst = true
        for k, kv in next, v do
            local tk = type(k)
            local tkv = type(kv)
            if __SERIALIZABLE[tk] and __SERIALIZABLE[tkv] and
                (tkv ~= "table" or next(kv)) and
                (tk ~= "table" or next(k)) and
                (tk ~= "string" or k:sub(1,1) ~= "_")
            then
                local pos = stream:length()
                local wasFirst = isFirst
                if isFirst then
                    isFirst = false
                    stream:append("{")
                else
                    stream:append(",")
                end
                if _HUMAN_READABLE then
                    stream:append("\n")
                    stream:append(indent)
                end
                stream:append("[")
                serialize(stream, k, newIndent)
                stream:append("]=")
                if serialize(stream, kv, newIndent) == false then
                    stream:setLength(pos)
                    isFirst = wasFirst
                end
            end
        end

        if isFirst then
            return false
        end

        if _HUMAN_READABLE then
            stream:append("\n")
            stream:append(indent:sub(2))
        end
        
        stream:append("}")
    end
end

local moduleName = __LUA_STATE:getModule()
local persistDir = __LUA_STATE:getModuleDir() .. "/storage/"
luajava.new(File, persistDir):mkdirs()

local function getPersistFile(hash)
    return persistDir .. hash .. ".lua"
end

local function savePersist(hash, tbl)
    local stream = luajava.new(StringBuilder)
    serialize(stream, tbl, __INDENT)
    local file = luajava.new(FileOutputStream, getPersistFile(hash))
    if _GZIP_COMPRESS then
        file:write(66)
        file = luajava.new(GZIPOutputStream, file)
    else
        file:write(65)
    end
    file:write(stream:toString())
    file:close()
end

local loaderEnv = {
    u = function(class, data)
        return findSerializer(class).unserialize(data)
    end
}

local function loadPersist(hash)
    local file = luajava.new(File, getPersistFile(hash))
    if not file:exists() then
        return {}
    end
    local file = luajava.new(FileInputStream, file)
    local storeType = file:read()
    if storeType == 65 then
    elseif storeType == 66 then
        file = luajava.new(GZIPInputStream, file)
    elseif storeType < 0 then
        file:close()
        return {}
    else
        file:close()
        print("ERROR: Invalid type: " .. moduleName .. "|" .. hash .. "|" .. tostring(storeType))
        return {}
    end

    local contents = luajava.new(Scanner, file)
    contents:useDelimiter("\\A")
    if not contents:hasNext() then
        file:close()
        return {}
    end
    contents = contents:next()
    file:close()

    local loaderFunc, err = load("return " .. contents, "__persister_temp." .. moduleName .. "." .. hash, "t", loaderEnv)
    if not loaderFunc then
        print("ERROR: Load error: " .. moduleName .. "|" .. hash .. "|" .. tostring(err))
        return {}
    end
    return loaderFunc and loaderFunc() or {}
end

local _persist_mt = {
    __index = function(tbl, index)
        if type(index) == "string" and index:sub(1,2) == "__" then
            return rawget(tbl, index:sub(3))
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
    get = function(self, hash, loader)
        local tbl = {
            value = loadPersist(hash),
            save = function(tbl)
                return savePersist(hash, rawget(tbl, "value"))
            end
        }
        if loader then loader(tbl) end
        return setmetatable(tbl, _persist_mt)
    end
}