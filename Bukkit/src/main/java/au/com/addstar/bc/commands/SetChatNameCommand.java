package au.com.addstar.bc.commands;

import au.com.addstar.bc.BungeeChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 19/08/2017.
 */
public class SetChatNameCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if(commandSender instanceof Player){
            commandSender.sendMessage("Must be used as a local player");
        }
        if (args.length == 0) {
            commandSender.sendMessage("Usage: /chatname <name>");
            return false;
        } else {
            String prefix = args[0];
            BungeeChat.getPlayerManager().setPlayerRPPrefix(commandSender, prefix);

            return true;
        }
    }
}
