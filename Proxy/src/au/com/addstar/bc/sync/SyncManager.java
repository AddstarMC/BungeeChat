package au.com.addstar.bc.sync;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.WeakHashMap;

import au.com.addstar.bc.MessageOutput;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class SyncManager implements Listener
{
	private HashMap<String, SyncMethod> mMethods;
	private HashMap<String, SyncConfig> mConfigs;
	
	private WeakHashMap<ProxiedPlayer, HashMap<String, Object>> mPlayerProperties;
	
	public SyncManager(Plugin plugin)
	{
		ProxyServer.getInstance().getPluginManager().registerListener(plugin, this);
		ProxyServer.getInstance().registerChannel("BungeeSync");
		mMethods = new HashMap<String, SyncMethod>();
		mConfigs = new HashMap<String, SyncConfig>();
		mPlayerProperties = new WeakHashMap<ProxiedPlayer, HashMap<String,Object>>();
		
		StorageMethods storage = new StorageMethods();
		addMethod("bungee:setProperty", storage);
		addMethod("bungee:getProperty", storage);
		addMethod("bungee:getProperties", storage);
	}
	
	public void addMethod(String name, SyncMethod method)
	{
		mMethods.put(name, method);
	}
	
	public void setConfig(String name, SyncConfig config)
	{
		mConfigs.put(name, config);
		sendConfig(name);
	}
	
	@EventHandler
	public void onMessageReceived(PluginMessageEvent event)
	{
		if(!(event.getSender() instanceof Server))
			return;
		
		if(event.getTag().equals("BungeeSync"))
		{
			ByteArrayInputStream stream = new ByteArrayInputStream(event.getData());
			DataInputStream input = new DataInputStream(stream);
			
			try
			{
				String subChannel = input.readUTF();
				onDataReceived(subChannel, input, ((Server)event.getSender()).getInfo());
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	@EventHandler(priority=127)
	public void onPlayerDisconnect(PlayerDisconnectEvent event)
	{
		mPlayerProperties.remove(event.getPlayer());
	}
	
	private void onDataReceived(String channel, DataInput input, ServerInfo caller) throws IOException
	{
		if(channel.equals("Call"))
		{
			doSyncMethod(input, caller);
		}
		else if(channel.equals("ConfigUpdate"))
		{
			String config = input.readUTF();
			sendConfig(config, caller);
		}
	}
	
	private void doSyncMethod(DataInput input, ServerInfo caller) throws IOException
	{
		String name = input.readUTF();
		int id = input.readInt();
		int count = input.readUnsignedByte();
		
		SyncMethod method = mMethods.get(name);
		
		if(method != null)
		{
			try
			{
				Object[] args = new Object[count];
				for(int i = 0; i < count; ++i)
					args[i] = SyncUtil.readObject(input);
				
				Object result = method.run(name, caller, args);
				
				MessageOutput out = new MessageOutput("BungeeSync", "CallRes")
					.writeInt(id)
					.writeBoolean(true);
				
				SyncUtil.writeObject(out.asDataOutput(), result);
				out.send(caller);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				new MessageOutput("BungeeSync", "CallRes")
				.writeInt(id)
				.writeBoolean(false)
				.writeUTF(e.getClass().getSimpleName())
				.writeUTF(e.getMessage() == null ? "" : e.getMessage())
				.send(caller);
			}
		}
		else
		{
			new MessageOutput("BungeeSync", "CallRes")
				.writeInt(id)
				.writeBoolean(false)
				.writeUTF("NoSuchMethodException")
				.writeUTF(name + " cannot be found")
				.send(caller);
		}
	}
	
	public void sendConfig(String name, ServerInfo server)
	{
		SyncConfig config = mConfigs.get(name);
		if(config == null)
			return;
		
		MessageOutput out = new MessageOutput("BungeeSync", "ConfigSync")
			.writeUTF(name);
		try
		{
			config.write(out.asDataOutput());
		}
		catch(IOException e)
		{
			// Cant happen
		}
		
		if(server == null)
			out.send(true);
		else
			out.send(server);
	}
	
	public void sendConfig(String name)
	{
		sendConfig(name, null);
	}
	
	public void setProperty(ProxiedPlayer player, String property, Object value)
	{
		HashMap<String, Object> values = mPlayerProperties.get(player);
		if(values == null)
		{
			values = new HashMap<String, Object>();
			mPlayerProperties.put(player, values);
		}
		
		Object oldValue;
		if(value == null)
			oldValue = values.remove(property);
		else
			oldValue = values.put(property, value);
		
		ProxyServer.getInstance().getPluginManager().callEvent(new PropertyChangeEvent(player, property, oldValue, value));
	}
	
	public Object getProperty(ProxiedPlayer player, String property)
	{
		HashMap<String, Object> values = mPlayerProperties.get(player);
		if(values == null)
			return null;
		
		return values.get(property);
	}
	
	public Map<String, Object> getProperties(String property)
	{
		HashMap<String, Object> values = new HashMap<String, Object>();
		for(Entry<ProxiedPlayer, HashMap<String, Object>> map : mPlayerProperties.entrySet())
		{
			HashMap<String, Object> properties = map.getValue();
			if(properties.containsKey(property))
				values.put(map.getKey().getUniqueId().toString(), properties.get(property));
		}
		
		return values;
	}
	
	public boolean getPropertyBoolean(ProxiedPlayer player, String property, boolean def)
	{
		Object value = getProperty(player, property);
		if(value == null)
			return def;

		if(value instanceof Number)
			return ((Number)value).byteValue() != 0;
		if(value instanceof Boolean)
			return (Boolean)value;
		
		return def;
	}
	
	public Collection<String> getPropertyNames(ProxiedPlayer player, String prefix)
	{
		HashMap<String, Object> values = mPlayerProperties.get(player);
		if(values == null)
			return Collections.emptySet();
		
		HashSet<String> properties = new HashSet<String>();
		for(String key : values.keySet())
		{
			if(key.startsWith(prefix))
				properties.add(key);
		}
		
		return properties;
	}
	
	private class StorageMethods implements SyncMethod
	{
		@Override
		public Object run( String name, ServerInfo server, Object... arguments )
		{
			if(name.equals("bungee:setProperty"))
			{
				if(arguments.length != 3)
					throw new IllegalArgumentException("Arguments: <player> <property> <value>");
				
				ProxiedPlayer player = ProxyServer.getInstance().getPlayer((UUID)arguments[0]);
				if(player == null)
					throw new IllegalArgumentException("Unknown player");
				
				setProperty(player, (String)arguments[1], arguments[2]);
				
				return null;
			}
			else if(name.equals("bungee:getProperty"))
			{
				if(arguments.length != 2)
					throw new IllegalArgumentException("Arguments: <player> <property>");
				
				ProxiedPlayer player = ProxyServer.getInstance().getPlayer((UUID)arguments[0]);
				if(player == null)
					throw new IllegalArgumentException("Unknown player");
				
				return getProperty(player, (String)arguments[1]);
			}
			else if(name.equals("bungee:getProperties"))
			{
				if(arguments.length != 1)
					throw new IllegalArgumentException("Arguments: <property>");
				
				return getProperties((String)arguments[0]);
			}
			
			return null;
		}
	}
}
