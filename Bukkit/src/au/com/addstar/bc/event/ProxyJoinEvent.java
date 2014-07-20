package au.com.addstar.bc.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ProxyJoinEvent extends Event
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
	
	
	public ProxyJoinEvent(Player player, String joinMessage)
	{
		mPlayer = player;
		mMessage = joinMessage;
	}
	
	public Player getPlayer()
	{
		return mPlayer;
	}
	
	public String getJoinMessage()
	{
		return mMessage;
	}
	
	public void setJoinMessage(String message)
	{
		mMessage = message;
	}
}
