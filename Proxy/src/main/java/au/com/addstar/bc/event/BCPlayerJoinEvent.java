package au.com.addstar.bc.event;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;

public class BCPlayerJoinEvent extends Event
{
	private ProxiedPlayer mPlayer;
	private String mJoinMessage;
	
	public BCPlayerJoinEvent(ProxiedPlayer player, String message)
	{
		mPlayer = player;
		mJoinMessage = message;
	}
	
	public ProxiedPlayer getPlayer()
	{
		return mPlayer;
	}
	
	public String getJoinMessage()
	{
		return mJoinMessage;
	}
	
	public void setJoinMessage(String message)
	{
		mJoinMessage = message;
	}
}
