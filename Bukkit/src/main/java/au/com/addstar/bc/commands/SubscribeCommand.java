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

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.objects.ChatChannel;
import au.com.addstar.bc.utils.Utilities;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created for the AddstarMC
 * Created by Narimm on 9/08/2017.
 */
public class SubscribeCommand implements CommandExecutor {
    private static BungeeChat instance;
    public SubscribeCommand(BungeeChat plugin) {
        instance = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("Cannot Execute from Console: " + command.getName());
            return true;
        }
        Player player = (Player) commandSender;
        if (args.length == 0) {
            Utilities.getAudienceProvider().player(player).sendMessage(Component.text("Usage: ")
                        .color(NamedTextColor.DARK_AQUA).append(Component.text("/chat <channel>").color(NamedTextColor.GOLD)));
            return false;
        } else {
            String channelName = args[0];
            if (channelName.equalsIgnoreCase("global")) {
                String subscribed = BungeeChat.getPlayerManager().getDefaultChatChannel(player);
                if (!subscribed.isEmpty() && instance.getChatChannelsManager().hasChannel(subscribed)) {
                    String perm = instance.getChatChannelsManager().getChannelSpeakPerm(subscribed);
                    BungeeChat.getPlayerManager().unsubscribeAll(player, true);
                    if (!player.hasPermission(perm)) {
                        Utilities.getAudienceProvider().player(player).sendMessage(Component.text("Unsubscribed from ")
                              .color(NamedTextColor.DARK_AQUA).append(Component.text(subscribed).color(NamedTextColor.GREEN)));
                    } else {
                        Utilities.getAudienceProvider().player(player).sendMessage(Component.text("Could not unsubscribe from ")
                              .color(NamedTextColor.DARK_AQUA).append(Component.text(subscribed).color(NamedTextColor.GREEN)));
                    }
                } else {
                    BungeeChat.getPlayerManager().unsubscribeAll(player, true);
                    Utilities.getAudienceProvider().player(player).sendMessage(Component.text("You are not subscribed to any channels.").color(NamedTextColor.DARK_AQUA));
                }
                return true;
            }
            if (commandSender.hasPermission("bungeechat.subscribe." + channelName)) {
                Component prefix = BungeeChat.getPlayerManager().getPlayerSettings(player).chatName;
                final ChatChannel chatChannel = instance.getChatChannelsManager().getChatChannel(channelName);
                if (chatChannel != null) {
                    String perm = chatChannel.permission;
                    if (perm == null) {
                        instance.getLogger().warning("The speak permission for " + chatChannel.name + " is null");
                        Utilities.getAudienceProvider().player(player).sendMessage(Component.text("Error with Channel permission please contact admin")
                              .color(NamedTextColor.RED));
                        return false;
                    }
                    BungeeChat.getPlayerManager().unsubscribeAll(player, true);
                    if (chatChannel.subscribe && !player.hasPermission(perm)) {
                        BungeeChat.permissionManager.playerAdd(null, player, perm);
                        player.recalculatePermissions();
                        if (player.hasPermission(perm)) {
                            BungeeChat.getPlayerManager().setDefaultChannel(player, chatChannel.name);
                            TextComponent.Builder builder = Component.text()
                                  .content("You have subscribed to ").color(NamedTextColor.DARK_AQUA)
                                  .append(Component.text(chatChannel.name).color(NamedTextColor.GREEN))
                                  .append(Component.newline())
                                  .append(Component.text("Your chat will default to this channel."))
                                  .append(Component.newline())
                                  .append(Component.text("Use ")).append(Component.text("!").color(NamedTextColor.GOLD))
                                  .append(Component.text(" before your message to send to public chat"))
                                  .append(Component.newline())
                                  .append(Component.text("Use ")).append(Component.text("/chat global").color(NamedTextColor.GOLD))
                                  .append(Component.text(" to leave ")).append(Component.text(chatChannel.name).color(NamedTextColor.GREEN))
                                  .append(Component.text(" chat"))
                                  .append(Component.newline());
                            if (instance.getChatChannelsManager().isRolePlay(channelName)) {
                                if (prefix != null && prefix != Component.empty()) {
                                    builder.append(Component.text("Your roleplay prefix is "))
                                          .append(prefix).append(Component.newline());
                                }
                                builder.append(Component.text("Set your roleplay name using")).append(Component.text("/chatname <RolePlayName>").color(NamedTextColor.GOLD));
                            }
                            Utilities.getAudienceProvider().player(player).sendMessage(builder.build());

                        }
                    } else {
                        if (!instance.getChatChannelsManager().isSubscribable(channelName)) {
                            Utilities.getAudienceProvider().player(player).sendMessage(Component.text("That channel is either not available or you do not have permission.").color(NamedTextColor.RED));
                            return false;
                        }
                        if (commandSender.hasPermission(perm)) {
                            Utilities.getAudienceProvider().player(player).sendMessage(
                                    Component.text().content("You are already subscribed to ").color(NamedTextColor.DARK_AQUA)
                                  .append(Component.text(channelName).color(NamedTextColor.GREEN))
                                  .append(Component.text(" with perm "))
                                  .append(Component.text(perm).color(NamedTextColor.GREEN)).build());
                            BungeeChat.getPlayerManager().setDefaultChannel(player, channelName);
                            return true;
                        }
                    }
                    return true;
                } else {
                    Utilities.getAudienceProvider().player(player).sendMessage(
                          Component.text().content("Channel ").color(NamedTextColor.DARK_AQUA)
                                .append(Component.text(channelName).color(NamedTextColor.RED))
                                .append(Component.text(" does not exist."))
                                .build());
                    return false;
                }
            } else {
                Utilities.getAudienceProvider().player(player).sendMessage(Component.text("No Permission for that command.").color(NamedTextColor.RED));
                return false;
            }
        }
    }
}
