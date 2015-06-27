--[[

    This file is part of FoxBukkitLua-lua.

    FoxBukkitLua-lua is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FoxBukkitLua-lua is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FoxBukkitLua-lua.  If not, see <http://www.gnu.org/licenses/>.

]]

local Player = require("Player")
local Server = require("Server")
local Permission = require("Permission")
require("Chat")

local cmdManager = __LUA_STATE:getCommandManager()
local moduleName = __LUA_STATE:getModule()

local next = next
local tonumber = tonumber
local type = type

local table_insert = table.insert
local table_concat = table.concat
local table_unpack = table.unpack
local table_contains = table.contains

local basePermission = "foxbukkit." .. moduleName

local function makeArgMaxImmunity(self, ply)
    if not ply or not self.immunityRequirement or not Permission:isAvailable() then
        return
    end
    return self.immunityRequirement
end

local BOOL_VALUES = {
    ["false"] = false,
    ["0"] = false,
    ["no"] = false,
    ["n"] = false,
    ["off"] = false,
    ["disabled"] = false,
    ["disable"] = false,

    ["true"] = true,
    ["1"] = true,
    ["yes"] = true,
    ["y"] = true,
    ["on"] = true,
    ["enabled"] = true,
    ["enable"] = true,

    [""] = false
}

local function makePlayerFindConstraint(self, arg, ply, cmd)
    local constraints = {}
    table_insert(constraints, Player.constraints.matchName(arg))
    if self.noMatchSelf then
        table_insert(constraints, Player.constraints.excludePlayer(ply))
    end
    local immunityRequirement = makeArgMaxImmunity(self, ply)
    if immunityRequirement then
        table_insert(constraints, Player.constraints.immunityRestrictionPlayer(ply, immunityRequirement))
    end
    local forbidMultiple = (arg == "" or arg:sub(1, 1) ~= "*")
    if cmd.permissionOther and not ply:hasPermission(cmd.permissionOther) then
        table_insert(constraints, Player.constraints.matchPlayer(ply))
    end
    return Player.constraints.andConstraint(constraints), forbidMultiple
end

