package au.com.addstar.bc.sync;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;

public class PropertyChangeEvent extends Event
{
	private ProxiedPlayer mPlayer;
	private String mProperty;
	private Object mOldValue;
	private Object mNewValue;
	
	public PropertyChangeEvent(ProxiedPlayer player, String property, Object oldVal, Object newVal)
	{
		mPlayer = player;
		mProperty = property;
		mOldValue = oldVal;
		mNewValue = newVal;
	}
	
	public ProxiedPlayer getPlayer()
	{
		return mPlayer;
	}
	
	public String getProperty()
	{
		return mProperty;
	}
	
	public Object getOldValue()
	{
		return mOldValue;
	}
	
	public Object getNewValue()
	{
		return mNewValue;
	}
}
