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
import au.com.addstar.bc.sync.IMethodCallback;
import au.com.addstar.bc.utils.Utilities;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang.StringUtils;
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
        if (commandSender.hasPermission("bungeechat.chatwho")) {
            if (commandSender instanceof Player) {
                BungeeChat.getSyncManager().callSyncMethod("bchat:getSubscribed",
                      new IMethodCallback<HashMap<String, String>>() {

                          @Override
                          public void onFinished(HashMap<String, String> data) {
                              if (data.size() == 0) {
                                  BungeeChat.getInstance().getLogger().info("No Data returned");
                                  return;
                              }
                              List<Component> result = new ArrayList<>();
                              String subscribed = BungeeChat.getPlayerManager().getDefaultChatChannel((Player) commandSender);
                              for (Map.Entry<String, String> entry : data.entrySet()) {
                                  if (entry.getValue().equals(subscribed)) {
                                      UUID uid = UUID.fromString(entry.getKey());
                                      CommandSender target = BungeeChat.getPlayerManager().getPlayer(uid);
                                      TextComponent.Builder name = Component.text();
                                      if (target != null) {
                                          Component chatName = BungeeChat.getPlayerManager().getPlayerSettings(target).chatName;
                                          if (target instanceof Player) {
                                              if (((Player) commandSender).canSee((Player) target)) {
                                                  name.append(((Player) target).displayName());
                                              }
                                          } else {
                                              Component nickname = BungeeChat.getPlayerManager().getPlayerNickname(target);
                                              if (!Component.empty().equals(nickname)) {
                                                  name.append(Component.text("[")).append(nickname).append(Component.text("] "));
                                              }
                                          }
                                          name.append(chatName);
                                      }
                                      if (name != Component.empty()) {
                                          result.add(name.build());
                                      }
                                  }
                              }
                              TextComponent.Builder builder = Component.text()
                                    .content(StringUtils.center("Players Currently Subscribed to " + subscribed, 40, '*')).color(NamedTextColor.GOLD).append(Component.newline());
                              for (Component s : result) {
                                  builder.append(s).append(Component.newline());
                              }
                              builder.append(Component.text(StringUtils.center("-----", 40, '-')));
                              commandSender.sendMessage(builder.build());
                          }

                          @Override
                          public void onError(String type, String message) {
                              commandSender.sendMessage(Component.text("Error: " + type + " : " + message).color(NamedTextColor.RED));
                          }
                      });
            }
            return true;
        } else {
            commandSender.sendMessage(Component.text("No Permission"));
        }
        return false;
    }
}
