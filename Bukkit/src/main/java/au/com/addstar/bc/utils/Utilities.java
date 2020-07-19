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

package au.com.addstar.bc.utils;

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

import java.awt.*;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.com.addstar.bc.commands.Debugger;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;

public class Utilities
{
	public static final NoConsoleChecker NO_CONSOLE = new NoConsoleChecker();
	public static final SocialSpyChecker SOCIAL_SPY_ENABLED = new SocialSpyChecker();

	private static final Pattern HEX_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");

	public static void broadcast(String message, String permission, ValidChecker<CommandSender> checker)
	{
		Collection<? extends Permissible> targets;
		if(permission != null)
			targets = Bukkit.getPluginManager().getPermissionSubscriptions(permission);
		else
			targets = Bukkit.getOnlinePlayers();

		for(Permissible perm : targets)
		{
			if(perm instanceof CommandSender && (permission == null || perm.hasPermission(permission)) && (checker == null || checker.isValid((CommandSender)perm)))
				((CommandSender)perm).sendMessage(message);

		}
	}
	
	public static void broadcast(String message, String permission, CommandSender except, ValidChecker<CommandSender> checker)
	{
		Collection<? extends Permissible> targets;
		if(permission != null)
			targets = Bukkit.getPluginManager().getPermissionSubscriptions(permission);
		else
			targets = Bukkit.getOnlinePlayers();
		
		for(Permissible perm : targets)
		{
			if(perm == except)
				continue;
			
			if(perm instanceof CommandSender && (permission == null || perm.hasPermission(permission)) && (checker == null || checker.isValid((CommandSender)perm)))
				((CommandSender)perm).sendMessage(message);
		}
	}

	/**
	 * Converts color place-holders.
	 *
	 * @param text text to color
	 * @return A String
	 */
	public static String colorize(String text) {
		text = parseRGBColors(text);
		return ChatColor.translateAlternateColorCodes('&', text);
	}

	public static long parseDateDiff(String dateDiff)
	{
		if(dateDiff == null)
			return 0;
		
		Pattern dateDiffPattern = Pattern.compile("^\\s*(\\-|\\+)?\\s*(?:([0-9]+)y)?\\s*(?:([0-9]+)mo)?\\s*(?:([0-9]+)w)?\\s*(?:([0-9]+)d)?\\s*(?:([0-9]+)h)?\\s*(?:([0-9]+)m)?\\s*(?:([0-9]+)s)?\\s*$");
		dateDiff = dateDiff.toLowerCase();
		
		Matcher m = dateDiffPattern.matcher(dateDiff);
		
		if(m.matches())
		{
			int years,months,weeks,days,hours,minutes,seconds;
			boolean negative;
			
			if(m.group(1) != null)
				negative = (m.group(1).compareTo("-") == 0);
			else
				negative = false;

			if(m.group(2) != null)
				years = Integer.parseInt(m.group(2));
			else
				years = 0;
			
			if(m.group(3) != null)
				months = Integer.parseInt(m.group(3));
			else
				months = 0;
			
			if(m.group(4) != null)
				weeks = Integer.parseInt(m.group(4));
			else
				weeks = 0;
			
			if(m.group(5) != null)
				days = Integer.parseInt(m.group(5));
			else
				days = 0;
			
			if(m.group(6) != null)
				hours = Integer.parseInt(m.group(6));
			else
				hours = 0;
			
			if(m.group(7) != null)
				minutes = Integer.parseInt(m.group(7));
			else
				minutes = 0;
			
			if(m.group(8) != null)
				seconds = Integer.parseInt(m.group(8));
			else
				seconds = 0;
			
			// Now calculate the time
			long time = 0;
			time += seconds * 1000L;
			time += minutes * 60000L;
			time += hours * 3600000L;
			time += days * 72000000L;
			time += weeks * 504000000L;
			time += months * 2191500000L;
			time += years * 26298000000L;
			
			if(negative)
				time *= -1;
			
			return time;
		}
		
		return 0;
	}
	
