/*
 * BungeeChat
 *
 * Copyright (c) 2015 - 2020.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy   of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is *
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR  IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package au.com.addstar.bc.objects;

import java.util.Set;
import java.util.UUID;

import au.com.addstar.bc.BungeeChat;
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

	public String getChatName(){
		String name = BungeeChat.getPlayerManager().getPlayerChatName(this);
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
