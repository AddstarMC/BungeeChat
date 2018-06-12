package au.com.addstar.bc;

import java.io.File;
import java.util.UUID;
import java.util.WeakHashMap;

import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class PlayerSettingsManager
{
	private WeakHashMap<ProxiedPlayer, PlayerSettings> mLoadedSettings = new WeakHashMap<>();
	private File mFolder;
	
	public PlayerSettingsManager(File folder)
	{
		mFolder = folder;
		mFolder.mkdirs();
	}
	
	public PlayerSettings getSettings(UUID player)
	{
		return getSettings(ProxyServer.getInstance().getPlayer(player));
	}
	
	public PlayerSettings getSettings(ProxiedPlayer player)
	{
		if(mLoadedSettings.containsKey(player))
			return mLoadedSettings.get(player);
		
		PlayerSettings settings = new PlayerSettings(new File(mFolder, player.getUniqueId().toString() + ".yml"));
		
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
		Debugger.log("Loaded player settings %s", player.getName());
		return settings;
	}
	
	public void savePlayer(UUID player)
	{
		savePlayer(ProxyServer.getInstance().getPlayer(player));
	}
	
	public void savePlayer(ProxiedPlayer player)
	{
		try
		{
			getSettings(player).save();
			Debugger.log("Saved player settings %s", player.getName());
		}
		catch(InvalidConfigurationException ignored)
		{
		}
	}
	
	public void updateSettings(UUID player)
	{
		updateSettings(ProxyServer.getInstance().getPlayer(player));
	}
	
	public void updateSettings(ProxiedPlayer player)
	{
		PlayerSettings settings = getSettings(player);
		BungeeChat.instance.getPacketManager().send(settings.getUpdatePacket(player.getUniqueId()), player.getServer().getInfo());
		Debugger.log("Sending player settings update %s", player.getName());
	}

	public void unloadPlayer( UUID id )
	{
		ProxiedPlayer player = ProxyServer.getInstance().getPlayer(id);
		mLoadedSettings.remove(player);
		BungeeChat.instance.getSubHandler().unSubscribe(id);
		Debugger.log("Unloaded player settings %s", player.getName());
		BungeeChat.instance.getSubHandler().unSubscribe(id);
	}
}
