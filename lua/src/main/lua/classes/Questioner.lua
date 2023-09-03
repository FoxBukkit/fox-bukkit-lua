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
local Chat = require("Chat")
local Player = require("Player")
local Command = require("Command")
local Event = require("Event")

local table_insert = table.insert
local table_concat = table.concat
local next = next

if not Chat:isAvailable() then
    return {
        isAvailable = function(self)
            return false
        end
    }
end

Event:register{
    class = "org.bukkit.event.player.PlayerQuitEvent",
    priority = Event.Priority.NORMAL,
    ignoreCancelled = true,
    run = function(self, event)
        Player:extend(event:getPlayer())._questions = nil
    end
}

local COMMAND_NAME = "/__qq__" .. __LUA_STATE:getModule()
Command:register{
    name = COMMAND_NAME:sub(2),
    permission = "foxbukkit.lua.questioner",
    arguments = {
        {
            name = "id",
            type = "string"
        },
        {
            name = "answer",
            type = "string"
        }
    },
    hidden = true,
    run = function(self, ply, args)
        local myQuestions = ply._questions
        if not myQuestions then
            ply:sendError("You answered an unknwon/expired question")
            return
        end
        local question = myQuestions[args.id]
        if not question then
            ply:sendError("You answered an unknwon/expired question")
            return
        end
        local callback = question[args.answer]
        myQuestions[args.id] = nil
        if callback then
            callback()
        end
    end
}

local function makeQuestionID(ply)
    local id
    local myQuestions = ply._questions
    while (not id) or (myQuestions and myQuestions[id]) do
        id = tostring(math.random(0, 99999999999))
    end
    return id
end

local function rememberConfirmation(ply, rememberKey, state)
    if not ply.rememberConfirmations then
        ply.rememberConfirmations = {}
    end
    ply.rememberConfirmations[rememberKey] = state
    ply:__save()
end

local Questioner = {
    isAvailable = function(self)
        return true
    end,

    forgetPlayerConfirmation = function(self, ply, rememberKey)
        if ply.rememberConfirmations then
            ply.rememberConfirmations[rememberKey] = nil
            ply:__save()
        end
    end,

    askPlayerConfirmation = function(self, ply, rememberKey, yesCallback, noCallback)
        if rememberKey then
            local rememberedConfirmation
            if ply.rememberConfirmations then
                rememberedConfirmation = ply.rememberConfirmations[rememberKey]
            end

            if rememberedConfirmation ~= nil then
                if rememberedConfirmation then
                    yesCallback()
                else
                    noCallback()
                end
                return
            end

            return self:askPlayer(ply, {
                {
                    name = "yes",
                    callback = yesCallback
                },
                {
                    name = "no",
                    callback = noCallback
                },
                {
                    name = "always",
                    callback = function()
                        rememberConfirmation(ply, rememberKey, true)
                        if yesCallback then
                            return yesCallback()
                        end
                    end
                },
                {
                    name = "never",
                    callback = function()
                        rememberConfirmation(ply, rememberKey, false)
                        if noCallback then
                            return noCallback()
                        end
                    end
                }
            }, rememberKey)
        else
            return self:askPlayer(ply, {
                {
                    name = "yes",
                    callback = yesCallback
                },
                {
                    name = "no",
                    callback = noCallback
                }
            })
        end
    end,

    askPlayer = function(self, ply, buttons, rememberKey)
        local msg = {}
        local buttonCallbacks = {}

        local questionId = rememberKey or makeQuestionID(ply)

        for _, v in next, buttons do
            buttonCallbacks[v.name] = v.callback
            table_insert(msg, Chat:makeButton(COMMAND_NAME .. " " .. questionId .. " " .. v.name, v.name, "blue", true, false))
        end

        if not ply._questions then
            ply._questions = {}
        end
        ply._questions[questionId] = buttonCallbacks

        return {
            id = questionId,
            message = table_concat(msg, " ")
        }
    end
}

Player:addExtensions{
    ask = function(self, buttons, rememberKey)
        return Questioner:askPlayer(self, buttons, rememberKey)
    end,

    forgetConfirmation = function(self, rememberKey)
        return Questioner:forgetPlayerConfirmation(self, rememberKey)
    end,

    askConfirmation = function(self, rememberKey, yesCallback, noCallback)
        return Questioner:askPlayerConfirmation(self, rememberKey, yesCallback, noCallback)
    end
}

return Questioner