package au.com.addstar.bc.utils;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;

public class Utilities
{
	public static final NoConsoleChecker NO_CONSOLE = new NoConsoleChecker();
	public static final SocialSpyChecker SOCIAL_SPY_ENABLED = new SocialSpyChecker();
	
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
	 * @param text
	 * @return
	 */
	public static String colorize(String text) {
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
}
