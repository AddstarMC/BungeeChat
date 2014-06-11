package au.com.addstar.bc;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called on the last server a player was on when they leave the proxy.
 * NOTE: If there are no players left on the server (or on others) there will be no event fired.
 */
public class ProxyLeaveEvent extends Event
{
	private static HandlerList handlers = new HandlerList();
	
	@Override
	public HandlerList getHandlers()
	{
		return handlers;
	}
	
	public static HandlerList getHandlerList()
	{
		return handlers;
	}
	
	private OfflinePlayer mPlayer;
	private String mDisplayName;
	private String mMessage;
	
	public ProxyLeaveEvent(OfflinePlayer player, String displayName, String message)
	{
		mPlayer = player;
		mDisplayName = displayName;
		mMessage = message;
	}
	
	public OfflinePlayer getPlayer()
	{
		return mPlayer;
	}
	
	public String getDisplayName()
	{
		return mDisplayName;
	}
	
	public String getQuitMessage()
	{
		return mMessage;
	}
	
	public void setQuitMessage(String message)
	{
		mMessage = message;
	}
}