	public static String timeDiffToString(long time)
	{
		StringBuilder builder = new StringBuilder();
		if(time >= 26298000000L)
		{
			int number = (int)(time / 26298000000L);
			time -= number * 26298000000L;
			
			builder.append(number);
			builder.append(" ");
			if(number == 1)
				builder.append("year");
			else
				builder.append("years");
		}
		
		if(time >= 2191500000L)
		{
			if(builder.length() != 0)
				builder.append(" ");
			
			int number = (int)(time / 2191500000L);
			time -= number * 2191500000L;
			
			builder.append(number);
			builder.append(" ");
			if(number == 1)
				builder.append("month");
			else
				builder.append("months");
		}
		
		if(time >= 504000000L)
		{
			if(builder.length() != 0)
				builder.append(" ");
			
			int number = (int)(time / 504000000L);
			time -= number * 504000000L;
			
			builder.append(number);
			builder.append(" ");
			if(number == 1)
				builder.append("week");
			else
				builder.append("weeks");
		}
		
		if(time >= 72000000L)
		{
			if(builder.length() != 0)
				builder.append(" ");
			
			int number = (int)(time / 72000000L);
			time -= number * 72000000L;
			
			builder.append(number);
			builder.append(" ");
			if(number == 1)
				builder.append("day");
			else
				builder.append("days");
		}
		
		if(time >= 3600000L)
		{
			if(builder.length() != 0)
				builder.append(" ");
			
			int number = (int)(time / 3600000L);
			time -= number * 3600000L;
			
			builder.append(number);
			builder.append(" ");
			if(number == 1)
				builder.append("hour");
			else
				builder.append("hours");
		}
		
		if(time >= 60000L)
		{
			if(builder.length() != 0)
				builder.append(" ");
			
			int number = (int)(time / 60000L);
			time -= number * 60000L;
			
			builder.append(number);
			builder.append(" ");
			if(number == 1)
				builder.append("minute");
			else
				builder.append("minutes");
		}
		
		if(time >= 1000L)
		{
			if(builder.length() != 0)
				builder.append(" ");
			
			int number = (int)(time / 1000L);
			
			builder.append(number);
			builder.append(" ");
			if(number == 1)
				builder.append("second");
			else
				builder.append("seconds");
		}
		
		return builder.toString();
	}
	
	public static String timeDiffToStringShort(long time)
	{
		StringBuilder builder = new StringBuilder();
		if(time >= 26298000000L)
		{
			int number = (int)(time / 26298000000L);
			time -= number * 26298000000L;
			
			builder.append(number);
			builder.append("y");
		}
		
		if(time >= 2191500000L)
		{
			if(builder.length() != 0)
				builder.append(" ");
			
			int number = (int)(time / 2191500000L);
			time -= number * 2191500000L;
			
			builder.append(number);
			builder.append("mo");
		}
		
		if(time >= 504000000L)
		{
			if(builder.length() != 0)
				builder.append(" ");
			
			int number = (int)(time / 504000000L);
			time -= number * 504000000L;
			
			builder.append(number);
			builder.append("w");
		}
		
		if(time >= 72000000L)
		{
			if(builder.length() != 0)
				builder.append(" ");
			
			int number = (int)(time / 72000000L);
			time -= number * 72000000L;
			
			builder.append(number);
			builder.append("d");
		}
		
		if(time >= 3600000L)
		{
			if(builder.length() != 0)
				builder.append(" ");
			
			int number = (int)(time / 3600000L);
			time -= number * 3600000L;
			
			builder.append(number);
			builder.append("h");
		}
		
		if(time >= 60000L)
		{
			if(builder.length() != 0)
				builder.append(" ");
			
			int number = (int)(time / 60000L);
			time -= number * 60000L;
			
			builder.append(number);
			builder.append("m");
		}
		
		if(time >= 1000L)
		{
			if(builder.length() != 0)
				builder.append(" ");
			
			int number = (int)(time / 1000L);
			
			builder.append(number);
			builder.append("s");
		}
		
		return builder.toString();
	}

