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

local cmdManager = __LUA_STATE:getCommandManager()
local moduleName = __LUA_STATE:getModule()

local Player = require("Player")
local Server = require("Server")
local Permission = require("Permission")

local next = next
local tonumber = tonumber

local basePermission = "foxbukkit." .. moduleName

local function makeArgMaxImmunity(self, ply)
    if not ply or not self.immunityRequirement or not Permission:isAvailable() then
        return
    end
    return self.immunityRequirement
end

local argTypes = {
    string = {
        parser = function(self, arg)
            return arg
        end,
        default = ""
    },
    number = {
        parser = function(self, arg)
            return tonumber(arg)
        end,
        default = 0
    },
    player = {
        parser = function(self, arg, ply, cmd)
            local ret = Player:findSingle(arg, self.noMatchSelf and ply or nil, makeArgMaxImmunity(self, ply), ply)

            if ret ~= ply and cmd.permissionOther and not ply:hasPermission(cmd.permissionOther) then
                return
            end

            return ret
        end,
        default = function(self, ply)
            if self.defaultSelf then
                return ply
            end
        end
    },
    players = {
        parser = function(self, arg, cmd)
            local ret = Player:find(arg, self.noMatchSelf and ply or nil, makeArgMaxImmunity(self, ply), ply)

            if cmd.permissionOther and not ply:hasPermission(cmd.permissionOther) then
                local found = false
                for _, v in next, ret do
                    if v == ply then
                        found = true
                        break
                    end
                end

                if found then
                    return {ply}
                else
                    return {}
                end
            end

            return ret
        end,
        default = function(self, ply)
            if self.defaultSelf then
                return {ply}
            else
                return {}
            end
        end
    },
    enum = {
        parser = function(self, arg)
            local argNumber = tonumber(arg)
            local enum = self.enum
            if argNumber ~= nil then
                local func = self.enumIdFunction
                if func then
                    return func(enum, argNumber)
                end
                return enum:values()[argNumber + 1]
            end
            local func = self.enumNameFunction
            if func then
                return func(enum, arg)
            end
            return enum[arg:upper()]
        end
    }
}

local class

local _command_mt = {
    __index = {
        register = function(self)
            return class:register(self)
        end,

        unregister = function(self)
            return class:unregister(self)
        end,

        getSubPermission = function(self, sub)
            return self.permission .. "." .. sub
        end,

        sendActionReply = function(self, ply, target, overrides, ...)
            overrides = overrides or {}
            
            local format = overrides.format or self.action.format
            local isProperty = overrides.isProperty
            if isProperty == nil then
                 isProperty = self.action.isProperty
            end

            if ply == target then
                ply:sendReply(format:format("You", isProperty and "your own" or "yourself", ...))
                return
            end
            ply:sendReply(format:format("You", isProperty and (target:getName() .. "'s") or target:getName(), ...))
            target:sendReply(format:format(ply:getName(), isProperty and "your" or "you", ...))

            local broadcast = overrides.broadcast
            if broadcast == nil then
                broadcast = self.action.broadcast
            end
            if broadcast then
                local players
                if type(broadcast) == "string" then
                    players = Player:getAll(nil, nil, nil, broadcast)
                else
                    players = Player:getAll()
                end

                for _, otherply in next, players do
                    if otherply ~= ply and otherply ~= target then
                        otherply:sendReply(format:format(ply:getName(), isProperty and (target:getName() .. "'s") or target:getName(), ...))                     
                    end
                end
            end
        end
    },
    __newindex = function()
        error("Readonly")
    end,
    __metatable = false
}

class = {
    register = function(self, cmd)
        if not cmd.__info then
            cmd.__info = {}
            cmd.permission = cmd.permission or (basePermission .. "." .. cmd.name)
            if cmd.permissionOther == nil or cmd.permissionOther == true then
                cmd.permissionOther = cmd.permission .. ".other"
            end

            if cmd.arguments then
                cmd.__info.lastRequiredArgument = 0
                for k, options in pairs(cmd.arguments) do
                    options.required = (options.required ~= false)
                    options.type = (options.type or "string"):lower()
                    local argType = argTypes[options.type] or argTypes.string
                    options.parser = options.parser or argType.parser
                    options.default = options.default or argType.default
                    options.aliases = options.aliases or argType.aliases or {}
                    if options.required then
                        cmd.__info.lastRequiredArgument = k
                    end
                    cmd.arguments[k] = options
                end
            end
            cmd = setmetatable(cmd, _command_mt)
        end

        local executor = function(ply, cmdStr, args, argStr, flagStr)
            if ply and ply.getUniqueId then
                ply = Player:extend(ply)
            end

            local parsedArgs
            if cmd.arguments then
                parsedArgs = {}
                local currentFitArg = 1
                local tryArg = cmd.arguments[currentFitArg]
                for _, arg in next, args do
                    if not tryArg then
                        ply:sendReply("Too many arguments")
                        return
                    end
                    local argAlias = tryArg.aliases[arg]
                    if argAlias ~= nil then
                        arg = argAlias
                    else
                        arg = tryArg:parser(arg, ply, cmd)
                    end
                    if arg == nil then
                        ply:sendReply("Could not find match for argument \"" .. tryArg.name .. "\"")
                        return
                    end
                    parsedArgs[tryArg.name] = arg
                    currentFitArg = currentFitArg + 1
                    tryArg = cmd.arguments[currentFitArg]
                end
                if currentFitArg <= cmd.__info.lastRequiredArgument then
                    ply:sendReply("Not enough arguments")
                    return
                end
                for i = currentFitArg, #cmd.arguments do
                    tryArg = cmd.arguments[i]
                    if type(tryArg.default) == "function" then
                        parsedArgs[tryArg.name] = tryArg:default(ply, cmd)
                    else
                        parsedArgs[tryArg.name] = tryArg.default
                    end
                end
            else
                parsedArgs = args
            end

            return cmd:run(ply, parsedArgs, flagStr, argStr, cmdStr)
        end

        cmdManager:register(cmd.name, cmd.permission, executor)
        if cmd.aliases then
            for _, cmdAlias in pairs(cmd.aliases) do
                cmdManager:register(cmdAlias, cmd.permission, executor)
            end
        end

        return cmd
    end,
    unregister = function(self, cmd)
        if type(cmd) == "string" then
            cmdManager:unregister(cmd)
        elseif cmd.name then
            cmdManager:unregister(cmd.name)
            if cmd.aliases then
                for _, cmdAlias in pairs(cmd.aliases) do
                    cmdManager:unregister(cmdAlias)
                end
            end
        else
            for _, v in pairs(cmd) do
                self:unregister(cmd)
            end
        end
    end
}

return class