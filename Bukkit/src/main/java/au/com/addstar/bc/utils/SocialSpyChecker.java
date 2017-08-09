package au.com.addstar.bc.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import au.com.addstar.bc.BungeeChat;

public class SocialSpyChecker implements ValidChecker<CommandSender>
{
	@Override
	public boolean isValid( CommandSender object )
	{
		if(object instanceof Player)
			return BungeeChat.isSocialSpyEnabled(object);
		
		return false;
	}
}
