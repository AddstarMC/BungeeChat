/*
 * BungeeChat
 *
 * Copyright (c) 2015 - 2020.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy   of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is *
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR  IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package au.com.addstar.bc;

/*-
 * #%L
 * BungeeChat-Proxy
 * %%
 * Copyright (C) 2015 - 2020 AddstarMC
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

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
		catch(org.yaml.snakeyaml.error.YAMLException e)
		{
			System.err.println("Error initialising PlayerSettings for " + player.getName() + " (" + player.getUniqueId() + ")");
			System.err.println(e.getMessage());
			//e.printStackTrace();
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
