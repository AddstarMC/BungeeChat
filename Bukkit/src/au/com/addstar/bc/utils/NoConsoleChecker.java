package au.com.addstar.bc.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NoConsoleChecker implements ValidChecker<CommandSender>
{
	@Override
	public boolean isValid( CommandSender object )
	{
		return object instanceof Player;
	}
}
