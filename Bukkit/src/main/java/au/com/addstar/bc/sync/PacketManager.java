package au.com.addstar.bc.sync;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.Debugger;
import au.com.addstar.bc.sync.ServerComLink.ConnectionStateNotify;

import com.google.common.collect.HashMultimap;

public class PacketManager implements IDataReceiver, ConnectionStateNotify
{
	private PacketCodec mCodec;
	private HashMultimap<Class<? extends Packet>, IPacketHandler> mHandlers;
	private BukkitComLink mComLink;
	
	// Packets that arrived before the schema did
	private LinkedList<DataInput> mPendingPackets;
	
	public PacketManager(Plugin plugin)
	{
		mComLink = BungeeChat.getComLink();
		mComLink.setNotifyHandle(this);
		
		mHandlers = HashMultimap.create();
		mPendingPackets = new LinkedList<DataInput>();
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
		try
		{
			mComLink.listenToChannel("BungeeChat", this).get();
			mComLink.listenToChannel("BCState", this).get();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return;
		}
		
		sendInitPackets();
	}
	
	public void send(Packet packet)
	{
		sendNoQueue(packet);
	}
	
	public boolean sendNoQueue(Packet packet)
	{
		Debugger.logp("Sending %s", packet);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);
		try
		{
			PacketRegistry.write(packet, out);
			byte[] data = stream.toByteArray();
			
			mComLink.sendMessage("BungeeChat", data);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		return true;
	}
	
	public void broadcast( Packet packet )
	{
		Debugger.logp("Broadcast %s", packet);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);
		try
		{
			PacketRegistry.write(packet, out);
			byte[] data = stream.toByteArray();
			
			mComLink.broadcastMessage("BungeeChat", data);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void sendInitPackets()
	{
		// Notify proxy that this server is now online
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);
		
		try
		{
			Debugger.logp("Sending online message");
			out.writeUTF("Online");
			
			byte[] data = stream.toByteArray();
			mComLink.sendMessage("BCState", data);
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
			Debugger.logp("Sending schema to proxy");
			PacketRegistry.writeSchemaPacket(out);
			
			byte[] data = stream.toByteArray();
			mComLink.sendMessage("BCState", data);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void handleDataPacket(DataInput in)
	{
		Packet packet = null;
		try
		{
			packet = mCodec.read(in);
			if(packet == null)
				return;
			
			Debugger.logp("Received packet %s", packet.toString());
			
			// Handler spec handlers
			for(IPacketHandler handler : mHandlers.get(packet.getClass()))
				handler.handle(packet);
			
			// Handle non spec handlers
			for(IPacketHandler handler : mHandlers.get(null))
				handler.handle(packet);
		}
		catch(Throwable e)
		{
			BungeeChat.getInstance().getLogger().severe("An error occured while handling packet " + packet + ":");
			e.printStackTrace();
		}
	}
	
	@Override
	public void onReceive( String channel, final DataInput in, MessageSender sender )
	{
		if(channel.equals("BungeeChat"))
		{
			if(mCodec == null)
			{
				Debugger.logp("Received packet. Pending codec.");
				mPendingPackets.add(in);
			}
			else
			{
				Bukkit.getScheduler().runTask(BungeeChat.getInstance(), new Runnable()
				{
					@Override
					public void run()
					{
						handleDataPacket(in);
					}
				});
			}
		}
		else if(channel.equals("BCState"))
		{
			try
			{
				String type = in.readUTF();
				if(type.equals("Schema"))
				{
					Debugger.logp("Received Schema.");
					mCodec = PacketCodec.fromSchemaData(in);
					doPending();
					
					// We may be out of date, request the update to resync
					BungeeChat.getInstance().requestUpdate();
				}
				else if (type.equals("SchemaRequest"))
				{
					Debugger.logp("Received Schema Request.");
					ByteArrayOutputStream ostream = new ByteArrayOutputStream();
					DataOutputStream out = new DataOutputStream(ostream);
					PacketRegistry.writeSchemaPacket(out);
					
					byte[] odata = ostream.toByteArray();
					mComLink.sendMessage("BCState", odata);
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private void doPending()
	{
		if(mCodec == null)
			return;
		
		Iterator<DataInput> it = mPendingPackets.iterator();
		
		while(it.hasNext())
		{
			DataInput data = it.next();
			it.remove();
			Debugger.logp("Do pending:");
			handleDataPacket(data);
		}
	}
	
	@Override
	public void onConnectionLost( Throwable e )
	{
	}

	@Override
	public void onConnectionRestored()
	{
		Bukkit.getScheduler().runTask(BungeeChat.getInstance(), new Runnable()
		{
			@Override
			public void run()
			{
				System.out.println("[BungeeChat] Redis connection restored");
				sendInitPackets();
				BungeeChat.getInstance().requestUpdate();
			}
		});
	}

	
}
