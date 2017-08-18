package au.com.addstar.bc.commands;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.ChatChannelManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created for the AddstarMC
 * Created by Narimm on 9/08/2017.
 */
public class RolePlaySubscribeCommand implements CommandExecutor {
    private static BungeeChat instance;
    public RolePlaySubscribeCommand(BungeeChat plugin) {
        instance = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
            if (args.length == 0) {
                commandSender.sendMessage("Usage: /rpsubscribe <channel> <time> <prefix>");
                return false;
            } else {
                String channel = args[0];
                if (commandSender.hasPermission("bungeechat.subscribe." + channel)) {
                    Integer ticks = 20 * 60 * 60;
                    if (args.length == 2) {
                        try {
                            ticks = Integer.parseInt(args[1]) * 20;
                        } catch (NumberFormatException e) {
                            commandSender.sendMessage(args[1] + " must be a integer.");
                        }
                    }
                    if (args.length > 2) {
                        String newPrefix = args[2];
                        BungeeChat.getPlayerManager().setPlayerRPPrefix(commandSender, newPrefix);
                        commandSender.sendMessage("RP Prefix set: " + newPrefix);
                    }
                    ChatChannelManager manager = instance.getChatChannelsManager();
                    String prefix = BungeeChat.getPlayerManager().getPlayerSettings(commandSender).rolePlayPrefix;
                    if (manager.hasChannel(channel)) {
                        String perm = manager.getChannelSpeakPerm(channel);
                        if (perm == null) {
                            instance.getLogger().warning("The speak permission for " + channel + " is null");
                            commandSender.sendMessage("Error with Channel permission please contact admin");
                            return false;
                        }
                        if (manager.isSubscribable(channel) && !commandSender.hasPermission(perm)) {
                            commandSender.addAttachment(instance, perm, true, ticks);
                            if (commandSender.hasPermission(perm)) {
                                commandSender.sendMessage("You have subcribed to " + channel + " for the next " +
                                        ticks / 20 + "seconds");
                                if (prefix != null) {
                                    commandSender.sendMessage("Your roleplay prefix is: " + prefix);
                                }
                            }
                        } else {
                            if (!manager.isSubscribable(channel)) {
                                commandSender.sendMessage("That channel is either not available or not subscribable.");
                                return false;
                            }
                            if (commandSender.hasPermission(perm)) {
                                commandSender.sendMessage("You are alredy subcribed to " + channel);
                                if (prefix != null) {
                                    commandSender.sendMessage("Your roleplay prefix is " + prefix);
                                }
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
