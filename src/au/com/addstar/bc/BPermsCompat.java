package au.com.addstar.bc;

import net.alpenblock.bungeeperms.bukkit.BungeePerms;
import net.alpenblock.bungeeperms.bukkit.Group;
import net.alpenblock.bungeeperms.bukkit.PermissionsManager;
import net.alpenblock.bungeeperms.bukkit.User;

import org.bukkit.entity.Player;

public class BPermsCompat
{
	private static PermissionsManager mManager;
	
	public static void initialize()
	{
		mManager = BungeePerms.getInstance().getPermissionsManager();
	}
	
	public static String getPrimaryGroup(Player player)
	{
		User user = mManager.getUser(player.getName());
		if(user == null)
			return null;
		
		Group group = mManager.getMainGroup(user);
		if(group == null)
			return null;
		
		return group.getName();
	}
	
	public static boolean isInGroup(Player player, String groupName)
	{
		User user = mManager.getUser(player.getName());
		if(user == null)
			return false;
		
		for(Group group : user.getGroups())
		{
			if(groupName.equals(group.getName()))
				return true;
		}
		
		return false;
	}
}
