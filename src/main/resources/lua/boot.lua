string.contains = function(self, sub)
	return self:find(sub, 1, true) ~= nil
end

string.ucfirst = function(self)
	if self:len() < 2 then
		return self:upper()
	end
	return self:sub(1, 1):upper() .. self:sub(2):lower()
end
