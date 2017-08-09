package au.com.addstar.bc.sync;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.AbstractMap.SimpleEntry;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.Debugger;
import au.com.addstar.bc.sync.ServerComLink.ConnectionStateNotify;

import com.google.common.collect.HashMultimap;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;

public class PacketManager implements Listener, IDataReceiver, ConnectionStateNotify
{
	private HashMap<ServerInfo, PacketCodec> mCodecs;
	private HashMultimap<Class<? extends Packet>, IPacketHandler> mHandlers;
	private ProxyComLink mComLink;
	
	// Packets that arrived before the schema did
	private LinkedList<SimpleEntry<ServerInfo, DataInput>> mPendingPackets;
	
	public PacketManager(Plugin plugin)
	{
		mCodecs = new HashMap<>();
		mHandlers = HashMultimap.create();
		mPendingPackets = new LinkedList<>();
		mComLink = BungeeChat.instance.getComLink();
		mComLink.listenToChannel("BungeeChat", this);
		mComLink.listenToChannel("BCState", this);
		ProxyServer.getInstance().getPluginManager().registerListener(plugin, this);
		BungeeChat.instance.getComLink().setNotifyHandle(this);
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
		
		BungeeChat.instance.getComLink().broadcastMessage("BCState", ostream.toByteArray());
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
		Debugger.logp("Sending to %s: %s", server.getName(), packet);
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);
		try
		{
			PacketRegistry.write(packet, out);
			BungeeChat.instance.getComLink().sendMessage("BungeeChat", stream.toByteArray(), server);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void broadcast(Packet packet)
	{
		Debugger.logp("Broadcast: %s", packet);
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);
		try
		{
			PacketRegistry.write(packet, out);
			BungeeChat.instance.getComLink().broadcastMessage("BungeeChat", stream.toByteArray());
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@Deprecated
	public void broadcastNoQueue( Packet packet )
	{
		broadcast(packet);
	}
	
	private void handleDataPacket(ServerInfo server, PacketCodec codec, DataInput in)
	{
		Packet packet = null;
		try
		{
			packet = codec.read(in);
			if(packet == null)
			{
				Debugger.logp("Received packet but decoded to null. %s", server.getName());
				return;
			}
			
			Debugger.logp("Received packet from %s: %s", server.getName(), packet);
			
			// Handler spec handlers
			for(IPacketHandler handler : mHandlers.get(packet.getClass()))
				handler.handle(packet, server);
			
			// Handle non spec handlers
			for(IPacketHandler handler : mHandlers.get(null))
				handler.handle(packet, server);
		}
		catch(Throwable e)
		{
			BungeeChat.instance.getLogger().severe("An error occured handling packet: " + packet + ":");
			e.printStackTrace();
		}
	}
	
	@Override
	public void onReceive( String channel, DataInput in, MessageSender sender )
	{
		if(channel.equals("BungeeChat"))
		{
			ServerInfo server = ProxyServer.getInstance().getServerInfo(sender.getName());
			PacketCodec codec = mCodecs.get(server);
			
			if(codec == null)
			{
				Debugger.logp("Received packet. Pending codec from %s", server.getName());
				synchronized(mPendingPackets)
				{
					mPendingPackets.add(new SimpleEntry<>(server, in));
				}
				return;
			}
			
			handleDataPacket(server, codec, in);
		}
		else if(channel.equals("BCState"))
		{
			ServerInfo server = ProxyServer.getInstance().getServerInfo(sender.getName());
			
			try
			{
				String type = in.readUTF();
				if(type.equals("Schema"))
				{
					Debugger.logp("Received schema from %s", server.getName());
					PacketCodec codec = PacketCodec.fromSchemaData(in);
					mCodecs.put(server, codec);
					doPending(server);
				}
				else if(type.equals("Online"))
				{
					Debugger.logp("Received online message from %s", server.getName());
					ByteArrayOutputStream ostream = new ByteArrayOutputStream();
					DataOutputStream out = new DataOutputStream(ostream);
					PacketRegistry.writeSchemaPacket(out);
					mComLink.sendMessage("BCState", ostream.toByteArray(), sender);
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			
			
		}
	}

	public void sendSchemas()
	{
		try
		{
			// Send schemas to all servers
			ByteArrayOutputStream ostream = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(ostream);
			PacketRegistry.writeSchemaPacket(out);
			
			mComLink.broadcastMessage("BCState", ostream.toByteArray());

			// Request schemas from all servers
			ostream = new ByteArrayOutputStream();
			out = new DataOutputStream(ostream);
			out.writeUTF("SchemaRequest");
			
			mComLink.broadcastMessage("BCState", ostream.toByteArray());
		}
		catch ( IOException e )
		{
			// cant happen
			e.printStackTrace();
		}
	}

	private void doPending(ServerInfo server)
	{
		PacketCodec codec = mCodecs.get(server);
		if(codec == null)
			return;
		
		synchronized(mPendingPackets)
		{
			Iterator<SimpleEntry<ServerInfo, DataInput>> it = mPendingPackets.iterator();
			
			while(it.hasNext())
			{
				SimpleEntry<ServerInfo, DataInput> entry = it.next();
				
				if(entry.getKey().equals(server))
				{
					it.remove();
					Debugger.logp("Do pending:");
					handleDataPacket(server, codec, entry.getValue());
				}
			}
		}
	}
	
	@Override
	public void onConnectionLost( Throwable e )
	{
	}

	@Override
	public void onConnectionRestored()
	{
		ProxyServer.getInstance().getScheduler().runAsync(BungeeChat.instance, new Runnable()
		{
			@Override
			public void run()
			{
				System.out.println("[BungeeChat] Redis connection restored");
				BungeeChat.instance.getSyncManager().sendConfig("bungeechat");
			}
		});
	}
}
