package au.com.addstar.bc.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import au.com.addstar.bc.objects.ChannelType;

public class ChatChannelEvent extends Event
{
	private static HandlerList handlers = new HandlerList();
	
	private String mChannel;
	private ChannelType mType;
	private String mMessage;
	
	public ChatChannelEvent(String channel, ChannelType type, String message)
	{
		mChannel = channel;
		mType = type;
		mMessage = message;
	}
	
	public String getChannelName()
	{
		return mChannel;
	}
	
	public ChannelType getChannelType()
	{
		return mType;
	}
	
	public String getMessage()
	{
		return mMessage;
	}
	
	@Override
	public HandlerList getHandlers()
	{
		return handlers;
	}

	public static HandlerList getHandlerList()
	{
		return handlers;
	}
}
