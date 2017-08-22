package au.com.addstar.bc.commands;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.PermissionSetting;
import au.com.addstar.bc.objects.Formatter;
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
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {           if(commandSender instanceof Player){
            if (args.length == 0) {
                commandSender.sendMessage("Usage: /chatname <name>");
                return false;
            } else {
                Player player = (Player) commandSender;
                String prefix = args[0];
                String colorprefix = BungeeChat.colorize(prefix,commandSender);
                BungeeChat.getPlayerManager().setPlayerChatName(player, colorprefix);
                return true;
            }
        }else{
            commandSender.sendMessage("Must be used as a local player");
        }
        return false;
    }
}
