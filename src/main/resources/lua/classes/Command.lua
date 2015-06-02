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

local basePermission = "foxbukkit." .. moduleName

local Player = require("Player")
local Server = require("Server")
local Permission = require("Permission")

local next = next

local _flags_mt = {
    __index = {
        has = function(self, flag)
            return self.str:find(flag, 1, true) ~= nil
        end
    },
    __newindex = function()
        error("Readonly")
    end,
    __metatable = false
}

local function makeArgMaxImmunity(self, ply)
    if not ply or not self.immunityRequirement or not Permission:isAvailable() then
        return
    end
    return self.immunityRequirement
end

local parsers = {
    string = function(self, arg)
        return arg
    end,
    number = function(self, arg)
        return tonumber(arg)
    end,
    player = function(self, arg, ply, cmd)
        local ret = Player:findSingle(arg, self.noMatchSelf and ply or nil, makeArgMaxImmunity(self, ply), ply)

        if ret ~= ply and cmd.permissionOther and not ply:hasPermission(cmd.permissionOther) then
            return
        end

        return ret
    end,
    players = function(self, arg, cmd)
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
    enum = function(self, arg)
        return self.enum[arg:upper()]
    end
}

local defaults = {
    string = "",
    number = 0,
    player = function(self, arg, ply)
        if self.defaultSelf then
            return ply
        end
    end,
    players = function(self, arg, ply)
        if self.defaultSelf then
            return {ply}
        else
            return {}
        end
    end
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
        end
    },
    __newindex = function()
        error("Readonly")
    end,
    __metatable = false
}

class = {
    register = function(self, cmd)
        if not cmd.__fullySetUp then
            cmd.permission = cmd.permission or (basePermission .. "." .. cmd.name)
            if cmd.permissionOther == nil or cmd.permissionOther == true then
                cmd.permissionOther = cmd.permission .. ".other"
            end

            if cmd.arguments then
                cmd.lastRequiredArgument = 0
                for k, options in pairs(cmd.arguments) do
                    options.required = (options.required ~= false)
                    options.type = (options.type or "string"):lower()
                    options.parser = options.parser or parsers[options.type] or parsers.string
                    options.default = options.default or defaults[options.type]
                    if options.required then
                        cmd.lastRequiredArgument = k
                    end
                    cmd.arguments[k] = options
                end
            end
            cmd.__fullySetUp = true
            cmd = setmetatable(cmd, _command_mt)
        end

        local executor = function(ply, cmdStr, args, argStr, flagStr)
            flagStr = setmetatable({
                str = flagStr
            }, _flags_mt)

            if ply and ply.getUniqueId then
                ply = Player:extend(ply)
            end

            local parsedArgs
            if cmd.arguments then
                parsedArgs = {}
                local currentFitArg = 1
                local tryArg = cmd.arguments[currentFitArg]
                for _, v in next, args do
                    if not tryArg then
                        ply:sendReply("Too many arguments (or unfitting optionals)")
                        return
                    end
                    v = tryArg:parser(v, ply, cmd)
                    if v == nil then
                        ply:sendReply("Could not find match for argument \"" .. tryArg.name .. "\"")
                        return
                    end
                    parsedArgs[tryArg.name] = v
                    currentFitArg = currentFitArg + 1
                    tryArg = cmd.arguments[currentFitArg]
                end
                if currentFitArg <= cmd.lastRequiredArgument then
                    ply:sendReply("Not enough arguments")
                    return
                end
                for i = currentFitArg, #cmd.arguments do
                    tryArg = cmd.arguments[i]
                    if type(tryArg.default) == "function" then
                        parsedArgs[tryArg.name] = tryArg:default(v, ply, cmd)
                    else
                        parsedArgs[tryArg.name] = tryArg.default
                    end
                end
            else
                parsedArgs = args
            end

            return cmd:run(ply, cmdStr, parsedArgs, argStr, flagStr)
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