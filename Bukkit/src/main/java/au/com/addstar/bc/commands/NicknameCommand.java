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
import java.util.regex.Pattern;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.PlayerManager;
import au.com.addstar.bc.objects.RemotePlayer;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class NicknameCommand implements CommandExecutor, TabCompleter
{
	private Pattern mAllowed = Pattern.compile("^[a-zA-Z0-9_]+$");
	
	@Override
	public List<String> onTabComplete( CommandSender sender, Command cmd, String label, String[] args )
	{
		if(args.length == 1)
			return BungeeChat.getPlayerManager().matchNames(args[0]);
		return null;
	}

	@Override
	public boolean onCommand( CommandSender sender, Command cmd, String label, String[] args )
	{
		if(args.length != 1 && args.length != 2)
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

		String name = args[args.length-1];
		if(name.equalsIgnoreCase("off"))
		{
			manager.setPlayerNickname(player, "");
			BungeeChat.audiences.sender(sender).sendMessage(
				TextComponent.of("Removed " + player.getName() + "'s nickname").color(NamedTextColor.GREEN));
		}
		else
		{
			if(!mAllowed.matcher(name).matches())
			{
				BungeeChat.audiences.sender(sender).sendMessage(
					TextComponent.of("Invalid characters in nickname. Can only use a-z 0-9 and _").color(NamedTextColor.RED));
				return true;
			}
			
			if (name.length() > 16)
			{
				BungeeChat.audiences.sender(sender).sendMessage(
					TextComponent.of("Nickname cannot be longer than 16 characters").color(NamedTextColor.RED));
				return true;
			}
			
			CommandSender other = manager.getPlayerExact(name);
			// Allow them to change the case of their name, but not to any other existing name
			if(other != null && other != player)
			{
				BungeeChat.audiences.sender(sender).sendMessage(
					TextComponent.of("You cannot nickname someone to an existing name").color(NamedTextColor.RED));
				return true;
			}
			
			manager.setPlayerNickname(player, name);
			BungeeChat.audiences.sender(sender).sendMessage(
				TextComponent.of("Nickname changed").color(NamedTextColor.GREEN));
		}
		
		return true;
	}

}
