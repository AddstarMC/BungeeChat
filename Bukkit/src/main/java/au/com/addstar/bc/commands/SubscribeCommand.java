package au.com.addstar.bc.commands;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.ChatChannelManager;
import au.com.addstar.bc.objects.ChatChannel;
import org.bukkit.ChatColor;
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
                String subscribed = BungeeChat.getPlayerManager().getDefaultChatChannel((Player)commandSender);
                if (instance.getChatChannelsManager().hasChannel(subscribed)) {
                    String perm = instance.getChatChannelsManager().getChannelSpeakPerm(subscribed);
                    if (perm == null) {
                        instance.getLogger().warning("The speak permission for " + channel + " is null");
                        player.sendMessage("Error with Channel permission please contact admin");
                        return false;
                    }
                    if (instance.getChatChannelsManager().isSubscribable(subscribed) && player.hasPermission(perm)) {
                        BungeeChat.permissionManager.playerRemove(player, perm);
                        player.recalculatePermissions();
                        if (!player.hasPermission(perm)) {
                            BungeeChat.getPlayerManager().setDefaultChannel(player, null);
                            commandSender.sendMessage("Unsubscribed from "+channel);
                        }else{
                            commandSender.sendMessage("Could not unsubscribe from " + channel);
                        }
                    }
                }else{
                    commandSender.sendMessage("You are not subscribed to any channels.");
                    for(ChatChannel c: BungeeChat.getInstance().getChatChannelsManager().getChannelObj().values()){
                        if(player.hasPermission(c.permission)){
                            BungeeChat.permissionManager.playerRemove(player, c.permission);
                            instance.getLogger().info("Removed Perm : " +player.getName() + " : " + c.permission );
                        }

                    }
                    player.recalculatePermissions();
                }
                return true;
            }
            if (commandSender.hasPermission("bungeechat.subscribe." + channel)) {
                String prefix = BungeeChat.getPlayerManager().getPlayerSettings(player).rolePlayPrefix;
                if (instance.getChatChannelsManager().hasChannel(channel)) {
                    String perm = instance.getChatChannelsManager().getChannelSpeakPerm(channel);
                    if (perm == null) {
                        instance.getLogger().warning("The speak permission for " + channel + " is null");
                        player.sendMessage("Error with Channel permission please contact admin");
                        return false;
                    }
                    if (instance.getChatChannelsManager().isSubscribable(channel) && !player.hasPermission(perm)) {
                        BungeeChat.permissionManager.playerAdd(player, perm);
                        player.recalculatePermissions();
                        if (player.hasPermission(perm)) {
                            BungeeChat.getPlayerManager().setDefaultChannel(player, channel);
                            player.sendMessage("You have subcribed to " + channel);
                            if (instance.getChatChannelsManager().isRolePlay(channel)) {
                                if (prefix != null) {
                                    commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                            "Your roleplay prefix is " + prefix));
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
