package au.com.addstar.bc.commands;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.objects.ChatChannel;
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
            player.sendMessage("Usage: /chat <channel>");
            return false;
        } else {
            String channel = args[0];
            if (channel.toLowerCase().equals("global")) {
                String subscribed = BungeeChat.getPlayerManager().getDefaultChatChannel(player);
                if (subscribed !=null  && instance.getChatChannelsManager().hasChannel(subscribed)) {
                    String perm = instance.getChatChannelsManager().getChannelSpeakPerm(subscribed);
                    BungeeChat.getPlayerManager().unsubscribeAll(player);
                    if (!player.hasPermission(perm)) {
                        commandSender.sendMessage("Unsubscribed from "+ subscribed);
                    }else{
                        commandSender.sendMessage("Could not unsubscribe from " + subscribed);
                    }
                }else{
                    BungeeChat.getPlayerManager().unsubscribeAll(player);
                    commandSender.sendMessage("You are not subscribed to any channels.");
                }
                return true;
            }
            if (commandSender.hasPermission("bungeechat.subscribe." + channel)) {
                String prefix = BungeeChat.getPlayerManager().getPlayerSettings(player).chatName;
                final ChatChannel chatChannel = instance.getChatChannelsManager().getChatChannel(channel);
                if (chatChannel != null) {
                    String perm = chatChannel.permission;
                    if (perm == null) {
                        instance.getLogger().warning("The speak permission for " + channel + " is null");
                        player.sendMessage("Error with Channel permission please contact admin");
                        return false;
                    }
                    BungeeChat.getPlayerManager().unsubscribeAll(player);
                    if (chatChannel.subscribe && !player.hasPermission(perm)) {
                        BungeeChat.permissionManager.playerAdd(null,player, perm);
                        player.recalculatePermissions();
                        if (player.hasPermission(perm)) {
                            BungeeChat.getPlayerManager().setDefaultChannel(player, chatChannel.name);
                            player.sendMessage("You have subcribed to " + chatChannel.name);
                            player.sendMessage("Your chat will default to this channel.");
                            player.sendMessage("Use ! before your message to send to public chat");
                            if (instance.getChatChannelsManager().isRolePlay(channel)) {
                                if (prefix != null) {
                                    commandSender.sendMessage("Your roleplay prefix is " +prefix);
                                }
                            }
                        }
                    } else {
                        if (!instance.getChatChannelsManager().isSubscribable(channel)) {
                            commandSender.sendMessage("That channel is either not available or not subscribable.");
                            return false;
                        }
                        if (commandSender.hasPermission(perm)) {
                            commandSender.sendMessage("You are already subcribed to " + channel + "with perm " +perm);
                            BungeeChat.getPlayerManager().setDefaultChannel(player, channel);
                            return true;
                        }
                    }
                    return true;
                } else {
                    commandSender.sendMessage(channel + " does not exist.");
                    return false;
                }
            } else {
                commandSender.sendMessage("&c No Permission for that command");
                return false;
            }
        }
    }
}
