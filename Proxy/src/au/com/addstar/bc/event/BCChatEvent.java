package au.com.addstar.bc.event;

import net.md_5.bungee.api.plugin.Event;

public class BCChatEvent extends Event
{
	private String mChannel;
	private String mMessage;
	
	public BCChatEvent(String channel, String message)
	{
		mChannel = channel;
		mMessage = message;
	}
	
	public String getChannel()
	{
		return mChannel;
	}
	
	public String getMessage()
	{
		return mMessage;
	}
}
