package au.com.addstar.bc.sync;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import au.com.addstar.bc.MessageOutput;

public class SyncManager implements PluginMessageListener
{
	public static final int protocolVersion = 1;
	
	private int mNextId = 0;
	
	private HashMap<Integer, IMethodCallback<Object>> mWaitingCallbacks;
	
	private Plugin mPlugin;
	public SyncManager(Plugin plugin)
	{
		mPlugin = plugin;
		Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "BungeeSync", this);
		mWaitingCallbacks = new HashMap<Integer, IMethodCallback<Object>>();
	}
	
	@Override
	public void onPluginMessageReceived( String channel, Player player, byte[] data )
	{
		ByteArrayInputStream stream = new ByteArrayInputStream(data);
		DataInputStream input = new DataInputStream(stream);
		
		try
		{
			String subChannel = input.readUTF();
			onDataReceived(subChannel, input);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void onDataReceived(String channel, DataInput input) throws IOException
	{
		if(channel.equals("ConfigSync"))
		{
			String name = input.readUTF();
			SyncConfig config = new SyncConfig();
			config.load(input);
			
			Bukkit.getPluginManager().callEvent(new ConfigReceiveEvent(name, config));
		}
		else if(channel.equals("CallRes"))
		{
			int id = input.readInt();
			onMethodResult(id, input);
		}
	}
	
	public void callSyncMethod(String method, IMethodCallback<Object> callback, Object... args)
	{
		int id = mNextId++;
		MessageOutput out = new MessageOutput("BungeeSync", "Call")
			.writeUTF(method)
			.writeInt(id)
			.writeByte(args.length);

		try
		{
			for(int i = 0; i < args.length; ++i)
				SyncUtil.writeObject(out.asDataOutput(), args[i]);
		}
		catch(IOException e)
		{
			// Cant happen
		}
		
		out.send(mPlugin);
		
		mWaitingCallbacks.put(id, callback);
	}
	
	private void onMethodResult(int id, DataInput input) throws IOException
	{
		IMethodCallback<Object> callback = mWaitingCallbacks.remove(id);
		
		if(callback == null)
			return;
		
		boolean success = input.readBoolean();
		
		if(success)
			callback.onFinished(SyncUtil.readObject(input));
		else
			callback.onError(input.readUTF(), input.readUTF());
	}
	
	public void requestConfigUpdate(String name)
	{
		new MessageOutput("BungeeSync", "ConfigUpdate")
			.writeUTF(name)
			.send(mPlugin);
	}
}
