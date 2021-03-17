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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.objects.ChannelType;
import au.com.addstar.bc.PlayerManager;
import au.com.addstar.bc.objects.RemotePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import au.com.addstar.bc.sync.IMethodCallback;
import au.com.addstar.bc.utils.Utilities;

public class MuteHandler implements CommandExecutor, TabCompleter
{
	public MuteHandler(Plugin plugin)
	{
	}
	
	@Override
	public List<String> onTabComplete( CommandSender sender, Command command, String label, String[] args )
	{
		if(args.length == 1)
			return BungeeChat.getPlayerManager().matchNames(args[0]);
		
		return null;
	}

	@Override
	public boolean onCommand( final CommandSender sender, Command command, String label, String[] args )
	{
		switch (command.getName()) {
			case "mutelist":
				if (args.length != 0)
					return false;
				BungeeChat.getSyncManager().callSyncMethod("bchat:getMuteList", new IMethodCallback<List<String>>() {
					@Override
					public void onFinished(List<String> data) {
						if (data.isEmpty()) {
							sender.sendMessage(Component.text("There are no muted players.").color(NamedTextColor.GOLD));
						} else {
							TextComponent.Builder builder = Component.text().content("Muted players: ").color(NamedTextColor.GOLD);
							data.forEach(entry -> {
								String[] parts = entry.split(":");
								long time = Long.parseLong(parts[1]);
								time = time - System.currentTimeMillis();
								builder.append(Component.text(parts[0]+"("+Utilities.timeDiffToStringShort(time)+")")).append(Component.space());
							});
							sender.sendMessage(Component.text(builder.toString()).color(NamedTextColor.GRAY));
						}
					}
					@Override
					public void onError(String type, String message) {
						throw new RuntimeException(type + ":" + message);
					}
				});

				break;
			case "globalmute":
				if (args.length != 0 && args.length != 1)
					return false;

				long time;

				if (args.length == 1) {
					time = Utilities.parseDateDiff(args[0]);
					if (time <= 0) {
						sender.sendMessage(Component.text("Bad time format. Expected 5m, 2h or 30m2h").color(NamedTextColor.RED));
						return true;
					}

					time = System.currentTimeMillis() + time;

					BungeeChat.getSyncManager().callSyncMethod("bchat:setGMute", null, time);
				} else
					BungeeChat.getSyncManager().callSyncMethod("bchat:toggleGMute", null);

				break;
			default:
				if (args.length < 1)
					return false;

				CommandSender target = BungeeChat.getPlayerManager().getPlayer(args[0]);
				InetAddress address = null;

				if (!(target instanceof Player) && !(target instanceof RemotePlayer)) {
					if (command.getName().equals("ipmute") || command.getName().equals("ipunmute")) {
						try {
							address = InetAddress.getByName(args[0]);
						} catch (UnknownHostException e) {
							sender.sendMessage(Component.text("Unknown player or ip address").color(NamedTextColor.RED));
							return true;
						}
					} else {
						sender.sendMessage(Component.text("Unknown player").color(NamedTextColor.RED));
						return true;
					}
				}

				String name;
				if (target != null) {
					name = BungeeChat.getPlayerManager().getPlayerNickname(target);
					if(name == null || name.length() < 1) {
						if (target instanceof Player ) {
							name = ((Player) target).getDisplayName();
						}else if(target instanceof RemotePlayer){
							name = ((RemotePlayer)target).getDisplayName();
						}
					}
				} else{
					name= address.getHostAddress();
				}
				switch (command.getName()){

				case "mute":
					if (args.length != 2)
						return false;

					long mutetime = Utilities.parseDateDiff(args[1]);
					if (mutetime <= 0) {
						sender.sendMessage(Component.text("Bad time format. Expected 5m, 2h or 30m2h").color(NamedTextColor.RED));
						return true;
					}

					String timeString = Utilities.timeDiffToString(mutetime);

					mutetime = System.currentTimeMillis() + mutetime;
					BungeeChat.getPlayerManager().setPlayerMuteTime(target, mutetime);
					Component message = Component.text(name + " has been muted for " + timeString).color(NamedTextColor.AQUA);
					BungeeChat.mirrorChat(message, ChannelType.Broadcast.getName());
					Utilities.localBroadCast(message,null,target,object -> true);
					Utilities.getAudienceProvider().all().sendMessage(message);
					if(target != null) {
						target.sendMessage(Component.text("You have been muted for " + timeString).color(NamedTextColor.AQUA));
					}

					break;
				case "unmute":
					if (args.length != 1)
						return false;

					BungeeChat.getPlayerManager().setPlayerMuteTime(target, 0);
					sender.sendMessage(Component.text(name + " has been unmuted").color(NamedTextColor.AQUA));

					if(target !=null) {
						target.sendMessage(Component.text("You are no longer muted. You may talk again.").color(NamedTextColor.AQUA));
					}
					break;
				case "ipmute":
					if (args.length != 2)
						return false;

					long ipmutetime = Utilities.parseDateDiff(args[1]);
					if (ipmutetime <= 0) {
						sender.sendMessage(Component.text("Bad time format. Expected 5m, 2h or 30m2h").color(NamedTextColor.RED));
						return true;
					}

					if (target != null)
						BungeeChat.getSyncManager().callSyncMethod("bchat:setMuteIP", null,
								PlayerManager.getUniqueId(target), ipmutetime);
					else
						BungeeChat.getSyncManager().callSyncMethod("bchat:setMuteIP", null,
								address.getHostAddress(), ipmutetime);
					break;
				case "ipunmute":
					if (args.length != 1)
						return false;

					if (target != null)
						BungeeChat.getSyncManager().callSyncMethod("bchat:setMuteIP", null,
								PlayerManager.getUniqueId(target), 0L);
					else
						BungeeChat.getSyncManager().callSyncMethod("bchat:setMuteIP", null,
								address.getHostAddress(), 0L);
					sender.sendMessage(Component.text( name + " has been unmuted").color(NamedTextColor.AQUA));
					break;
				default:
					return false;
			}
			return true;
		}
		return true;
	}
}
