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

import au.com.addstar.bc.commands.Debugger;
import au.com.addstar.bc.objects.ChannelType;
import au.com.addstar.bc.utils.Utilities;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import au.com.addstar.bc.event.ChatChannelEvent;
import au.com.addstar.bc.sync.IPacketHandler;
import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.packet.MirrorPacket;
import au.com.addstar.bc.sync.packet.SendPacket;
import au.com.addstar.bc.sync.packet.UpdateNamePacket;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedReader;


public class PacketHandler implements IPacketHandler
{

	@Override
	public void handle( Packet packet )
	{
		if(packet instanceof MirrorPacket)
			handleMirror((MirrorPacket)packet);
		else if(packet instanceof SendPacket)
			handleSend((SendPacket)packet);
		else if(packet instanceof UpdateNamePacket)
			handleUpdateName((UpdateNamePacket)packet);
	}
	
	private void handleMirror(MirrorPacket packet)
	{
		ChannelType type = ChannelType.from(packet.getChannel());
		Bukkit.getPluginManager().callEvent(new ChatChannelEvent(packet.getChannel(), type,packet.getMessage()));
	}
	
	private void handleSend(SendPacket packet)
	{
			Debugger.log("Sending message to %s: '%s'", packet.getUUID(), packet.getMessage());
		 	Player player = Bukkit.getPlayer(packet.getUUID());
		 	if(player != null) {
				player.sendMessage(packet.getMessage());
			}
	}
	
	private void handleUpdateName(UpdateNamePacket packet)
	{
		CommandSender player = BungeeChat.getPlayerManager().getPlayer(packet.getID());
		if (!(player instanceof Player))
			return;
		Component oldNick = BungeeChat.getPlayerManager().getPlayerNickname(player);
		BungeeChat.getPlayerManager().setPlayerNickname0(player, packet.getName());
		Debugger.log("Received nickname %s to '%s'", oldNick, packet.getName());
	}
	
	
}
