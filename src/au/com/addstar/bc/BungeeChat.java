package au.com.addstar.bc;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class BungeeChat extends JavaPlugin
{
	private static Permission permissionManager;
	
	public static String serverName = "ERROR";
	
	private Formatter mFormatter;
	
	@Override
	public void onEnable()
	{
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(Permission.class);
		if (permissionProvider != null)
			permissionManager = permissionProvider.getProvider();
		else
			permissionManager = null;
		
		if(isBPermsAvailable())
			BPermsCompat.initialize();
		
		mFormatter = new Formatter();
		
		Bukkit.getPluginManager().registerEvents(new ChatHandler(mFormatter), this);
	}
	
	public static String getPrimaryGroup(Player player)
	{
		if(permissionManager == null)
			return null;
		
		try
		{
			return permissionManager.getPrimaryGroup(player);
		}
		catch(UnsupportedOperationException e)
		{
			if(isBPermsAvailable())
				return BPermsCompat.getPrimaryGroup(player);
			return null;
		}
	}
	
	public static boolean isInGroup(Player player, String group)
	{
		if(permissionManager == null)
			return false;
		
		try
		{
			return permissionManager.playerInGroup(player, group);
		}
		catch(UnsupportedOperationException e)
		{
			if(isBPermsAvailable())
				return BPermsCompat.isInGroup(player, group);
			return false;
		}
	}
	
	public static boolean isBPermsAvailable()
	{
		return Bukkit.getPluginManager().isPluginEnabled("BungeePermsBukkit");
	}
}
