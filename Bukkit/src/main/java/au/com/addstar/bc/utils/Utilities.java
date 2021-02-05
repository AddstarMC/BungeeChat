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

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.com.addstar.bc.BungeeChat;
import net.kyori.adventure.platform.AudienceProvider;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;

public class Utilities
{
	public static final NoConsoleChecker NO_CONSOLE = new NoConsoleChecker();
	public static final SocialSpyChecker SOCIAL_SPY_ENABLED = new SocialSpyChecker();
	private static final Pattern HEX_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");

	public static void broadcast(Component component, String permission, ValidChecker<CommandSender> checker , AudienceProvider audience) {
		Collection<? extends Permissible> targets;
		if(permission != null) {
			targets = Bukkit.getPluginManager().getPermissionSubscriptions(permission);
		} else {
			targets = Bukkit.getOnlinePlayers();
		}
		for(Permissible perm : targets)
		{
			sendPermissionMessageToAudience(component, permission, checker, audience, perm);
		}
	}

	private static void sendPermissionMessageToAudience(Component component, String permission, ValidChecker<CommandSender> checker, AudienceProvider audience, Permissible perm) {
		if(perm instanceof CommandSender && (permission == null || perm.hasPermission(permission)) && (checker == null || checker.isValid((CommandSender)perm))) {
			if(audience instanceof BukkitAudiences) {
				((BukkitAudiences)audience).sender((CommandSender) perm).sendMessage(component);
			} else {
				audience.console().sendMessage(component);
			}
		}
	}

	public static void broadcast(Component component, String permission, ValidChecker<CommandSender> checker) {
		broadcast(component,permission,checker,BungeeChat.audiences);
	}

	@Deprecated
	public static void broadcast(String message, String permission, ValidChecker<CommandSender> checker) {
		broadcast(TextComponent.of(message),permission,checker);
	}

	public static void broadcast(Component component, String permission, CommandSender except, ValidChecker<CommandSender> checker) {
		broadcast(component,permission,except,checker,BungeeChat.audiences);
	}


	public static void broadcast(Component component, String permission, CommandSender except, ValidChecker<CommandSender> checker, AudienceProvider audienceProvider)
	{
		Collection<? extends Permissible> targets;
		if(permission != null)
			targets = Bukkit.getPluginManager().getPermissionSubscriptions(permission);
		else
			targets = Bukkit.getOnlinePlayers();

		for(Permissible perm : targets)
		{
			if(perm == except) {
				continue;
			}
			sendPermissionMessageToAudience(component, permission, checker, audienceProvider, perm);
		}
	}

	@Deprecated
	public static void broadcast(String message, String permission, CommandSender except, ValidChecker<CommandSender> checker) {
		broadcast(TextComponent.of(message),permission,except,checker);
	}

	public static void broadcast(Component component, String permission) {
		broadcast(component,permission,null);
	}

	@Deprecated
	public static void broadcast(String message, String permission) {
		broadcast(TextComponent.of(message),permission,null);
	}

	/**
	 * Converts color place-holders to {@link MiniMessage} formatted colours as a string
	 *
	 * @param text text to color
	 * @return A String
	 */
	public static String colorize(String text) {
		if(text.indexOf('&') > -1) {
			return MiniMessage.get().serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
		} else {
			return text;
		}
	}

	public static Component colorizeAsComponent(String text) {
		if(text.indexOf('&') > -1) {
			return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
		} else {
			return MiniMessage.get().deserialize(text);
		}
	}

	/**
	 * Parses the text in Minimessage format and makes sure it has atleast some content.
	 * @param text
	 * @return boolean true if empty
	 */
	public static boolean isEmpty(String text) {
		Component c = MiniMessage.get().parse(text);
		return isEmpty(c);
	}

	public static boolean isEmpty(Component text) {
		return !componentHasContent(text);
	}
	private static boolean componentHasContent(Component c){
		if(c instanceof TextComponent){
			if(!((TextComponent) c).content().isEmpty()) {
				return true;
			}
		}
		if(c instanceof TranslatableComponent) {
			if (!((TranslatableComponent) c).key().isEmpty()) {
				return true;
			}
		}
		for(Component child:c.children()){
			if(componentHasContent(child)){
				return true;
			}
		}
		return false;
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

	/**
	 * Removes Styling if a sender does NOT have correct permissions.
	 * @param component String {@link MiniMessage} formatted
	 * @param sender CommandSender;
	 * @return String cleaned of all disallowed styling.
	 */
	private static Component filterStyles(Component component, CommandSender sender){
		boolean hasBold = sender.hasPermission("bungeechat.format.bold");
		boolean hasItalic = sender.hasPermission("bungeechat.format.italic");
		boolean hasUnderline = sender.hasPermission("bungeechat.format.underline");
		boolean hasStrikethrough = sender.hasPermission("bungeechat.format.strikethrough");
		boolean hasMagic = sender.hasPermission("bungeechat.format.magic");
		boolean hasColor = sender.hasPermission("bungeechat.color");
		checkComponentColor(component,hasColor);
		checkComponent(component,TextDecoration.BOLD,hasBold);
		checkComponent(component,TextDecoration.ITALIC,hasItalic);
		checkComponent(component,TextDecoration.UNDERLINED,hasUnderline);
		checkComponent(component,TextDecoration.STRIKETHROUGH,hasStrikethrough);
		checkComponent(component,TextDecoration.OBFUSCATED,hasMagic);
		return  component;

	}

	private static  void checkComponentColor(Component component, boolean allowed) {
		if(component.color() != null && !allowed) {
			component.color(null);
		}
		for(Component c: component.children()) {
			checkComponentColor(c,allowed);
		}
	}

	private static void checkComponent(Component component, TextDecoration decoration, boolean allowed) {
		if(component.hasDecoration(decoration) && !allowed && component.decoration(decoration).equals(TextDecoration.State.TRUE)) {
			component.decoration(decoration, TextDecoration.State.FALSE);
		}
		for (Component c:component.children()){
			checkComponent(c,decoration,allowed);
		}
	}

	public static Component colorizeComponent(String message, CommandSender sender)
	{
		Component c = colorizeAsComponent(message);
		return filterStyles(c,sender);
	}

	public static String colorize(String message, CommandSender sender)
	{
		Component c = colorizeAsComponent(message);
		return MiniMessage.get().serialize(filterStyles(c,sender));
	}

	public static String parseChatColors(final String input){
		Component component = MiniMessage.get().parse(input);
		return MiniMessage.get().serialize(component);
	}


	public static TextColor getColor(String color) {
		TextColor out = NamedTextColor.NAMES.value(color);
		if(out != null) {
			return out;
		}
		if(color.indexOf('&') > -1 ){
			TextComponent component = LegacyComponentSerializer.legacyAmpersand()
				.deserialize(color+" ");
			return component.color();
		}
		try {
			return TextColor.of(Integer.parseInt(color,16));

		} catch (NumberFormatException e){
			return null;
		}

	}

}
