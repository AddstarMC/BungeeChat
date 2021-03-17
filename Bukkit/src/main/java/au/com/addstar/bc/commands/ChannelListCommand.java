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
import au.com.addstar.bc.ChatChannelManager;
import au.com.addstar.bc.sync.IMethodCallback;
import au.com.addstar.bc.utils.Utilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.units.qual.C;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Created for the Ark: Survival Evolved.
 * Created by Narimm on 17/08/2017.
 */
public class ChannelListCommand implements CommandExecutor {


    private static BungeeChat instance;
    public ChannelListCommand(BungeeChat plugin) {
        instance = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender commandSender, final @NotNull Command command, @NotNull String s, String[] args) {
        if(commandSender.hasPermission("bungeechat.chatlist")) {
            boolean subnf = true;
            if(args.length > 0 && args[0].equals("all") && commandSender.hasPermission("bungeechat.chatlist.all")){
                subnf = false;
            }
            final boolean sub = subnf;
            BungeeChat.getSyncManager().callSyncMethod("bchat:getSubscribed",new Subscribed(commandSender,sub));
                       return true;
        }else{
            Utilities.getAudienceProvider().sender(commandSender).sendMessage(Component.text("No Permission").color(NamedTextColor.RED));
        }
        return false;
    }

    private static class Subscribed implements IMethodCallback<HashMap<String,String>> {

        private final CommandSender sender;
        private final boolean sub;

        public Subscribed(CommandSender sender, boolean sub) {
            this.sender = sender;
            this.sub = sub;
        }

        public void onFinished(HashMap<String, String> data) {
            HashMap<String, Integer> result = new HashMap<>();
            ChatChannelManager manager = instance.getChatChannelsManager();
            List<String> existChannels = manager.getChannelNames(sub);
            for (String c: existChannels){
                result.put(c,0);
            }
            for(Map.Entry<String, String> entry: data.entrySet()){
                if(result.containsKey(entry.getValue())){
                    Integer current = result.get(entry.getValue());
                    current++;
                    result.put(entry.getValue(),current);
                }
            }
            TextComponent.Builder builder = Component.text().color(NamedTextColor.WHITE).append(createHeader(sub));
            builder.append(Component.text(
                    StringUtils.center("-Channel Name-:-Total Subscribed-",31)).color(NamedTextColor.GOLD)).append(TextComponent.newline());
            for(Map.Entry<String, Integer> res: result.entrySet()){
                builder.append(Component.text(StringUtils.rightPad(res.getKey(),15))
                        .append(Component.text(" : ").color(NamedTextColor.GOLD))
                        .append(Component.text(StringUtils.leftPad(res.getValue().toString(),15))))
                .append(Component.newline());
            }
            builder.append(Component.text(StringUtils.center("-----------------------",31)).color(NamedTextColor.GOLD));
            Utilities.getAudienceProvider().sender(sender).sendMessage(builder.build());
        }

        @Override
        public void onError(String type, String error) {
            ChatChannelManager manager = instance.getChatChannelsManager();
            Utilities.getAudienceProvider().sender(sender).sendMessage(Component.text("Error: " + type +" : " + error));
            TextComponent.Builder builder = TextComponent.builder().append(createHeader(sub));
            manager.getChannelNames(sub).forEach(c -> builder.append(Component.text(c)).append(TextComponent.newline()));
            builder.append(Component.text(StringUtils.center("-----------------------",31)).color(NamedTextColor.GOLD));
            Utilities.getAudienceProvider().sender(sender).sendMessage(builder.build());
        }

        private Component createHeader(boolean sub){
            TextComponent.Builder message = Component.text();
            if(!sub){
                message.append(Component.text(StringUtils.center("***List of Channels**",31))
                        .color(NamedTextColor.GOLD)).append(TextComponent.newline());
            }else {
                message.append(Component.text(StringUtils.center("***List of Available Channels**",31))
                        .color(NamedTextColor.GOLD)).append(TextComponent.newline());
            }
            message.append(Component.text(StringUtils.center("-----------------------",31)).color(NamedTextColor.GOLD)).append(TextComponent.newline());
            return message.build();
        }


    }
}
