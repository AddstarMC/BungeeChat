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

import java.util.List;

import au.com.addstar.bc.*;
import au.com.addstar.bc.objects.Formatter;
import au.com.addstar.bc.objects.PlayerSettings;
import au.com.addstar.bc.objects.RemotePlayer;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import au.com.addstar.bc.sync.IMethodCallback;

public class MessageCommand implements CommandExecutor, TabCompleter
{
	@Override
	public List<String> onTabComplete( CommandSender sender, Command command, String label, String[] args )
	{
		if(command.getName().equals("tell"))
		{
			if(args.length == 1)
				return BungeeChat.getPlayerManager().matchNames(args[0]);
		}
		
		return null;
	}

	private void doSendMessage(CommandSender to, CommandSender from, String message)
	{
		String fullMessageOut = String.format(Formatter.getPMFormat(to, false), message);
		String fullMessageIn = String.format(Formatter.getPMFormat(from, true), message);
		
		Debugger.logCorrect(to);
		
		if (to instanceof RemotePlayer)
		{
			Player player = Bukkit.getPlayer(((RemotePlayer) to).getUniqueId());
			if (player != null)
			{
				BungeeChat.getInstance().getLogger().severe("Attempted to message wrong remote player object for local player. " + to.getName());
				to = player;
			}
		}
		
		to.sendMessage(fullMessageIn);
		from.sendMessage(fullMessageOut);
		
		BungeeChat.setLastMsgTarget(from, to);
		BungeeChat.setLastMsgTarget(to, from);
		
		BungeeChat.getAFKHandler().checkAFK(from, to, ChatColor.GRAY + "This player is AFK. They may not see this message.");
	}
	
	@Override
	public boolean onCommand( final CommandSender sender, Command command, String label, String[] args )
	{
		switch (command.getName()) {
			case "tell": {
				if (args.length < 2)
					return false;

				if (BungeeChat.getPlayerManager().isPlayerMuted(sender)) {
					sender.sendMessage(ChatColor.AQUA + "You are muted. You may not talk");
					return true;
				}

				final CommandSender player = BungeeChat.getPlayerManager().getPlayer(args[0]);
				if (player == null) {
					sender.sendMessage(ChatColor.RED + "Cannot find player " + args[0]);
					return true;
				}

				final String message = BungeeChat.colorize(StringUtils.join(args, ' ', 1, args.length), sender);

				if (ChatColor.stripColor(message).trim().isEmpty())
					return false;

				if ((sender instanceof Player) && !sender.hasPermission("bungeechat.message.override")) {
					if (player instanceof RemotePlayer) {
						BungeeChat.getSyncManager().callSyncMethod("bchat:canMsg", new IMethodCallback<Boolean>() {
							@Override
							public void onError(String type, String message) {
								throw new RuntimeException(type + ": " + message);
							}

							@Override
							public void onFinished(Boolean data) {
								if (!data)
									sender.sendMessage(ChatColor.RED + "That player has messaging disabled.");
								else
									doSendMessage(player, sender, message);
							}

						}, PlayerManager.getUniqueId(player));
						return true;
					} else if (player instanceof Player) {
						if (!BungeeChat.getPlayerManager().getPlayerSettings(player).msgEnabled) {
							sender.sendMessage(ChatColor.RED + "That player has messaging disabled.");
							return true;
						}
					}
				}

				doSendMessage(player, sender, message);

				return true;
			}
			case "reply": {
				if (args.length == 0)
					return false;

				if (BungeeChat.getPlayerManager().isPlayerMuted(sender)) {
					sender.sendMessage(ChatColor.AQUA + "You are muted. You may not talk");
					return true;
				}

				CommandSender player = BungeeChat.getPlayerManager().getPlayerSettings(sender).getLastMsgTarget();
				if (player == null) {
					sender.sendMessage(ChatColor.RED + "You have nobody to reply to");
					return true;
				}

				String message = StringUtils.join(args, ' ', 0, args.length);
				message = BungeeChat.colorize(message, sender);

				if (ChatColor.stripColor(message).trim().isEmpty())
					return false;

				doSendMessage(player, sender, message);

				return true;
			}
			case "msgtoggle":
				if (args.length != 0)
					return false;

				if (!(sender instanceof Player))
					return false;

				PlayerSettings settings = BungeeChat.getPlayerManager().getPlayerSettings(sender);
				settings.msgEnabled = !settings.msgEnabled;

				if (settings.msgEnabled)
					sender.sendMessage(ChatColor.GREEN + "Incoming Messaging Enabled");
				else
					sender.sendMessage(ChatColor.GOLD + "Incoming Messaging Disabled");

				BungeeChat.getPlayerManager().updatePlayerSettings(sender);
				return true;
		}
		return false;
	}
}
