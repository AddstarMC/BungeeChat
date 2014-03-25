package au.com.addstar.bc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
		if(!configFile.exists())
		{
			configFile.getParentFile().mkdirs();
			
			InputStream stream = getClass().getResourceAsStream("/config.yml");
			if(stream == null)
				throw new IllegalStateException("default config.yml is missing!");
			
			try
			{
				FileOutputStream out = new FileOutputStream(configFile);
				
				byte[] buffer = new byte[1024];
				int read;
				
				while((read = stream.read(buffer)) != -1)
					out.write(buffer, 0, read);
				
				out.close();
			}
			catch(IOException e)
			{
				getLogger().severe("Could not save default config");
				e.printStackTrace();
			}
		}
		
		mConfig = new Config(configFile);
		
		try
		{
			mConfig.load();
		}
		catch(IOException e)
		{
			getLogger().severe("Could not load config");
			e.printStackTrace();
		}
		
		getProxy().registerChannel("BungeeChat");
		getProxy().getPluginManager().registerListener(this, this);
		
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
			output.writeUTF(mConfig.getDefaultFormat());
			
			Map<String, String> groups = mConfig.getGroupFormats();
			output.writeShort(groups.size());
			for(Entry<String, String> entry : groups.entrySet())
			{
				output.writeUTF(entry.getKey());
				output.writeUTF(entry.getValue());
			}
			
			List<ChatChannel> channels = mConfig.getChannels();
			output.writeShort(channels.size());
			for(ChatChannel channel : channels)
			{
				output.writeUTF(channel.name);
				output.writeUTF(channel.command);
				output.writeUTF(channel.format);
				output.writeUTF(channel.permission);
				output.writeUTF(channel.listenPerm);
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
