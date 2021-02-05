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

package au.com.addstar.bc.commands;

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

import java.util.List;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.PlayerManager;
import au.com.addstar.bc.objects.RemotePlayer;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import au.com.addstar.bc.sync.IMethodCallback;

public class SkinCommand implements CommandExecutor, TabCompleter
{
	@Override
	public List<String> onTabComplete( CommandSender sender, Command cmd, String label, String[] args )
	{
		if(args.length == 1)
			return BungeeChat.getPlayerManager().matchNames(args[0]);
		return null;
	}

	@Override
	public boolean onCommand( final CommandSender sender, Command cmd, String label, String[] args )
	{
		if (args.length != 1 && args.length != 2)
			return false;
		
		PlayerManager manager = BungeeChat.getPlayerManager();
		
		CommandSender player = sender;
		
		if(args.length == 2)
		{
			player = manager.getPlayer(args[0]);
			if(!(player instanceof Player || player instanceof RemotePlayer))
			{
				BungeeChat.audiences.sender(sender).sendMessage(
					TextComponent.of("Unknown player " + args[0]).color(NamedTextColor.RED));
				return true;
			}
		}
		else if(!(sender instanceof Player))
		{
			BungeeChat.audiences.sender(sender).sendMessage(
				TextComponent.of("A player name must be specified if not called by a player.").color(NamedTextColor.RED));
			return true;
		}

		final String name = args[args.length-1];
		if(name.equalsIgnoreCase("off"))
		{
			BungeeChat.getSyncManager().callSyncMethod("bchat:setSkin", null, PlayerManager.getUniqueId(player), null);

			BungeeChat.audiences.sender(sender).sendMessage(
				TextComponent.of("Restored " + player.getName() + "'s skin").color(NamedTextColor.GREEN));
		}
		else
		{
			BungeeChat.audiences.sender(sender).sendMessage(
				TextComponent.of("Looking up skin for " + name).color(NamedTextColor.GREEN));
			final String playerName = player.getName();
			@SuppressWarnings( "deprecation" )
			OfflinePlayer oplayer = Bukkit.getOfflinePlayer(name);
			Object target;
			// Online mode UUIDs are version 4 and so represent actual players
			if (oplayer.getUniqueId().version() == 4)
				target = oplayer.getUniqueId();
			else
				target = name;
			
			BungeeChat.getSyncManager().callSyncMethod("bchat:setSkin", new IMethodCallback<Void>()
			{
				@Override
				public void onFinished( Void data )
				{
					BungeeChat.audiences.sender(sender).sendMessage(
						TextComponent.of("Setting " + playerName + "'s skin to be " + name + "'s skin.").color(NamedTextColor.GREEN));
				}
				
				@Override
				public void onError( String type, String message )
				{
					BungeeChat.audiences.sender(sender).sendMessage(
						TextComponent.of(message).color(NamedTextColor.RED));
				}
			}, PlayerManager.getUniqueId(player), target);
		}
		
		return true;
	}

}
