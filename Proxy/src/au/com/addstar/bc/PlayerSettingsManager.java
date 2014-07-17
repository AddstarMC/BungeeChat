package au.com.addstar.bc;

import java.io.File;
import java.util.UUID;
import java.util.WeakHashMap;

import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.api.ProxyServer;
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
		}
		catch(InvalidConfigurationException e)
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
		
		new MessageOutput("BungeeChat", "SyncPlayer")
			.writeUTF(player.getUniqueId().toString())
			.writeUTF(settings.lastMsgTarget == null ? "" : settings.lastMsgTarget.toString())
			.writeUTF(settings.nickname)
			.writeByte(settings.socialSpyState)
			.writeBoolean(settings.msgEnabled)
			.writeLong(settings.muteTime)
			.writeBoolean(settings.isAFK)
			.send(player.getServer().getInfo());
	}
}
