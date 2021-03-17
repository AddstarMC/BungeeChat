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

package au.com.addstar.bc.objects;

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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import au.com.addstar.bc.BungeeChat;
import au.com.addstar.bc.PermissionSetting;
import com.google.gson.reflect.TypeToken;
import io.papermc.paper.chat.ChatFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionDefault;

import au.com.addstar.bc.config.KeywordHighlighterConfig;
import au.com.addstar.bc.config.PermissionSettingConfig;
import au.com.addstar.bc.sync.SyncConfig;
import au.com.addstar.bc.utils.NoPermissionChecker;
import au.com.addstar.bc.utils.Utilities;
import org.jetbrains.annotations.NotNull;

public class Formatter
{
	public static ArrayList<PermissionSetting> permissionLevels = new ArrayList<>();
	public static Component consoleOverride = null;
	
	private static Component mDefaultFormat = Component.text("<%DISPLAYNAME%> %MESSAGE%");
	private static Component mRpDefaultFormat = Component.text("<%DISPLAYNAME%>(%CHATNAME%) %MESSAGE%");
	public static boolean keywordsEnabled;

	public static ArrayList<String> keywordEnabledChannels = new ArrayList<>();
	public static String keywordPerm;
	public static HashMap<Pattern, Style> keywordPatterns = new HashMap<>();
	
	public static Component mPMFormatInbound = Component.text("[%DISPLAYNAME% -> Me]: %MESSAGE%");
	public static Component mPMFormatOutbound = Component.text("[Me -> %DISPLAYNAME%]: %MESSAGE%");

	private final static Pattern DISPLAYNAME = Pattern.compile("%DISPLAYNAME%");
	private final static Pattern RAWDISPLAYNAME = Pattern.compile("%RAWDISPLAYNAME%");
	private final static Pattern NAME = Pattern.compile("%NAME%");
	private final static Pattern SERVER = Pattern.compile("%SERVER%");
	private final static Pattern CHATNAME = Pattern.compile("%CHATNAME%");
	private final static Pattern GROUP = Pattern.compile("%GROUP%");
	private final static Pattern WORLD = Pattern.compile("%WORLD%");
	private final static Pattern MESSAGE = Pattern.compile("%MESSAGE%");

	public static void load(SyncConfig config)
	{
		if(!config.getString("consolename", "").isEmpty())
			consoleOverride = Utilities.colorize(config.getString("consolename", ""));
		else
			consoleOverride = null;
		
		mPMFormatInbound = Utilities.colorize(config.getString("pm-format-in", "[%DISPLAYNAME% -> Me]: %MESSAGE%"));
		mPMFormatOutbound = Utilities.colorize(config.getString("pm-format-out", "[Me -> %DISPLAYNAME%}]: %MESSAGE%"));
		
		permissionLevels.clear();
		
		SyncConfig permLevels = config.getSection("perms");
		for(String key : permLevels.getKeys())
		{
			PermissionSettingConfig setting = (PermissionSettingConfig) permLevels.get(key, null);
			permissionLevels.add(setting.convert());
		}
		
		Collections.sort(Formatter.permissionLevels);
		
		KeywordHighlighterConfig kh = (KeywordHighlighterConfig)config.get("highlight", null);
		
		keywordsEnabled = kh.enabled;
		if(kh.enabled)
		{
			keywordPerm = kh.permission;
			keywordEnabledChannels.clear();
			keywordPatterns.clear();
			
			SyncConfig keywords = config.getSection("keywords");
			for(String key : keywords.getKeys())
			{
				try
				{
					Pattern pattern = Pattern.compile(key, Pattern.CASE_INSENSITIVE);
					String jsonStyle = keywords.getString(key, null);
					if(jsonStyle != null) {
						Type styleToken = TypeToken.get(Style.class).getType();
						Style style = GsonComponentSerializer.gson().serializer().fromJson(jsonStyle,styleToken);
						Formatter.keywordPatterns.put(pattern,style);
					}
					Formatter.keywordPatterns.put(pattern,null);

				}
				catch (PatternSyntaxException e)
				{
					// Cant happen
				}
			}
			
			try
			{
				Bukkit.getPluginManager().addPermission(new org.bukkit.permissions.Permission(keywordPerm, PermissionDefault.OP));
			}
			catch(IllegalArgumentException ignored)
			{
			}
		}
	}
	
	public static PermissionSetting getPermissionLevel(Permissible sender)
	{
		if((sender instanceof ConsoleCommandSender) && !permissionLevels.isEmpty())
			return permissionLevels.get(permissionLevels.size()-1);
		
		if (!(sender instanceof Player))
			return null;
		
		PermissionSetting level = null;
		for(PermissionSetting setting : permissionLevels)
		{
			if(setting.permission == null || sender.hasPermission(setting.permission))
				level = setting;
		}
		
		return level;
	}
	
