package au.com.addstar.bc.commands;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.ChatChannelManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Created for the Ark: Survival Evolved.
 * Created by Narimm on 17/08/2017.
 */
public class RPChannelListCommand implements CommandExecutor {


    private static BungeeChat instance;
    public RPChannelListCommand(BungeeChat plugin) {
        instance = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(commandSender.hasPermission("bungeechat.rplist")) {
            boolean sub = true;
            if(strings.length > 0 && strings[0].equals("all"))sub = false;
            ChatChannelManager manager = instance.getChatChannelsManager();
            List<String> message = new ArrayList<>();
            if(sub){
                message.add("***List of Channels**");
            }else {
                message.add("***List of Roleplay Channels**");
            }
            message.add("-----------------------");
            message.addAll(manager.getChannels(sub));
            message.add("-----------------------");
            String[] messages = new String[message.size()];
            message.toArray(messages);
            commandSender.sendMessage(messages);
            return true;
        }else{
            commandSender.sendMessage("No Permission");
        }
        return false;
    }
}
