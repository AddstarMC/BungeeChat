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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import au.com.addstar.bc.config.ChatChannel;
import au.com.addstar.bc.config.Config;
import au.com.addstar.bc.config.KeywordHighlighterSettings;
import au.com.addstar.bc.config.PermissionSetting;
import au.com.addstar.bc.config.ServerConfig;
import au.com.addstar.bc.listeners.BungeeListener;
import au.com.addstar.bc.listeners.PlayerHandler;
import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketManager;
import au.com.addstar.bc.sync.ProxyComLink;
import au.com.addstar.bc.sync.SyncConfig;
import au.com.addstar.bc.sync.SyncManager;
import au.com.addstar.bc.sync.SyncUtil;
import au.com.addstar.bc.sync.packet.PlayerListPacket;
import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import net.md_5.bungee.api.plugin.Plugin;

public class BungeeChat extends Plugin
{
	private Config mConfig;
	private SyncConfig mConfigSync;
	
	private HashMap<String, String> mKeywordSettings = new HashMap<>();
	private PlayerSettingsManager mSettings;
	
	public static BungeeChat instance;
	private String channelName = "bungeechat:chat";
	
	private SyncManager mSyncManager;
	private PacketManager mPacketManager;
	private MuteHandler mMuteHandler;

	public SubscriptionHandler getSubHandler() {
		return mSubHandler;
	}

	private SubscriptionHandler mSubHandler;
	private SkinLibrary mSkins;
	
	private ProxyComLink mComLink;
	
	@Override
	public void onEnable()
	{
		instance = this;
		
		File configFile = new File(getDataFolder(), "config.yml");
		if(!getDataFolder().exists())
			getDataFolder().mkdirs();
		
		saveResource("/keywords.txt", false);
		
		mConfig = new Config(configFile);
		loadConfig();
		
		mSettings = new PlayerSettingsManager(new File(getDataFolder(), "players"));
		
		mComLink = new ProxyComLink();
		// This setup is needed as the redis connection cannot be established on the main thread, but we need it to be established before continuing
		final CountDownLatch setupWait = new CountDownLatch(1);
		getProxy().getScheduler().runAsync(this, () -> {
			try
			{
				mComLink.init(mConfig.redis.host, mConfig.redis.port, mConfig.redis.password);
			}
			finally
			{
				setupWait.countDown();
			}
		});
		
		try
		{
			setupWait.await();
		}
		catch(InterruptedException ignored)
		{
		}
		
		mPacketManager = new PacketManager(this);
		mPacketManager.initialize();
		mPacketManager.addHandler(new PacketHandler(), (Class<? extends Packet>[])null);
		
		mSyncManager = new SyncManager(this);
		SyncUtil.addSerializer(ChatChannel.class, "ChatChannel");
		SyncUtil.addSerializer(KeywordHighlighterSettings.class, "KHSettings");
		SyncUtil.addSerializer(PermissionSetting.class, "PermSetting");
		
		applySyncConfig();
		
		StandardServMethods methods = new StandardServMethods();
		mSyncManager.addMethod("bungee:getServerName", methods);
		mSyncManager.addMethod("bchat:isAFK", methods);
		mSyncManager.addMethod("bchat:canMsg", methods);
		mSyncManager.addMethod("bchat:setAFK", methods);
		mSyncManager.addMethod("bchat:toggleAFK", methods);
		mSyncManager.addMethod("bchat:setTabColor", methods);
		mSyncManager.addMethod("bchat:setMute", methods);
		mSyncManager.addMethod("bchat:setMuteIP", methods);
		mSyncManager.addMethod("bchat:setGMute", methods);
		mSyncManager.addMethod("bchat:toggleGMute", methods);
		mSyncManager.addMethod("bchat:setMsgTarget", methods);
		mSyncManager.addMethod("bchat:getMuteList", methods);
		mSyncManager.addMethod("bchat:kick", methods);
		mSyncManager.addMethod("bchat:setSkin", methods);
		mSyncManager.addMethod("bchat:getSubscribed", methods);
		
		getProxy().registerChannel(channelName);
		
		getProxy().getPluginManager().registerListener(this, new BungeeListener(channelName,this));
		getProxy().getPluginManager().registerCommand(this, new ManagementCommand(this));
		getProxy().getPluginManager().registerCommand(this, new Debugger());
		getProxy().getPluginManager().registerListener(this, new PlayerHandler());

		mMuteHandler = new MuteHandler(this);
		mSubHandler = new SubscriptionHandler(this);
		mMuteHandler.updateSettings(mConfig);
		
		mSkins = new SkinLibrary();

		mSyncManager.sendConfig("bungeechat");
		mPacketManager.sendSchemas();
	}
	
