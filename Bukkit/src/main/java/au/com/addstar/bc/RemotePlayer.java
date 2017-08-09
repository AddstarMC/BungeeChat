package au.com.addstar.bc;

import java.util.Set;
import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

public class RemotePlayer implements CommandSender
{
	private UUID mId;
	private String mName;
	
	public RemotePlayer(UUID id, String name)
	{
		mId = id;
		mName = name;
	}
	
	public String getDisplayName()
	{
		String name = BungeeChat.getPlayerManager().getPlayerNickname(this);
		if(name == null || name.isEmpty())
			return mName;
		return name;
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
		return false;
	}

	@Override
	public boolean hasPermission( Permission perm )
	{
		return false;
	}

	@Override
	public boolean isPermissionSet( String perm )
	{
		return false;
	}

	@Override
	public boolean isPermissionSet( Permission perm )
	{
		return false;
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
	public Spigot spigot() {
		return null;
	}

	public UUID getUniqueId()
	{
		return mId;
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
