package au.com.addstar.bc.event;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;

public class BCPlayerQuitEvent extends Event
{
	private ProxiedPlayer mPlayer;
	private String mQuitMessage;
	
	public BCPlayerQuitEvent(ProxiedPlayer player, String message)
	{
		mPlayer = player;
		mQuitMessage = message;
	}
	
	public ProxiedPlayer getPlayer()
	{
		return mPlayer;
	}
	
	public String getQuitMessage()
	{
		return mQuitMessage;
	}
	
	public void setQuitMessage(String message)
	{
		mQuitMessage = message;
	}
}