	public boolean loadConfig()
	{
		try
		{
			mConfig.init();
			
			for(ChatChannel channel : mConfig.channels.values())
			{
				if(channel.listenPermission == null)
					channel.listenPermission = channel.permission;
				else if(channel.listenPermission.equals("*"))
					channel.listenPermission = "";
			}
			
			if(mConfig.keywordHighlighter.enabled)
			{
				loadKeywordFile(mConfig.keywordHighlighter.keywordFile);
			}
			if(mMuteHandler != null)
				mMuteHandler.updateSettings(mConfig);
			return true;
		}
		catch ( InvalidConfigurationException e )
		{
			getLogger().severe("Could not load config");
			e.printStackTrace();
			return false;
		}
		catch (IOException e)
		{
			getLogger().severe("Could not load " + mConfig.keywordHighlighter.keywordFile);
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean applySyncConfig()
	{
		try
		{
			SyncConfig syncConfig = mConfig.toSyncConfig();
			if(mConfig.keywordHighlighter.enabled)
			{
				loadKeywordFile(mConfig.keywordHighlighter.keywordFile);
				SyncConfig keywords = syncConfig.createSection("keywords");
				for(Entry<String, String> entry : mKeywordSettings.entrySet())
					keywords.set(entry.getKey(), entry.getValue());
			}
			
			mConfigSync = syncConfig;
			mSyncManager.setConfig("bungeechat", mConfigSync);
			return true;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	private void loadKeywordFile(String file) throws IOException
	{
		mKeywordSettings.clear();
		File onDisk = new File(getDataFolder(), file);

		try (InputStream input = new FileInputStream(onDisk)) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));

			int lineNo = 0;
			while (reader.ready()) {
				++lineNo;
				String line = reader.readLine();
				if (line.startsWith("#") || line.trim().isEmpty())
					continue;

				String regex, colourString;

				if (line.contains(">")) {
					int pos = line.lastIndexOf('>');
					regex = line.substring(0, pos).trim();
					colourString = line.substring(pos + 1).trim();
				} else {
					regex = line.trim();
					colourString = NamedTextColor.GOLD.toString();
				}

				try {
					Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
				} catch (PatternSyntaxException e) {
					getLogger().warning("[" + file + "] Invalid regex: \"" + regex + "\" at line " + lineNo);
					continue;
				}

				StringBuilder colour = new StringBuilder();
				Style.Builder textStyleBuilder = Style.style();
				String[] parts = colourString.split(";");
				for (String part:parts){
					try {
						TextColor  color = NamedTextColor.NAMES.value(colourString);
						if (color != null) {
							textStyleBuilder.color(color);
							continue;
						}
						color = TextColor.fromHexString(part);
						if (color != null) {
							textStyleBuilder.color(color);
							continue;
						}
						color = TextColor.color(Integer.parseInt(part,16));
						textStyleBuilder.color(color);
						continue;
					} catch (NumberFormatException e){
						//ignore
					}
					try {
						TextDecoration decoration = TextDecoration.valueOf(part);
						textStyleBuilder.decoration(decoration, TextDecoration.State.TRUE);
						continue;
					} catch (IllegalArgumentException e) {
						//ignore
					}
					getLogger().warning("[" + file + "] Invalid colour code: \'" + part + "\' at line " + lineNo);
				}
				Style style = textStyleBuilder.build();
				String styleString = GsonComponentSerializer.gson().serializer().toJson(style);
				mKeywordSettings.put(regex, styleString);
			}
			reader.close();
		}
	}
	
	private void saveResource(String resource, boolean overwrite)
	{
		File destination = new File(getDataFolder(), resource);
		
		if(destination.exists() && !overwrite)
			return;
		
		destination.getParentFile().mkdirs();
		
		InputStream input = getClass().getResourceAsStream(resource);
		if(input == null)
		{
			getLogger().severe("Could not save resource " + resource + ". It does not exist in the jar.");
			return;
		}
		
		FileOutputStream output = null;
		try
		{
			output = new FileOutputStream(destination);
			byte[] buffer = new byte[1024];
			int read;
			
			while((read = input.read(buffer)) != -1)
			{
				output.write(buffer, 0, read);
			}
		}
		catch(IOException e)
		{
			getLogger().severe("Could not save resource " + resource + ". An IOException occured:");
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (output != null)
					output.close();
				input.close();
			}
			catch ( IOException ignored)
			{
			}
		}
	}
	public Map<String, ChatChannel> getChannels(){
		return mConfig.channels;
	}

	
	public void sendPlayerUpdates(ServerInfo server)
	{
		Collection<ProxiedPlayer> players = getProxy().getPlayers();
		ArrayList<UUID> ids = new ArrayList<>(players.size());
		ArrayList<String> names = new ArrayList<>(players.size());
		ArrayList<String> nicknames = new ArrayList<>(players.size());
		
		for(ProxiedPlayer player : players)
		{
			ids.add(player.getUniqueId());
			names.add(player.getName());
			PlayerSettings settings = mSettings.getSettings(player);
			nicknames.add(settings.nickname);
		}
		
		PlayerListPacket packet = new PlayerListPacket(ids, names, nicknames);
		if(server != null)
			mPacketManager.send(packet, server);
		else
			mPacketManager.broadcast(packet);
	}

	
	public ChatChannel getChannel(String name)
	{
		return mConfig.channels.get(name);
	}
	
	public PacketManager getPacketManager()
	{
		return mPacketManager;
	}
	
	public PlayerSettingsManager getManager()
	{
		return mSettings;
	}
	
	public SyncManager getSyncManager()
	{
		return mSyncManager;
	}
	
	public ProxyComLink getComLink()
	{
		return mComLink;
	}
	
	public MuteHandler getMuteHandler()
	{
		return mMuteHandler;
	}
	
	public SkinLibrary getSkinLibrary()
	{
		return mSkins;
	}
	
}
