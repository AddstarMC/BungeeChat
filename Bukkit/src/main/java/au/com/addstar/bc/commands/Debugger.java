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
import au.com.addstar.bc.PlayerManager;
import au.com.addstar.bc.objects.RemotePlayer;
import au.com.addstar.bc.utils.Utilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class Debugger implements CommandExecutor
{
	private static boolean mDebugEnabled = false;
	private static boolean mPacketDebugEnabled = false;
	
	public static void setGeneralDebugState(boolean on)
	{
		mDebugEnabled = on;
	}
	
	public static void setPacketDebugState(boolean on)
	{
		mPacketDebugEnabled = on;
	}
	
	private static void log0(String category, String message)
	{
		if (category == null || category.isEmpty())
			BungeeChat.getInstance().getLogger().info(String.format("[Debug] %s", message));
		else
			BungeeChat.getInstance().getLogger().info(String.format("[Debug-%s] %s", category, message));
	}
	
	/**
	 * General logging
	 */
	public static void log(String message, Object... params)
	{
		if (mDebugEnabled)
			log0(null, String.format(message, params));
	}
	
	/**
	 * Packet logging
	 */
	public static void logp(String message, Object... params)
	{
		if (mPacketDebugEnabled)
			log0("Packet", String.format(message, params));
	}
	
	/**
	 * Logs if the specified command sender is invalid, eg. A local player when not online, or a remote player when online
	 */
	public static void logCorrect(CommandSender sender)
	{
		if (!mDebugEnabled)
			return;
		
		if (sender instanceof Player)
		{
			Player player = (Player)sender;
			Player oplayer = Bukkit.getPlayer(player.getUniqueId());
			logTrue(oplayer != null, "Player %s was marked as local but is remote", sender.getName());
			logTrue(oplayer != null && oplayer == player, "Player %s's instance in BC did not match the offical instance", sender.getName());
		}
		else if (sender instanceof RemotePlayer)
		{
			RemotePlayer player = (RemotePlayer)sender;
			Player oplayer = Bukkit.getPlayer(player.getUniqueId());
			logTrue(oplayer == null, "Player %s was marked as remote but is local", sender.getName());
		}
	}
	
	private static void logTrue(boolean expression, String message, Object... params)
	{
		if (!expression)
			log(message, params);
	}
	
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args )
	{
		if (args.length == 0) {
            Component c = TextComponent.of("General debug |")
                    .append(createOnOffButtons(mDebugEnabled,"bchat general"));
            Component cp = TextComponent.of("Packet debug |")
                    .append(createOnOffButtons(mPacketDebugEnabled,"bchat packet"));
            c.append(TextComponent.newline()).append(cp);
            Utilities.getAudienceProvider().audience(sender).sendMessage(c.append(TextComponent.newline()).append(cp));
            return true;
        }

		if (args[0].equalsIgnoreCase("general"))
		{
			if (args.length == 2) {
                boolean on = Boolean.parseBoolean(args[1]);
                setGeneralDebugState(on);
            }
			Component c = TextComponent.of("General debug |")
                    .append(createOnOffButtons(mDebugEnabled,"bchat general"));
			Utilities.getAudienceProvider().audience(sender).sendMessage(c);
		}
		else if (args[0].equalsIgnoreCase("packet"))
		{
			if (args.length == 2) {
                boolean on = Boolean.parseBoolean(args[1]);
                setPacketDebugState(on);
            }
            Component c = TextComponent.of("Packet debug |")
                    .append(createOnOffButtons(mPacketDebugEnabled,"bchat packet"));
            Utilities.getAudienceProvider().audience(sender).sendMessage(c);
        }
		else if (args[0].equalsIgnoreCase("player"))
		{
			if (args.length != 2)
				return false;
			
			CommandSender player = BungeeChat.getPlayerManager().getPlayerExact(args[1]);
            UUID uuid = PlayerManager.getUniqueId(player);
			if(uuid != null) {
                Player bukkitPlayer = Bukkit.getPlayer(uuid);
                Utilities.getAudienceProvider().audience(sender)
                        .sendMessage(
                                TextComponent.of(String.format("State %s: %s", args[1], buildPlayerDebug(player, bukkitPlayer))));
            } else {
			    return false;
            }

		}
		else if (args[0].equalsIgnoreCase("allplayers"))
		{
            Utilities.getAudienceProvider().audience(sender)
                    .sendMessage(
                            TextComponent.of("Total tracked: " + BungeeChat.getPlayerManager().getPlayers().size()
                                    + " Bukkit players: " + Bukkit.getOnlinePlayers().size()));
			// Check all tracked players
			for (CommandSender player : BungeeChat.getPlayerManager().getPlayers()) {
                UUID uuid = PlayerManager.getUniqueId(player);
                if (uuid != null) {
                    Utilities.getAudienceProvider().audience(sender)
                            .sendMessage(
                                    TextComponent.of(String.format(" %s: %s", player.getName(),
                                            buildPlayerDebug(player, Bukkit.getPlayer(uuid)))));
                }
            }
			
			// Look for any that are not tracked
			for (Player player : Bukkit.getOnlinePlayers())
			{
				CommandSender bplayer = BungeeChat.getPlayerManager().getPlayer(player.getUniqueId());
				if (bplayer == null)
                    Utilities.getAudienceProvider().audience(sender)
                            .sendMessage(
                                    TextComponent.of(String.format(" %s: %s", player.getName(), buildPlayerDebug(null, player))));
			}
		}
		else if (args[0].equalsIgnoreCase("resync"))
		{
            Utilities.getAudienceProvider().audience(sender)
                    .sendMessage(
                            TextComponent.of("Resynching BungeeChat"));
			BungeeChat.getInstance().requestUpdate();
		}
		else
			return false;
		
		return true;
	}
	private static  Component createOnOffButtons(final boolean status,String baseCommand){
	    String onText;
	    String offText;
	    if(status){
	         onText = " ON🗸|";
	         offText = " OFF |";
        } else {
             onText = " ON |";
             offText = " OFF🗸|";

        }
		Component compOn = TextComponent.of(onText)
				.style(getStyle(true,status))
				.hoverEvent(HoverEvent.showText(TextComponent.of("Click to turn on")))
				.clickEvent(ClickEvent.runCommand(baseCommand+" true"));
		Component compOff = TextComponent.of(offText).style(getStyle(false,!status))
				.hoverEvent(HoverEvent.showText(TextComponent.of("Click to turn OFF")))
				.clickEvent(ClickEvent.runCommand(baseCommand+" false"));
		return compOn.append(compOff);
	}

	private static Style getStyle(boolean onbutton, boolean iftrue){
		TextColor color;
		if(iftrue)
			if(onbutton) {
				color = NamedTextColor.GREEN;
			} else {
				color = NamedTextColor.RED;
			}
		else {
			color = NamedTextColor.DARK_GRAY;

		}
		return Style.builder().color(color).build();
	}

	private String buildPlayerDebug(CommandSender player, Player local)
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("BCPlayer: ");
		if (player instanceof RemotePlayer)
			builder.append("remote");
		else if(player instanceof Player)
			builder.append("local");
		else
			builder.append("no");
		
		builder.append(" Local: ");
		
		if (local != null)
			builder.append("yes");
		else
			builder.append("no");

		if (player instanceof Player && local != null)
		{
			if (player != local)
				builder.append(" *ply mismatch*");
		}
		
		
		return builder.toString();
	}
}
