package au.com.addstar.bc.commands;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.sync.IMethodCallback;
import au.com.addstar.bc.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Created for the Ark: Survival Evolved.
 * Created by Narimm on 23/08/2017.
 */
public class ListSubscribedCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(commandSender.hasPermission("bungeechat.chatwho")) {
            if(commandSender instanceof Player) {
                BungeeChat.getSyncManager().callSyncMethod("bchat:getSubscribed",
                        new IMethodCallback<HashMap<String, String>>() {

                            @Override
                            public void onFinished(HashMap<String, String> data) {
                                if(data.size() == 0){
                                    BungeeChat.getInstance().getLogger().info("No Data returned");
                                    return;
                                }
                                List<String> result = new ArrayList<>();
                                String subscribed = BungeeChat.getPlayerManager().getDefaultChatChannel((Player)commandSender);
                                for (Map.Entry<String, String> entry : data.entrySet()) {
                                    if (entry.getValue().equals(subscribed)) {
                                        UUID uid = UUID.fromString(entry.getKey());
                                        CommandSender target = BungeeChat.getPlayerManager().getPlayer(uid);
                                        StringBuilder name = new StringBuilder();
                                        if(target != null){
                                            if(target instanceof Player){
                                                if(((Player)commandSender).canSee((Player)target))
                                                    name.append(((Player)target).getDisplayName());
                                            }else {
                                                String chatName = BungeeChat.getPlayerManager().getPlayerNickname(target);
                                                if(chatName != null && !chatName.isEmpty())name.append(" [").append(chatName).append("] ");
                                        }
                                        }
                                        String finalName = name.toString();
                                        if(!(finalName.isEmpty()))result.add(finalName);
                                    }
                                }
                                List<String> message = new ArrayList<>();
                                message.add(Utilities.colorize("&6***Players Currently Subscribed to "+ subscribed +  " ***&f"));
                                message.addAll(result);
                                message.add(Utilities.colorize("&6-----------------------&f"));
                                String[] messages = new String[message.size()];
                                message.toArray(messages);
                                commandSender.sendMessage(messages);
                            }

                            @Override
                            public void onError(String type, String message) {
                                commandSender.sendMessage("Error: " + type + " : " + message);
                            }
                        });
            }
            return true;
        }else{
            commandSender.sendMessage("No Permission");
        }
        return false;
    }
}
