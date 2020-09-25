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

package au.com.addstar.bc;

/*-
 * #%L
 * BungeeChat-Proxy
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

import au.com.addstar.bc.config.ChatChannel;
import au.com.addstar.bc.util.Utilities;
import net.kyori.adventure.text.TextComponent;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Map;

public class ManagementCommand extends Command
{
    private BungeeChat mPlugin;
    
    public ManagementCommand(BungeeChat plugin)
    {
        super("bungeechat", "bungeechat.command.manage", "bchat");
        mPlugin = plugin;
    }

    @Override
    public void execute( CommandSender sender, String[] args )
    {
        if(args.length < 1)
        {
            Utilities.audience.audience(sender).sendMessage(TextComponent.of("Usage: /bungeechat reload"));
            Utilities.audience.audience(sender).sendMessage(TextComponent.of("Usage: /bungeechat listchannel"));
            Utilities.audience.audience(sender).sendMessage(TextComponent.of("Usage: /bungeechat playerproperties <player>"));
            return;
        }

        switch(args[0].toLowerCase()){
            case "reload":

            if(mPlugin.loadConfig())
            {
                mPlugin.applySyncConfig();
                mPlugin.getSyncManager().sendConfig("bungeechat");
                Utilities.audience.audience(sender).sendMessage(TextComponent.of("BungeeChat config reloaded"));
            }
            else
            {
                Utilities.audience.audience(sender).sendMessage(TextComponent.of("An error occured while loading the BungeeChat config! Check the Proxy console"));
            }
            break;
            case "listchannel":
                Utilities.audience.audience(sender).sendMessage(TextComponent.of("List of Channels"));
                Utilities.audience.audience(sender).sendMessage(TextComponent.of("<Name> : <ListenPermission> : <SpeakPermission> : <command> "));

                for(Map.Entry<String, ChatChannel> entry: mPlugin.getChannels().entrySet()) {
                    Utilities.audience.audience(sender).sendMessage(TextComponent.of(entry.getKey() + " : "+ entry.getValue().listenPermission +
                            " : "+ entry.getValue().permission +" : "+entry.getValue().command));

                }
                Utilities.audience.audience(sender).sendMessage(TextComponent.of("------------------"));
            break;
            case "playerproperties":
                if(args.length < 2){
                    ProxiedPlayer target = BungeeChat.instance.getProxy().getPlayer(args[1]);
                    if(target ==null){
                        Utilities.audience.audience(sender).sendMessage(TextComponent.of("Player not found: "+ args[1]));
                        return;
                    }
                    Map<String, Object> result =
                            BungeeChat.instance.getSyncManager().getProperties(target);
                    if(result!=null) {
                        Utilities.audience.audience(sender).sendMessage(TextComponent.of("List of Properties for " + target.getDisplayName()+"("+target.getName()+")" ));
                        Utilities.audience.audience(sender).sendMessage(TextComponent.of("------------------"));
                        for (Map.Entry<String, Object> entry : result.entrySet()) {
                            Utilities.audience.audience(sender).sendMessage(TextComponent.of(entry.getKey()+" : " + entry.getValue().toString() ));
                        }
                        Utilities.audience.audience(sender).sendMessage(TextComponent.of("------------------"));
                        return;
                    }
                }
                break;
            default:
                Utilities.audience.audience(sender).sendMessage(TextComponent.of("Usage: /bungeechat " +
                        "reload|listchannel|playerproperties <name>"));
                break;
        }
        }
    }
