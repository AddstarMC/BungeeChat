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
import au.com.addstar.bc.objects.Formatter;
import au.com.addstar.bc.objects.PlayerSettings;
import au.com.addstar.bc.objects.RemotePlayer;
import au.com.addstar.bc.utils.Utilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
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

	private void doSendMessage(CommandSender to, CommandSender from, Component message)
	{
		Component fullMessageOut = Formatter.replaceMessage(Formatter.getPMFormat(to, false),message);
		Component fullMessageIn = Formatter.replaceMessage(Formatter.getPMFormat(from, true), message);
		
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
		Utilities.getAudienceProvider().sender(to).sendMessage(fullMessageIn);
		Utilities.getAudienceProvider().sender(from).sendMessage(fullMessageOut);
		
		BungeeChat.setLastMsgTarget(from, to);
		BungeeChat.setLastMsgTarget(to, from);
		
		BungeeChat.getAFKHandler().checkAFK(from, to,
			Component.text("This player is AFK. They may not see this message.").color(NamedTextColor.GRAY));
	}
	
	@Override
	public boolean onCommand( final CommandSender sender, Command command, String label, String[] args )
	{
		switch (command.getName()) {
			case "tell": {
				if (args.length < 2)
					return false;

				if (BungeeChat.getPlayerManager().isPlayerMuted(sender)) {
					Utilities.getAudienceProvider().sender(sender)
						.sendMessage(Component.text("You are muted. You may not talk").color(NamedTextColor.AQUA));
					return true;
				}

				final CommandSender player = BungeeChat.getPlayerManager().getPlayer(args[0]);
				if (player == null) {
					Utilities.getAudienceProvider().sender(sender)
						.sendMessage(Component.text("Cannot find player " + args[0]).color(NamedTextColor.RED));
					return true;
				}

				final Component message = Utilities.colorize(StringUtils.join(args, ' ', 1, args.length), sender);

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
									Utilities.getAudienceProvider().sender(sender)
										.sendMessage(Component.text("That player has messaging disabled.").color(NamedTextColor.RED));
								else
									doSendMessage(player, sender, message);
							}

						}, PlayerManager.getUniqueId(player));
						return true;
					} else if (player instanceof Player) {
						if (!BungeeChat.getPlayerManager().getPlayerSettings(player).msgEnabled) {
							Utilities.getAudienceProvider().sender(sender)
								.sendMessage(Component.text("That player has messaging disabled.").color(NamedTextColor.RED));
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
					Utilities.getAudienceProvider().sender(sender)
						.sendMessage(Component.text("You are muted. You may not talk").color(NamedTextColor.AQUA));
					return true;
				}

				CommandSender player = BungeeChat.getPlayerManager().getPlayerSettings(sender).getLastMsgTarget();
				if (player == null) {
					Utilities.getAudienceProvider().sender(sender)
						.sendMessage(Component.text("You have nobody to reply to").color(NamedTextColor.RED));
					return true;
				}

				Component message = Utilities.colorize(StringUtils.join(args, ' ', 0, args.length),sender);
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
					Utilities.getAudienceProvider().sender(sender)
						.sendMessage(Component.text("Incoming Messaging Enabled").color(NamedTextColor.GREEN));
				else
					Utilities.getAudienceProvider().sender(sender)
						.sendMessage(Component.text("Incoming Messaging Disabled").color(NamedTextColor.GOLD));
				BungeeChat.getPlayerManager().updatePlayerSettings(sender);
				return true;
		}
		return false;
	}
}
