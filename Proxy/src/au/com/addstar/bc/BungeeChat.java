package au.com.addstar.bc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class BungeeChat extends Plugin implements Listener
{
	private Config mConfig;
	
	@Override
	public void onEnable()
	{
		File configFile = new File(getDataFolder(), "config.yml");
		if(!getDataFolder().exists())
			getDataFolder().mkdirs();
		
		mConfig = new Config(configFile);
		
		loadConfig();
		
		getProxy().registerChannel("BungeeChat");
		getProxy().getPluginManager().registerListener(this, this);
		getProxy().getPluginManager().registerCommand(this, new ManagementCommand(this));
		
		resync();
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
			return true;
		}
		catch ( InvalidConfigurationException e )
		{
			getLogger().severe("Could not load config");
			e.printStackTrace();
			return false;
		}
	}
	
	public void resync()
	{
		for(ServerInfo server : getProxy().getServers().values())
			sendInfo(server);
	}
	
	private void mirrorChat(byte[] data, Server except)
	{
		ServerInfo exceptInfo = except.getInfo();
		for(ServerInfo server : getProxy().getServers().values())
		{
			if(!server.equals(exceptInfo) && !server.getPlayers().isEmpty())
				server.sendData("BungeeChat", data);
		}
	}
	
	private void sendInfo(ServerInfo server)
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream output = new DataOutputStream(stream);
		
		try
		{
			output.writeUTF("Update");
			output.writeUTF(server.getName());
			output.writeUTF(mConfig.consoleName);
			
			Map<String, PermissionSetting> permissions = mConfig.permSettings;
			output.writeShort(permissions.size());
			for(Entry<String, PermissionSetting> entry : permissions.entrySet())
			{
				PermissionSetting setting = entry.getValue();

				output.writeUTF((setting.permission == null ? "" : setting.permission));
				output.writeShort(setting.priority);
				output.writeUTF(setting.format);
				output.writeUTF(setting.color);
			}
			
			Map<String, ChatChannel> channels = mConfig.channels;
			output.writeShort(channels.size());
			for(Entry<String, ChatChannel> entry : channels.entrySet())
			{
				ChatChannel channel = entry.getValue();
				
				output.writeUTF(entry.getKey());
				
				output.writeUTF(channel.command);
				output.writeUTF(channel.format);
				output.writeUTF(channel.permission);
				output.writeUTF(channel.listenPermission);
			}
			
			server.sendData("BungeeChat", stream.toByteArray());
		}
		catch(IOException e)
		{
		}
	}
	
	@EventHandler
	public void onMessage(PluginMessageEvent event)
	{
		if(event.getTag().equals("BungeeChat") && event.getSender() instanceof Server)
		{
			ByteArrayInputStream stream = new ByteArrayInputStream(event.getData());
			DataInputStream input = new DataInputStream(stream);
			
			try
			{
				String subChannel = input.readUTF();
				
				if(subChannel.equals("Mirror"))
				{
					input.readUTF();
					String message = input.readUTF();
					getProxy().getConsole().sendMessage(new TextComponent(message));
					mirrorChat(event.getData().clone(), (Server)event.getSender());
				}
				else if(subChannel.equals("Update"))
				{
					sendInfo(((Server)event.getSender()).getInfo());
				}
			}
			catch(IOException e)
			{
			}
		}
	}
}