	public static Component getChatFormat(PermissionSetting level)
	{
		if(level != null)
			return level.textComponentFormat;
		else
			return mDefaultFormat;
	}

	public static ChatFormatter getChatFormatForUse(Player player, PermissionSetting level)
	{
		return (displayName, message) -> replaceKeywords(getChatFormat(level),player,level);
	}

	private static Component getChatName(CommandSender sender){
		if(sender instanceof Player) {
			return BungeeChat.getPlayerManager().getPlayerChatName(sender);
		}else if (sender instanceof RemotePlayer){
			return BungeeChat.getPlayerManager().getPlayerChatName(sender);
		}else{
			return Component.empty();
		}
	}

	public static Component getDisplayName(CommandSender sender, PermissionSetting level)
	{
		//todo Possible cache display names to save parsing

		if(consoleOverride != null && sender == Bukkit.getConsoleSender())
			return consoleOverride;
		final String displayName;
		if(sender instanceof Player)
			displayName = ((Player)sender).getDisplayName();
		else if(sender instanceof RemotePlayer)
			displayName = ((RemotePlayer)sender).getDisplayName();
		else
			displayName = sender.getName();
		Component result;
		if(level != null) {
			result = MiniMessage.get().parse(level.color, Template.of("%NAME%", displayName));
		} else {
			result = Component.text(displayName);
		}
		return result;
	}

	public static Component replaceMessage(Component format,final Component message){
		return format.replaceText(builder -> builder.match(MESSAGE).replacement(message));

	}

	/**
	 * Replace the keywords.
	 *
	 * @param compo Component
	 * @param sender Sender
	 * @param level PermissionLevel
	 * @return Component
	 */

	public static Component replaceKeywords(Component compo, CommandSender sender, PermissionSetting level)
	{
		Component displayName = getDisplayName(sender,level);
		Component rawDisplayName = getDisplayName(sender,null);
		compo = compo
			.replaceText(builder -> builder.match(DISPLAYNAME).replacement(displayName))
			.replaceText(builder -> builder.match(RAWDISPLAYNAME).replacement(rawDisplayName))
			.replaceText(builder -> builder.match(NAME).replacement(sender.getName()))
			.replaceText(builder -> builder.match(SERVER).replacement(sender.getServer().getName()));
		compo = updateIfPlayer(sender,compo);
		return compo;
	}

	/**
	 * Replace the keywords.
	 *
	 * @param string Component
	 * @param sender Sender
	 * @param level PermissionLevel
	 * @return Component
	 * @deprecated  Use {@link Formatter#replaceKeywords(Component, CommandSender, PermissionSetting)}
	 */
	@Deprecated
	public static Component replaceKeywordsPartial(Component string, CommandSender sender, PermissionSetting level)
	{
		return replaceKeywords(string,sender,level);
	}
	
	public static Component updateIfPlayer(CommandSender sender, Component message){
		if (sender instanceof Player) {
			Player player = (Player)sender;
			String group = BungeeChat.getPrimaryGroup(player);
			message = message
				.replaceText(builder -> builder.match(CHATNAME).replacement(getChatName(sender)))
				.replaceText(builder -> builder.match(GROUP).replacement(group != null ? group : "Default"))
				.replaceText(builder -> builder.match(WORLD).replacement(player.getWorld().getName()));
		}
		else {
			message = message
				.replaceText(builder -> builder.match(CHATNAME).replacement(""))
				.replaceText(builder -> builder.match(SERVER).replacement("Server"))
				.replaceText(builder -> builder.match(WORLD).replacement(""));

		}
		return message;
	}
	
	public static Component getPMFormat(CommandSender to, boolean inbound)
	{
		PermissionSetting level = getPermissionLevel(to);
		
		if(inbound)
			return replaceKeywords(mPMFormatInbound, to, level);
		else
			return replaceKeywords(mPMFormatOutbound, to, level);
	}


	/**
	 * Adds Key word highlighting to a message.
	 * @param message Component
	 * @return Component or null if no highlighting performed
	 */
	public static Component highlightKeywords(Component message)
	{
		AtomicBoolean atomicBoolean = new AtomicBoolean(false);
		for(Entry<Pattern, Style> entry : keywordPatterns.entrySet()) {
			message = message.replaceText(builder -> builder.match(entry.getKey()).replacement(builder1 -> {
				atomicBoolean.set(true);
				return builder1.style(entry.getValue());
			}));
		}
		if(!atomicBoolean.get()) {
			return null;
		}
		return message;
	}
	
	public static void localBroadcast(Component message)
	{
		if(!keywordsEnabled)
			Utilities.localBroadCast(message, (String) null);
		else
			Utilities.localBroadCast(message, null, new NoPermissionChecker(keywordPerm));
	}
}
