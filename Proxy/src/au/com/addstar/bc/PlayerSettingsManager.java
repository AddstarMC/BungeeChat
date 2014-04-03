package au.com.addstar.bc;

import java.io.File;
import java.util.WeakHashMap;

import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class PlayerSettingsManager
{
	private WeakHashMap<ProxiedPlayer, PlayerSettings> mLoadedSettings = new WeakHashMap<ProxiedPlayer, PlayerSettings>();
	private File mFolder;
	
	public PlayerSettingsManager(File folder)
	{
		mFolder = folder;
		mFolder.mkdirs();
	}
	
	public PlayerSettings getSettings(String player)
	{
		return getSettings(BungeeCord.getInstance().getPlayer(player));
	}
	
	public PlayerSettings getSettings(ProxiedPlayer player)
	{
		if(mLoadedSettings.containsKey(player))
			return mLoadedSettings.get(player);
		
		PlayerSettings settings = new PlayerSettings(new File(mFolder, player.getName().toLowerCase() + ".yml"));
		
		try
		{
			settings.init();
		}
		catch(InvalidConfigurationException e)
		{
			System.err.println("Could not load player data for " + player.getName());
			e.printStackTrace();
		}
		
		mLoadedSettings.put(player, settings);
		return settings;
	}
	
	public void savePlayer(String player)
	{
		savePlayer(BungeeCord.getInstance().getPlayer(player));
	}
	
	public void savePlayer(ProxiedPlayer player)
	{
		try
		{
			getSettings(player).save();
		}
		catch(InvalidConfigurationException e)
		{
		}
	}
}
