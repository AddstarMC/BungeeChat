package au.com.addstar.bc;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

public class RemotePlayer implements CommandSender
{
	private String mName;
	
	public RemotePlayer(String player)
	{
		mName = player;
	}
	
	@Override
	public PermissionAttachment addAttachment( Plugin arg0 )
	{
		return null;
	}

	@Override
	public PermissionAttachment addAttachment( Plugin arg0, int arg1 )
	{
		return null;
	}

	@Override
	public PermissionAttachment addAttachment( Plugin arg0, String arg1, boolean arg2 )
	{
		return null;
	}

	@Override
	public PermissionAttachment addAttachment( Plugin arg0, String arg1, boolean arg2, int arg3 )
	{
		return null;
	}

	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions()
	{
		return null;
	}

	@Override
	public boolean hasPermission( String perm )
	{
		return BungeeChat.permissionManager.has(Bukkit.getWorlds().get(0), mName, perm);
	}

	@Override
	public boolean hasPermission( Permission perm )
	{
		return BungeeChat.permissionManager.has(Bukkit.getWorlds().get(0), mName, perm.getName());
	}

	@Override
	public boolean isPermissionSet( String perm )
	{
		return hasPermission(perm);
	}

	@Override
	public boolean isPermissionSet( Permission perm )
	{
		return hasPermission(perm);
	}

	@Override
	public void recalculatePermissions()
	{
	}

	@Override
	public void removeAttachment( PermissionAttachment arg0 )
	{
	}

	@Override
	public boolean isOp()
	{
		return false;
	}

	@Override
	public void setOp( boolean arg0 )
	{
	}

	@Override
	public String getName()
	{
		return mName;
	}

	@Override
	public Server getServer()
	{
		return null;
	}

	@Override
	public void sendMessage( String message )
	{
		BungeeChat.sendMessage(this, message);
	}

	@Override
	public void sendMessage( String[] message )
	{
		for(String m : message)
			BungeeChat.sendMessage(this, m);
	}

}