	public static String colorize(String message, CommandSender sender)
	{
		int pos = -1;
		char colorChar = '&';
		boolean hasRGB = sender.hasPermission("bungeechat.format.rgb");
		String updatedMessage = message;
		if(hasRGB) {
			updatedMessage = parseRGBColors(message,Bukkit.getServer().getBukkitVersion());
		}
		StringBuilder buffer = new StringBuilder(updatedMessage);

		boolean hasColor = sender.hasPermission("bungeechat.color");
		boolean hasReset = sender.hasPermission("bungeechat.format.reset");
		boolean hasBold = sender.hasPermission("bungeechat.format.bold");
		boolean hasItalic = sender.hasPermission("bungeechat.format.italic");
		boolean hasUnderline = sender.hasPermission("bungeechat.format.underline");
		boolean hasStrikethrough = sender.hasPermission("bungeechat.format.strikethrough");
		boolean hasMagic = sender.hasPermission("bungeechat.format.magic");

		while((pos = updatedMessage.indexOf(colorChar, pos+1)) != -1)
		{
			if(updatedMessage.length() > pos + 1)
			{
				char atPos = Character.toLowerCase(updatedMessage.charAt(pos+1));

				boolean allow = false;
				if(((atPos >= '0' && atPos <= '9') || (atPos >= 'a' && atPos <= 'f')) && hasColor)
					allow = true;
				else if(atPos == 'r' && hasReset)
					allow = true;
				else if(atPos == 'l' && hasBold)
					allow = true;
				else if(atPos == 'm' && hasStrikethrough)
					allow = true;
				else if(atPos == 'n' && hasUnderline)
					allow = true;
				else if(atPos == 'o' && hasItalic)
					allow = true;
				else if(atPos == 'k' && hasMagic)
					allow = true;

				if(allow)
					buffer.setCharAt(pos, ChatColor.COLOR_CHAR);
			}
		}

		return buffer.toString();
	}

	public static String parseChatColors(final String input, final String serverVersion) {
		String out;
		out = ChatColor.translateAlternateColorCodes('&',input);
		return parseRGBColors(out,serverVersion);
	}

	public static String parseChatColors(final String input){
		String out;
		out = ChatColor.translateAlternateColorCodes('&',input);
		return parseRGBColors(out);
	}

	/**
	 * Parse RBG based on Server Versions
	 * @param input String
	 * @param serverVersion String
	 * @return String
	 */

	public static String parseRGBColors(final String input, final String serverVersion){
		boolean supported = true;
		VersionUtil.BukkitVersion version = VersionUtil.BukkitVersion.fromString(serverVersion);
		if(version.isLowerThan(VersionUtil.v1_16_1_R01)) {
			supported =false;
		}
		if(supported) {
			return parseRGBColors(input);
		} else {
			return stripRGBColors(input);
		}
	}

	/**
	 * Remove RGB if not supported.
	 *
	 * @param input String
	 * @return String
	 */
	public static String stripRGBColors (final String input) {
		String out = input;
		Matcher matcher = HEX_PATTERN.matcher(out);
		while(matcher.find()) {
			int groups = matcher.groupCount();
			for (int i=0;i < groups;i++) {
				String hex = matcher.group(i);
				out = out.replace(hex, "");
			}
			matcher = HEX_PATTERN.matcher(out);
		}
		return out;
	}

	/**
	 * Parse RBG to Color
	 * @param input String
	 * @return String
	 */
	public static String parseRGBColors(final String input){
		String out = input;
		Matcher matcher = HEX_PATTERN.matcher(out);
		while(matcher.find()) {
			int groups = matcher.groupCount();
			for (int i=0;i < groups;i++) {
				String hex = matcher.group(i);
				Color color;
				try {
					color = Color.decode(hex);
					out = out.replace(hex, ChatColor.of(color).toString());
				} catch (NumberFormatException e){
					out = out.replace(hex, "");
					Debugger.log("Invalid hex code removed: " +hex+ " from " + input);
				}
			}
			matcher = HEX_PATTERN.matcher(out);
		}
		return out;
	}
}
