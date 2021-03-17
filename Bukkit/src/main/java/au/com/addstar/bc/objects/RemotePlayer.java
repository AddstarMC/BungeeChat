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

/*-
 * #%L
 * BungeeChat-Bukkit
 * %%
 * Copyright (C) 2015 - 2020 AddstarMC
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.util.Set;
import java.util.UUID;

import au.com.addstar.bc.BungeeChat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

	public Component getChatName(){
		Component name = BungeeChat.getPlayerManager().getPlayerChatName(this);
		if(name == Component.empty())
			return Component.text(mName);
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
	public @NotNull Spigot spigot() {
		return new RemoteSpigot(mId);
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

	public void sendMessage( Component message )
	{
		BungeeChat.sendRemoteMessage(this, message);
	}

	@Override
	public void sendMessage(@NotNull String message) {
		this.sendMessage(MiniMessage.get().parse(message));
	}

	@Override
	public void sendMessage( String[] message )
	{
		for(String m : message)
			this.sendMessage(m);
	}

	@Override
	public void sendMessage(@Nullable UUID sender, @NotNull String message) {
		//todo BungeeChat doesnt not respect identities when cross server sending messages.
		BungeeChat.sendRemoteMessage(this,MiniMessage.get().parse(message));
	}

	@Override
	public void sendMessage(@Nullable UUID sender, @NotNull String[] messages) {
		for (String m: messages
			 ) {
			this.sendMessage(sender,m);
		}
	}

	private static class RemoteSpigot extends Spigot{
		private final UUID uuid;

		RemoteSpigot(UUID uuid) {
			super();
			this.uuid = uuid;
		}

		@Override
		public void sendMessage(@NotNull BaseComponent component) {
			BaseComponent[] c = {component};
			this.sendMessage(c);
		}

		@Override
		public void sendMessage(@NotNull BaseComponent... components) {
			BungeeChat.sendRemoteMessage(uuid,BungeeComponentSerializer.get().deserialize(components));
		}
	}
}
