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
        ChatChannelManager manager = instance.getChatChannelsManager();
        List<String> message = new ArrayList<>();
        message.add("***List of Roleplay Channels**");
        message.add("-----------------------");
        for(String string : manager.getChannels(true))
        {
            message.add(string);
        };
        message.add("-----------------------");
        return true;
    }
}
