package au.com.addstar.bc.utils;

import org.bukkit.command.CommandSender;

public class NoPermissionChecker implements ValidChecker<CommandSender>
{
	private String mPermisison;
	
	public NoPermissionChecker(String permission)
	{
		mPermisison = permission;
	}
	
	@Override
	public boolean isValid( CommandSender object )
	{
		return !object.hasPermission(mPermisison);
	}
}
