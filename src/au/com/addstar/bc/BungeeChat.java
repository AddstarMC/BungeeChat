package au.com.addstar.bc;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class BungeeChat extends Plugin implements Listener
{
	@Override
	public void onEnable()
	{
		getProxy().registerChannel("BungeeChat");
		getProxy().getPluginManager().registerListener(this, this);
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
			}
			catch(IOException e)
			{
			}
		}
	}
}
