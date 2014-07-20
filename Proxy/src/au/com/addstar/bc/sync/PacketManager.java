package au.com.addstar.bc.sync;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.AbstractMap.SimpleEntry;

import com.google.common.collect.HashMultimap;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class PacketManager implements Listener
{
	private HashMap<ServerInfo, PacketCodec> mCodecs;
	private HashMultimap<Class<? extends Packet>, IPacketHandler> mHandlers;
	
	// Packets that arrived before the schema did
	private LinkedList<SimpleEntry<ServerInfo, byte[]>> mPendingPackets;
	
	public PacketManager(Plugin plugin)
	{
		mCodecs = new HashMap<ServerInfo, PacketCodec>();
		mHandlers = HashMultimap.create();
		mPendingPackets = new LinkedList<SimpleEntry<ServerInfo,byte[]>>();
		ProxyServer.getInstance().getPluginManager().registerListener(plugin, this);
		ProxyServer.getInstance().registerChannel("BungeeChat");
		ProxyServer.getInstance().registerChannel("BCState");
	}
	
	public void initialize()
	{
		ByteArrayOutputStream ostream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(ostream);
		try
		{
			PacketRegistry.writeSchemaPacket(out);
		}
		catch(IOException e)
		{
			// Cant happen
		}
		
		for(ServerInfo server : ProxyServer.getInstance().getServers().values())
			server.sendData("BCState", ostream.toByteArray());
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
	
	public void send(Packet packet, ServerInfo server)
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);
		try
		{
			PacketRegistry.write(packet, out);
			server.sendData("BungeeChat", stream.toByteArray());
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void broadcast(Packet packet)
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);
		try
		{
			PacketRegistry.write(packet, out);
			byte[] data = stream.toByteArray();
			
			for(ServerInfo server : ProxyServer.getInstance().getServers().values())
				server.sendData("BungeeChat", data);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void broadcastNoQueue( Packet packet )
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);
		try
		{
			PacketRegistry.write(packet, out);
			byte[] data = stream.toByteArray();
			
			for(ServerInfo server : ProxyServer.getInstance().getServers().values())
			{
				if(!server.getPlayers().isEmpty())
					server.sendData("BungeeChat", data);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void handleDataPacket(ServerInfo server, PacketCodec codec, byte[] data)
	{
		ByteArrayInputStream stream = new ByteArrayInputStream(data);
		DataInputStream input = new DataInputStream(stream);
		
		try
		{
			Packet packet = codec.read(input);
			if(packet == null)
				return;
			
			// Handler spec handlers
			for(IPacketHandler handler : mHandlers.get(packet.getClass()))
				handler.handle(packet, server);
			
			// Handle non spec handlers
			for(IPacketHandler handler : mHandlers.get(null))
				handler.handle(packet, server);
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
	
	@EventHandler
	public void onReceive(PluginMessageEvent event)
	{
		if(!(event.getSender() instanceof Server))
			return;
		
		if(event.getTag().equals("BungeeChat"))
		{
			ServerInfo server = ((Server)event.getSender()).getInfo();
			PacketCodec codec = mCodecs.get(server);
			
			if(codec == null)
			{
				mPendingPackets.add(new SimpleEntry<ServerInfo, byte[]>(server, event.getData()));
				return;
			}
			
			handleDataPacket(server, codec, event.getData());
		}
		else if(event.getTag().equals("BCState"))
		{
			ServerInfo server = ((Server)event.getSender()).getInfo();
			ByteArrayInputStream stream = new ByteArrayInputStream(event.getData());
			DataInputStream input = new DataInputStream(stream);
			
			try
			{
				String type = input.readUTF();
				if(type.equals("Schema"))
				{
					PacketCodec codec = PacketCodec.fromSchemaData(input);
					mCodecs.put(server, codec);
					doPending(server);
				}
				else if(type.equals("Online"))
				{
					ByteArrayOutputStream ostream = new ByteArrayOutputStream();
					DataOutputStream out = new DataOutputStream(ostream);
					PacketRegistry.writeSchemaPacket(out);
					server.sendData("BCState", ostream.toByteArray());
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			
			
		}
	}
	
	private void doPending(ServerInfo server)
	{
		PacketCodec codec = mCodecs.get(server);
		if(codec == null)
			return;
		
		Iterator<SimpleEntry<ServerInfo, byte[]>> it = mPendingPackets.iterator();
		
		while(it.hasNext())
		{
			SimpleEntry<ServerInfo, byte[]> entry = it.next();
			
			if(entry.getKey().equals(server))
			{
				it.remove();
				handleDataPacket(server, codec, entry.getValue());
			}
		}
	}
}
