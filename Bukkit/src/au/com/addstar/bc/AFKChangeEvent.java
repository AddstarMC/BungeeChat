package au.com.addstar.bc;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AFKChangeEvent extends Event implements Cancellable
{
	private static HandlerList handlers = new HandlerList();
	
	private Player mPlayer;
	private boolean mAFK;
	
	private boolean mCancelled;
	
	public AFKChangeEvent(Player player, boolean toAFK)
	{
		mPlayer = player;
		mAFK = toAFK;
		
		mCancelled = false;
	}
	
	@Override
	public boolean isCancelled()
	{
		return mCancelled;
	}

	@Override
	public void setCancelled( boolean cancelled )
	{
		mCancelled = cancelled;
	}
	
	public Player getPlayer()
	{
		return mPlayer;
	}
	
	public boolean toAFK()
	{
		return mAFK;
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
