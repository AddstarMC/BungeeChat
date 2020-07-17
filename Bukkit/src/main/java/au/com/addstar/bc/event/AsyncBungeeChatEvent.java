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

package au.com.addstar.bc.event;

import au.com.addstar.bc.objects.ChatChannel;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 12/09/2020.
 */
public class AsyncBungeeChatEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Set<CommandSender> recipients;
    private boolean cancelled = false;
    private CommandSender chatSender;
    private Component message;
    private ChatChannel chatChannel;
    private Component format;
    public AsyncBungeeChatEvent(CommandSender chatSender, Set<CommandSender> recipients, Component format, Component message, ChatChannel channel) {
        super(true);
        this.chatSender = chatSender;
        this.recipients = recipients;
        this.message = message;
        this.chatChannel = channel;
        this.format = format;

    }

    public static HandlerList getHanderlist() {
        return handlers;
    }

    public CommandSender getChatSender() {
        return chatSender;
    }

    public void setChatSender(CommandSender chatSender) {
        this.chatSender = chatSender;
    }

    public Set<CommandSender> getRecipients() {
        return recipients;
    }

    public Component getMessage() {
        return message;
    }

    public void setMessage(Component message) {
        this.message = message;
    }

    public ChatChannel getChatChannel() {
        return chatChannel;
    }

    public void setChatChannel(ChatChannel chatChannel) {
        this.chatChannel = chatChannel;
    }

    public Component getFormat() {
        return format;
    }

    public void setFormat(Component format) {
        this.format = format;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
