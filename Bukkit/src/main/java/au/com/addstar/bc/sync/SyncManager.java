package au.com.addstar.bc.sync;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.event.ConfigReceiveEvent;
import au.com.addstar.bc.sync.packet.CallFailedResponsePacket;
import au.com.addstar.bc.sync.packet.CallPacket;
import au.com.addstar.bc.sync.packet.CallSuccessResponsePacket;
import au.com.addstar.bc.sync.packet.ConfigPacket;
import au.com.addstar.bc.sync.packet.ConfigRequestPacket;

public class SyncManager implements IPacketHandler
{
	public static final int protocolVersion = 1;
	
	private int mNextId = 0;
	
	private HashMap<Integer, IMethodCallback<Object>> mWaitingCallbacks;
	
	@SuppressWarnings( "unchecked" )
	public SyncManager()
	{
		mWaitingCallbacks = new HashMap<>();
		BungeeChat.getPacketManager().addHandler(this, ConfigPacket.class, CallFailedResponsePacket.class, CallSuccessResponsePacket.class);
	}
	
	@Override
	public void handle( Packet packet )
	{
		if(packet instanceof ConfigPacket)
			Bukkit.getPluginManager().callEvent(new ConfigReceiveEvent(((ConfigPacket) packet).getName(), ((ConfigPacket) packet).getConfig()));
		else if(packet instanceof CallFailedResponsePacket)
			onCallFailed((CallFailedResponsePacket)packet);
		else if(packet instanceof CallSuccessResponsePacket)
			onCallSuccess((CallSuccessResponsePacket)packet);
	}

	public void callSyncMethod(String method, IMethodCallback<?> callback, Object... args)
	{
		int id = mNextId++;
		
		BungeeChat.getPacketManager().send(new CallPacket(method, id, args));
				
		if(callback != null)
			mWaitingCallbacks.put(id, (IMethodCallback<Object>) callback);
	}
	
	private void onCallFailed(CallFailedResponsePacket packet)
	{
		IMethodCallback<Object> callback = mWaitingCallbacks.remove(packet.getId());
		
		if(callback == null)
			return;
		
		callback.onError(packet.getErrorName(), packet.getErrorMessage());
	}
	
	private void onCallSuccess(CallSuccessResponsePacket packet)
	{
		IMethodCallback<Object> callback = mWaitingCallbacks.remove(packet.getId());
		
		if(callback == null)
			return;
		
		callback.onFinished(packet.getResult());
	}
	
	public void requestConfigUpdate(String name)
	{
		BungeeChat.getPacketManager().send(new ConfigRequestPacket(name));
	}
	
	/**
	 * Stores a property against a player for as long as they are on the proxy 
	 */
	public void setPlayerProperty(UUID player, String property, Object value)
	{
		callSyncMethod("bungee:setProperty", null, player, property, value);
	}
	
	public void getPlayerPropertyAsync(UUID player, String property, IMethodCallback<Object> callback)
	{
		callSyncMethod("bungee:getProperty", callback, player, property);
	}
	
	/**
	 * Gets all values for the property and who has that value
	 * @param property The property to check
	 * @param callback A callback to get the data
	 *  through callback, Map< String, Object >: uuid of player as string, value of property
	 */
	public void getPropertiesAsync(String property, IMethodCallback<Map<String, Object>> callback)
	{
		callSyncMethod("bungee:getProperties", callback, property);
	}
}