local argTypes = {
    string = {
        parser = function(self, arg)
            return arg
        end
    },
    boolean = {
        parser = function(self, arg)
            return BOOL_VALUES[arg]
        end
    },
    number = {
        parser = function(self, arg)
            return tonumber(arg)
        end
    },
    player = {
        parser = function(self, arg, ply, cmd)
            return Player:findSingle(makePlayerFindConstraint(self, arg, ply, cmd))
        end,
        default = function(self, ply)
            if self.defaultSelf then
                return ply
            end
        end
    },
    players = {
        parser = function(self, arg, ply, cmd)
            return Player:find(makePlayerFindConstraint(self, arg, ply, cmd))
        end,
        default = function(self, ply)
            if self.defaultSelf then
                return {ply}
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

        makePlayerFindConstraint = makePlayerFindConstraint,

        unregister = function(self)
            return class:unregister(self)
        end,

        getSubPermission = function(self, sub)
            return self.permission .. "." .. sub
        end,

        sendActionReply = function(self, ply, target, overrides, ...)
            overrides = overrides or {}

            if target and target.__entity then
                target = {target}
            end

            local format = overrides.format or self.action.format
            local isProperty = overrides.isProperty
            if isProperty == nil then
                isProperty = self.action.isProperty
            end

            local containsSelf = false
            local function referToTarget(target, sendTo)
                if target == ply then
                    if containsSelf then
                        if sendTo == ply then
                            return isProperty and "your own" or "yourself"
                        else
                            return isProperty and "their own" or "themselves"
                        end
                    else
                        containsSelf = true
                        if sendTo == target then
                            return "you"
                        else
                            return target:getName()
                        end
                    end
                elseif sendTo == target then
                    return isProperty and "your" or "you"
                else
                    return isProperty and (target:getName() .. "'s") or target:getName()
                end
            end
            local function referToTargets(targets, sendTo)
                local str = {}
                local oldCS = containsSelf
                local newCS = containsSelf
                for _, target in next, targets do
                    containsSelf = oldCS
                    table_insert(str, referToTarget(target, sendTo))
                    newCS = newCS or containsSelf
                end
                containsSelf = containsSelf or newCS
                return table_concat(str, ", ")
            end
            local function doFormat(sendTo, ...)
                containsSelf = false
                local args = {...}
                for k, v in next, args do
                    local arg = args[k]
                    local targ = type(arg)
                    if targ == "table" or targ == "userdata" then
                        if arg.getName then
                            arg = referToTarget(arg, sendTo)
                        else
                            arg = referToTargets(arg, sendTo)
                        end
                        if (arg == "you" or arg:sub(1, 4) == "you,") and k == 1 then
                            arg = "Y" .. arg:sub(2)
                        end
                    end
                    args[k] = arg
                end
                sendTo:sendReply(format:format(table_unpack(args)))
            end

            if not target then
                doFormat(ply, ply, ...)
            else
                doFormat(ply, ply, target, ...)
                if not overrides.silentToTarget  then
                    for _, targetPly in next, target do
                        if targetPly ~= ply then
                            doFormat(targetPly, ply, target, ...)
                        end
                    end
                end
            end

            local broadcast = overrides.broadcast
            if broadcast == nil then
                broadcast = self.action.broadcast
            end
            if broadcast then
                if broadcast == true then
                    broadcast = overrides.silent and self.action.broadcastSilentPermission or self.action.broadcastPermission
                end

                local players
                if type(broadcast) == "string" then
                    players = Player:find(Player.contraints.permissionRestriction(broadcast))
                else
                    players = Player:getAll()
                end

                for _, otherply in next, players do
                    if otherply ~= ply then
                        if target then
                            if not table_contains(target, otherply) then
                                doFormat(otherply, ply, target, ...)
                            end
                        else
                            doFormat(otherply, ply, ...)
                        end
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

            if cmd.action then
                cmd.action.isProperty = cmd.action.isProperty or false
                cmd.action.broadcast = cmd.action.broadcast or false
                if cmd.action.broadcastPermission == nil or cmd.action.broadcastPermission == true then
                    cmd.action.broadcastPermission = cmd.permission .. ".broadcast"
                end
                if cmd.action.broadcastPermission == false then
                    cmd.action.broadcastSilentPermission = cmd.permission .. ".broadcast.silent"
                elseif cmd.action.broadcastSilentPermission == nil or cmd.action.broadcastSilentPermission == true then
                    cmd.action.broadcastSilentPermission = cmd.action.broadcastPermission .. ".silent"
                end
            end

            if cmd.arguments then
                for k, options in next, cmd.arguments do
                    options.required = (options.required ~= false)
                    options.type = (options.type or "string"):lower()
                    local argType = argTypes[options.type] or argTypes.string
                    options.parser = options.parser or argType.parser
                    if options.default == nil then
                        options.default = argType.default
                    end
                    options.aliases = options.aliases or argType.aliases or {}
                    cmd.arguments[k] = options
                end
            end
            cmd = setmetatable(cmd, _command_mt)
        end

        local executor = function(ply, cmdStr, args, argStr, flagStr)
            if ply and ply.getUniqueId then
                ply = Player:extend(ply)
            else
                ply = Player:getConsole()
            end

            if not cmd.noLogging then
                local str = ply:getName() .. " executed /" .. cmdStr .. " " .. argStr
                print(str)
            end

            local parsedArgs
            if cmd.arguments then
                parsedArgs = {}

                local function pushArg(arg, value)
                    parsedArgs[arg.name] = value
                end

                local function argApplicable(arg)
                    if arg.flagsRequired and not flagStr:contains(arg.flagsRequired) then
                        return false
                    end
                    if arg.flagsForbidden and flagStr:contains(arg.flagsForbidden) then
                        return false
                    end
                    return true
                end

                local currentFitArg = 1
                local tryArg = cmd.arguments[currentFitArg]
                for _, arg in next, args do
                    while tryArg and not argApplicable(tryArg) do
                        currentFitArg = currentFitArg + 1
                        tryArg = cmd.arguments[currentFitArg]
                    end

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
                    if arg == nil or (type(arg) == "table" and next(arg) == nil) then
                        ply:sendReply("Could not find match for argument \"" .. tryArg.name .. "\"")
                        return
                    end

                    pushArg(tryArg, arg)

                    currentFitArg = currentFitArg + 1
                    tryArg = cmd.arguments[currentFitArg]
                end
                for i = currentFitArg, #cmd.arguments do
                    tryArg = cmd.arguments[i]
                    if argApplicable(tryArg) then
                        if tryArg.required then
                            ply:sendReply("Not enough arguments")
                            return
                        elseif type(tryArg.default) == "function" then
                            pushArg(tryArg, tryArg:default(ply, cmd))
                        else
                            pushArg(tryArg, tryArg.default)
                        end
                    end
                end
            else
                parsedArgs = args
            end

            return cmd:run(ply, parsedArgs, flagStr, argStr, cmdStr)
        end

        local info = {
            permission = cmd.permission,
            help = cmd.help,
            usage = cmd.usage
        }
        if not info.usage and cmd.arguments then
            local mainUsage = {}
            local availableFlags = {
                [false] = true
            }
            for _, v in next, cmd.arguments do
                if v.flagsForbidden then
                    for i = 1, v.flagsForbidden:len() do
                        availableFlags[v.flagsForbidden:sub(i,i)] = true
                    end
                elseif v.flagsRequired then
                    for i = 1, v.flagsRequired:len() do
                        availableFlags[v.flagsRequired:sub(i,i)] = true
                    end
                end
            end
            for flag, _ in next, availableFlags do
                local usage = {"/" .. cmd.name .. (flag and (" -" .. flag) or "")}
                for _, v in next, cmd.arguments do
                    if
                    (not v.flagsForbidden or not flag or not v.flagsForbidden:contains(flag)) and
                            (not v.flagsRequired or (flag and v.flagsRequired:contains(flag)))
                    then
                        table_insert(usage, (v.required and "&lt;" or "[") .. v.name .. (v.required and "&gt;" or "]"))
                    end
                end
                table_insert(mainUsage, table_concat(usage, " "))
            end
            info.usage = table_concat(mainUsage, "\n")
        end

        cmdManager:register(cmd.name, cmd.permission, executor, info)
        if cmd.aliases then
            for _, cmdAlias in next, cmd.aliases do
                cmdManager:register(cmdAlias, cmd.permission, executor, info)
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
                for _, cmdAlias in next, cmd.aliases do
                    cmdManager:unregister(cmdAlias)
                end
            end
        else
            for _, v in next, cmd do
                self:unregister(v)
            end
        end
    end,
    getCommands = function(self)
        return cmdManager:getCommands()
    end,
    getInfo = function(self, cmd)
        return cmdManager:getInfo(cmd)
    end
}

return class