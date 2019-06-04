package au.com.addstar.bc.commands;

/*-
 * #%L
 * BungeeChat-Bukkit
 * %%
 * Copyright (C) 2015 - 2019 AddstarMC
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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
	
	public static void logTrue(boolean expression, String message, Object... params)
	{
		if (!expression)
			log(message, params);
	}
	
	@Override
	public boolean onCommand( CommandSender sender, Command cmd, String label, String[] args )
	{
		if (args.length == 0)
			return false;
		
		if (args[0].equalsIgnoreCase("general"))
		{
			if (args.length != 2)
				return false;
			
			boolean on = Boolean.valueOf(args[1]);
			setGeneralDebugState(on);
			
			sender.sendMessage(ChatColor.GOLD + "General debug is now " + (on ? "on" : "off"));
		}
		else if (args[0].equalsIgnoreCase("packet"))
		{
			if (args.length != 2)
				return false;
			
			boolean on = Boolean.valueOf(args[1]);
			setPacketDebugState(on);
			
			sender.sendMessage(ChatColor.GOLD + "Packet debug is now " + (on ? "on" : "off"));
		}
		else if (args[0].equalsIgnoreCase("player"))
		{
			if (args.length != 2)
				return false;
			
			CommandSender player = BungeeChat.getPlayerManager().getPlayerExact(args[1]);
			Player bplayer = Bukkit.getPlayer(PlayerManager.getUniqueId(player));
			
			sender.sendMessage(String.format("State %s: %s", args[1], buildPlayerDebug(player, bplayer)));
		}
		else if (args[0].equalsIgnoreCase("allplayers"))
		{
			sender.sendMessage("Total tracked: " + BungeeChat.getPlayerManager().getPlayers().size() + " Bukkit players: " + Bukkit.getOnlinePlayers().size());
			// Check all tracked players
			for (CommandSender player : BungeeChat.getPlayerManager().getPlayers())
				sender.sendMessage(String.format(" %s: %s", player.getName(), buildPlayerDebug(player, Bukkit.getPlayer(PlayerManager.getUniqueId(player)))));
			
			// Look for any that are not tracked
			for (Player player : Bukkit.getOnlinePlayers())
			{
				CommandSender bplayer = BungeeChat.getPlayerManager().getPlayer(player.getUniqueId());
				if (bplayer == null)
					sender.sendMessage(String.format(" %s: %s", player.getName(), buildPlayerDebug(null, player)));
			}
		}
		else if (args[0].equalsIgnoreCase("resync"))
		{
			sender.sendMessage("Resynching BungeeChat");
			BungeeChat.getInstance().requestUpdate();
		}
		else
			return false;
		
		return true;
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
