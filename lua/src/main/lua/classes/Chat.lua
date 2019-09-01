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

local bukkitServer = require("Server"):getBukkitServer()
local Player = require("Player")

Player:addConsoleExtensions{
    sendReply = function(self, message)
        return self:sendMessage("[FB] " .. message)
    end,

    sendError = function(self, message)
        return self:sendMessage("[FB] [ERROR] " .. message)
    end
}

local Chat = {
    isAvailable = function(self)
        return false
    end,

    getConsole = Player.getConsole,

    makeButton = function(self, command, label, color, run, addHover)
        return "<BUTTON:" .. command ">" .. label .. "</BUTTON>"
    end,

    getPlayerUUID = function(self, name)
        return nil
    end,

    sendGlobal = function(self, source, type, content)
        bukkitServer:broadcastMessage(content)
    end,

    broadcastLocal = function(self, source, content)
        bukkitServer:broadcastMessage(content)
    end,

    sendLocalToPlayer = function(self, source, content, target)
        if target then
            target:sendMessage(content)
        else -- content, target
            content:sendMessage(source)
        end
    end,

    sendLocalToPermission = function(self, source, content, target)
        if target then
            bukkitServer:broadcastMessage("$" + target, content)
        else -- content, target
            bukkitServer:broadcastMessage("$" + content, source)
        end
    end,

    sendLocal = function(self, source, content, chatTarget, targetFilter)
        bukkitServer:broadcastMessage("!" .. tostring(targetFilter) .. "!" .. tostring(chatTarget), content)
    end,

    getPlayerNick = function(self, ply_or_uuid)
        return ply_or_uuid:getDisplayName()
    end,
}

Player:addExtensions{
    sendXML = function(self, message)
        return Chat:sendLocalToPlayer(message, self)
    end,

    sendReply = function(self, message)
        return self:sendXML("[FB] " .. message)
    end,

    sendError = function(self, message)
        return self:sendXML("[FB] [ERROR] " .. message)
    end,

    getNickName = function(self)
        return self:getDisplayName()
    end
}

return Chat