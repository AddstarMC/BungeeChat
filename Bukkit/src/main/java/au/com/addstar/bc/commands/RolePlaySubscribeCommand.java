package au.com.addstar.bc.commands;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.ChatChannelManager;;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

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
        if(commandSender.hasPermission("bungeechat.subscribe")){
            String channel = args[0];
            Integer ticks = 20*60*60;
            try {
                ticks = Integer.parseInt(args[1])*20;
            } catch (NumberFormatException e){
                commandSender.sendMessage(args[1]+ " must be a integer.");
            }
            if(args.length > 2){
                String newPrefix = args[2];
                BungeeChat.getPlayerManager().setPlayerRPPrefix(commandSender, newPrefix);
            }
            ChatChannelManager manager = instance.getChatChannelsManager();
            String prefix = BungeeChat.getPlayerManager().getPlayerSettings(commandSender).rolePlayPrefix;
            String perm = manager.getChannelSpeakPerm(channel);
            if(manager.isSubscribable(channel) && !commandSender.hasPermission(perm)) {
                commandSender.addAttachment(instance,perm,true, ticks);
                if(commandSender.hasPermission(perm)){
                    commandSender.sendMessage("You have subcribed to " + channel + " for the next " + ticks/20 + "seconds");
                    if(prefix != null){
                        commandSender.sendMessage("Your roleplay prefix is " + prefix);
                    }
                }
            }else {
                if (commandSender.hasPermission(perm)) {
                    commandSender.sendMessage("You are alredy subcribed to " + channel);
                    if(prefix != null){
                        commandSender.sendMessage("Your roleplay prefix is " + prefix);
                    }
                }
            }
        }else{
            commandSender.sendMessage("&c No Permission for that command");
        }
        return false;
    }
}
