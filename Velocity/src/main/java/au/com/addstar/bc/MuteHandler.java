/*
 * BungeeChat
 *
 * Copyright (c) 2015 - 2021.
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


import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import au.com.addstar.bc.config.Config;
import au.com.addstar.bc.util.Utilities;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class MuteHandler {

    private long mGMuteTime;
    private final HashMap<InetAddress, Long> mIPMuteTime;
    private Set<String> mMutedCommands;
    private final PlayerSettingsManager mPlayerManager;

    public MuteHandler(ProxyChat plugin) {
        mPlayerManager = plugin.getManager();
        mIPMuteTime = new HashMap<>();
        mMutedCommands = Collections.emptySet();
        mGMuteTime = 0;
        ScheduledTask task = plugin.getServer().getScheduler().buildTask(plugin, new UnmuteTimer(plugin.getServer()))
                .delay(5, TimeUnit.SECONDS).repeat(5, TimeUnit.SECONDS).schedule();
        plugin.getServer().getEventManager().register(plugin, this);
    }

    public void updateSettings(Config config) {
        mMutedCommands = new HashSet<>(config.mutedCommands);
    }

    public void toggleGMute(ProxyServer server) {
        if (mGMuteTime == 0)
            mGMuteTime = Long.MAX_VALUE;
        else
            mGMuteTime = 0;

        if (mGMuteTime == 0)
            server.sendMessage(Component.text("The global mute has ended").color(NamedTextColor.AQUA));
        else
            server.sendMessage(
                    Component.text("A ").color(NamedTextColor.AQUA)
                            .append(Component.text("global")
                                    .color(NamedTextColor.RED))
                            .append(Component.text(" mute has been started")));
    }

    public void setGMute(ProxyServer server, long endTime) {
        if (mGMuteTime == endTime)
            return;

        if (endTime == 0)
            server.sendMessage(Component.text("The global mute has ended").color(NamedTextColor.AQUA));
        else {
            Component message = Component.text("A ").color(NamedTextColor.AQUA)
                    .append(Component.text("global").color(NamedTextColor.RED))
                    .append(Component.text(" mute has been started"));

            if (endTime != Long.MAX_VALUE) {
                long timeLeft = Math.round((endTime - System.currentTimeMillis()) / 1000) * 1000L;
                message = message.append(Component.text(" for " + Utilities.timeDiffToString(timeLeft)));
            }
            server.sendMessage(message);
        }

        mGMuteTime = endTime;
    }

    public boolean isGMuted() {
        return mGMuteTime != 0;
    }

    public void setIPMute(ProxyServer server, InetAddress address, long endTime) {
        Component message;
        if (endTime == 0) {
            if (mIPMuteTime.remove(address) == null)
                return;

            message = Component.text("You are no longer muted. You may talk again.");
        } else {
            long timeLeft = Math.round((endTime - System.currentTimeMillis()) / 1000F) * 1000L;
            message = Component.text("You have been muted for " + Utilities.timeDiffToString(timeLeft));

            mIPMuteTime.put(address, endTime);
        }

        // Send message to affected players
        for (Player p : server.getAllPlayers()) {
            if (p.getRemoteAddress().getAddress().equals(address)) {
                p.sendMessage(message);
                break;
            }
        }
    }

    public boolean isMuted(Player player) {
        PlayerSettings settings = mPlayerManager.getSettings(player);
        InetAddress address = player.getRemoteAddress().getAddress();

        if (mGMuteTime > System.currentTimeMillis() && !player.hasPermission("bungeechat.globalmute.exempt"))
            return true;
        else if (settings.muteTime > System.currentTimeMillis())
            return true;
        else
            return mIPMuteTime.containsKey(address) && mIPMuteTime.get(address) > System.currentTimeMillis();
    }

    public boolean isMutedCommand(String commandString) {
        String command = commandString.split(" ")[0];
        return mMutedCommands.contains(command);
    }

    @Subscribe
    public void onPlayerCommand(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player)) {
            return;
        }
        if (!isMutedCommand(event.getCommand())) {
            return;
        }
        Player player = (Player) event.getCommandSource();
        if (!isMuted(player))
            return;
        event.setResult(CommandExecuteEvent.CommandResult.denied());
        boolean global = isGMuted();
        if (global)
            player.sendMessage(Component.text("Everyone is muted. You may not use that command.").color(NamedTextColor.AQUA));
        else
            player.sendMessage(Component.text("You are muted. You may not use that command.").color(NamedTextColor.AQUA));

    }


    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!isMuted(player))
            return;
        event.setResult(PlayerChatEvent.ChatResult.denied());
        boolean global = isGMuted();
        if (global)
            player.sendMessage(Component.text("Everyone is muted. You may not talk.").color(NamedTextColor.AQUA));
        else
            player.sendMessage(Component.text("You are muted. You may not talk.").color(NamedTextColor.AQUA));
    }

    private class UnmuteTimer implements Runnable {

        private final ProxyServer server;

        private UnmuteTimer(ProxyServer server) {
            this.server = server;
        }

        @Override
        public void run() {
            if (mGMuteTime > 0 && System.currentTimeMillis() >= mGMuteTime) {
                server.sendMessage(Component.text("The global mute has ended"));
                mGMuteTime = 0;
                // TODO: Remove, this is only for legacy
                //server.getPacketManager().broadcast(new GlobalMutePacket(0));
            }

            Iterator<Entry<InetAddress, Long>> it = mIPMuteTime.entrySet().iterator();
            while (it.hasNext()) {
                Entry<InetAddress, Long> entry = it.next();

                if (System.currentTimeMillis() >= entry.getValue()) {
                    it.remove();
                    Component message = Component.text("You are no longer muted. You may talk again.");
                    for (Player player : server.getAllPlayers()) {
                        if (player.getRemoteAddress().getAddress().equals(entry.getKey()))
                            player.sendMessage(message);
                    }
                }
            }

            for (Player player : server.getAllPlayers()) {
                PlayerSettings settings = mPlayerManager.getSettings(player);
                if (settings.muteTime > 0 && System.currentTimeMillis() >= settings.muteTime) {
                    settings.muteTime = 0;
                    mPlayerManager.updateSettings(player);
                    mPlayerManager.savePlayer(player);

                    player.sendMessage(Component.text("You are no longer muted. You may talk again."));
                }
            }
        }
    }
}

