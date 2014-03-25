package au.com.addstar.bc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class BungeeChat extends JavaPlugin implements PluginMessageListener
{
	private static Permission permissionManager;
	
	public static String serverName = "ERROR";
	private static BungeeChat mInstance;
	
	private Formatter mFormatter;
	
	@Override
	public void onEnable()
	{
		mInstance = this;
		
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(Permission.class);
		if (permissionProvider != null)
			permissionManager = permissionProvider.getProvider();
		else
			permissionManager = null;
		
		if(isBPermsAvailable())
			BPermsCompat.initialize();
		
		mFormatter = new Formatter();
		
		Bukkit.getPluginManager().registerEvents(new ChatHandler(mFormatter), this);
		Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
		Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeChat", this);
		Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeChat");
		Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
	}
	
	public static void mirrorChat(Player player, String fullChat, String channel)
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);
		
		try
		{
			out.writeUTF("Mirror");
			out.writeUTF(channel);
			out.writeUTF(fullChat);
		}
		catch ( IOException e ) 
		{
		}
		
		player.sendPluginMessage(mInstance, "BungeeChat", stream.toByteArray());
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

	@Override
	public void onPluginMessageReceived( String channel, Player player, byte[] data )
	{
		ByteArrayInputStream stream = new ByteArrayInputStream(data);
		DataInputStream input = new DataInputStream(stream);
		try
		{
			String subChannel = input.readUTF();
			if(channel.equals("BungeeCord"))
			{
				
			}
			else if(channel.equals("BungeeChat"))
			{
				if(subChannel.equals("Mirror"))
				{
					String chatChannel = input.readUTF();
					String message = input.readUTF();
					
					// TODO: ChatChannels
					Bukkit.broadcastMessage(message);
				}
			}
		}
		catch(IOException e) 
		{
		}
	}
}
