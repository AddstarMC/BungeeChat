package au.com.addstar.bc.sync;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.collect.HashMultimap;

public class PacketManager implements PluginMessageListener, Listener
{
	public static boolean enabledDebug = false;
	private LinkedList<Packet> mSendQueue;
	private PacketCodec mCodec;
	private Plugin mPlugin;
	private HashMultimap<Class<? extends Packet>, IPacketHandler> mHandlers;
	private Player mSendPlayer;
	private boolean mInitialized;
	
	public PacketManager(Plugin plugin)
	{
		Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "BungeeChat", this);
		Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "BCState", this);
		Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "BungeeChat");
		Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "BCState");
		Bukkit.getPluginManager().registerEvents(this, plugin);
		
		mSendQueue = new LinkedList<Packet>();
		mPlugin = plugin;
		mHandlers = HashMultimap.create();
	}
	
	public void addHandler(IPacketHandler handler, Class<? extends Packet>... packets)
	{
		if(packets == null)
			mHandlers.put(null, handler);
		else
		{
			for(Class<? extends Packet> clazz : packets)
				mHandlers.put(clazz, handler);
		}
	}
	
	public void initialize()
	{
		// Send schema information
		if(getSendPlayer() != null)
		{
			sendInitPackets();
			mInitialized = true;
		}
	}
	
	public void send(Packet packet)
	{
		if(!sendNoQueue(packet))
			mSendQueue.add(packet);
	}
	
	public boolean sendNoQueue(Packet packet)
	{
		Player player = getSendPlayer();
		if(player == null)
		{
			if(enabledDebug)
				debug("No send 0 players. " + packet);
			return false;
		}
		
		if(enabledDebug)
			debug("Sending " + packet);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);
		try
		{
			PacketRegistry.write(packet, out);
			byte[] data = stream.toByteArray();
			
			player.sendPluginMessage(mPlugin, "BungeeChat", data);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		return true;
	}
	
	private Player getSendPlayer()
	{
		if(mSendPlayer == null || !mSendPlayer.isOnline())
		{
			Player[] players = Bukkit.getOnlinePlayers();
			if(players.length == 0)
				mSendPlayer = null;
			else
				mSendPlayer = players[0];
		}
		return mSendPlayer;
	}
	
	private void sendInitPackets()
	{
		Player player = getSendPlayer();
		
		// Notify proxy that this server is now online
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);
		
		try
		{
			if(enabledDebug)
				debug("Sending online message");
			out.writeUTF("Online");
			
			byte[] data = stream.toByteArray();
			player.sendPluginMessage(mPlugin, "BCState", data);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		// Tell the proxy this servers schema
		stream = new ByteArrayOutputStream();
		out = new DataOutputStream(stream);
		
		try
		{
			if(enabledDebug)
				debug("Sending schema to proxy");
			PacketRegistry.writeSchemaPacket(out);
			
			byte[] data = stream.toByteArray();
			player.sendPluginMessage(mPlugin, "BCState", data);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void onPluginMessageReceived( String channel, Player player, byte[] data  )
	{
		if(channel.equals("BungeeChat"))
		{
			ByteArrayInputStream stream = new ByteArrayInputStream(data);
			DataInputStream input = new DataInputStream(stream);
			
			try
			{
				Packet packet = mCodec.read(input);
				if(packet == null)
					return;
				
				if(enabledDebug)
					debug("Received packet " + packet.toString());
				
				// Handler spec handlers
				for(IPacketHandler handler : mHandlers.get(packet.getClass()))
					handler.handle(packet);
				
				// Handle non spec handlers
				for(IPacketHandler handler : mHandlers.get(null))
					handler.handle(packet);
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			catch(IllegalArgumentException e)
			{
				e.printStackTrace();
			}
		}
		else if(channel.equals("BCState"))
		{
			ByteArrayInputStream stream = new ByteArrayInputStream(data);
			DataInputStream input = new DataInputStream(stream);
			
			try
			{
				String type = input.readUTF();
				if(type.equals("Schema"))
					mCodec = PacketCodec.fromSchemaData(input);
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	@EventHandler
	private void onPlayerLeave(PlayerQuitEvent event)
	{
		if(event.getPlayer().equals(mSendPlayer))
			mSendPlayer = null;
	}
	
	@EventHandler
	private void onPlayerLeave(PlayerKickEvent event)
	{
		if(event.getPlayer().equals(mSendPlayer))
			mSendPlayer = null;
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onPlayerLogin(PlayerJoinEvent event)
	{
		Player[] players = Bukkit.getOnlinePlayers();
		
		if(players.length != 1)
			return;
		
		Bukkit.getScheduler().runTaskLater(mPlugin, new Runnable()
		{
			@Override
			public void run()
			{
				if(getSendPlayer() == null)
					return;
				
				if(!mInitialized)
				{
					sendInitPackets();
					mInitialized = true;
				}
				
				Iterator<Packet> it = mSendQueue.iterator();
				while(it.hasNext())
				{
					Packet packet = it.next();
					if(sendNoQueue(packet))
						it.remove();
					else
						break;
				}
			}
		}, 2L);
	}
	
	private void debug(String text)
	{
		System.out.println("[BungeeChatDebug] " + text);
	}
}
