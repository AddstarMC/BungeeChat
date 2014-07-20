package au.com.addstar.bc.event;

import org.bukkit.entity.Player;
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
	
	private Player mPlayer;
	private String mMessage;
	
	public ProxyLeaveEvent(Player player, String message)
	{
		mPlayer = player;
		mMessage = message;
	}
	
	public Player getPlayer()
	{
		return mPlayer;
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
