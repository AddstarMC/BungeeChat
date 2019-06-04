package au.com.addstar.bc.config;

/*-
 * #%L
 * BungeeChat-Proxy
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import au.com.addstar.bc.sync.SyncConfig;
import net.cubespace.Yamler.Config.Comment;
import net.cubespace.Yamler.Config.Comments;
import net.cubespace.Yamler.Config.Path;
import net.cubespace.Yamler.Config.YamlConfig;

public class Config extends YamlConfig
{

	@Comment("You can override the name of the console here. Leave blank for no change")
	public String consoleName = "";
	
	@Path("PM-format-in")
	public String pmFormatIn = "[{DISPLAYNAME}&r -> Me]: {MESSAGE}";
	@Path("PM-format-out")
	public String pmFormatOut = "[Me -> {DISPLAYNAME}&r]: {MESSAGE}";
	
	@Comment("Here you can set up permission based formats")
	public Map<String, PermissionSetting> permSettings;
	
	@Comment("Channels allow you to set up layers of chat based on permissions")
	public Map<String, ChatChannel> channels;
	
	@Comment("Keyword highlighter allows you to show some words as highlighted to some users")
	public KeywordHighlighterSettings keywordHighlighter;
	
	public List<String> socialSpyKeywords;
	
	@Comment("List of commands to be included in mute")
	public List<String> mutedCommands;
	
	@Comment("The time in seconds of no activity that someone is considered afk.")
	@Path("afk-delay")
	public int afkDelay = 30;
	
	@Path("afk-kick-enabled")
	public boolean afkKickEnabled = false;

	@Path("debug")
	public boolean debug = false;

	@Comment("The time in minutes of being afk that someone is kicked")
	@Path("afk-kick-delay")
	public int afkKickDelay = 30;
	
	@Path("afk-kick-message")
	public String afkKickMessage = "You have been kicked for idling more than %d minutes.";
	
	@Comment("Settings for redis so that")
	public RedisSettings redis = new RedisSettings();
	
	@Comments({"Changes what text appears in the tab header. This may contain tokens:", "{PLAYER} The players name", "{DISPLAYNAME} The players display name", "{TABNAME} The players tab display name (includes colour)", "{COUNT} The player count", "{MAX} The max player count", "{SERVER} The servers name"})
	@Path("tab.header")
	public String tabListHeader = "Welcome";
	@Comments({"Changes what text appears in the tab footer. This may contain tokens:", "{PLAYER} The players name", "{DISPLAYNAME} The players display name", "{TABNAME} The players tab display name (includes colour)", "{COUNT} The player count", "{MAX} The max player count", "{SERVER} The servers name"})
	@Path("tab.footer")
	public String tabListFooter = "&l{COUNT}/{MAX}";
	
	@Comments("Place server specific settings here")
	public HashMap<String, ServerConfig> servers = new HashMap<>();

	public Config(File file)
	{
		//super(file.getPath(), "BungeeChat config");
		super(file.getPath());

		permSettings = new LinkedHashMap<>();
		channels = new LinkedHashMap<>();

		permSettings.put("default", new PermissionSetting("<{DISPLAYNAME}>: {MESSAGE}", "f", 0));
		channels.put("BCast", new ChatChannel("bcast", "&6[&4Broadcast&6] &a{MESSAGE}", "bungeechat.broadcast", "*"));

		keywordHighlighter = new KeywordHighlighterSettings();

		socialSpyKeywords = new ArrayList<>(Arrays.asList("msg", "m", "w", "whisper", "t", "tell", "r", "reply"));
		mutedCommands = new ArrayList<>(Arrays.asList("msg", "m", "w", "whisper", "t", "tell", "r", "reply", "me", "afk"));

		servers.put("servername", new ServerConfig());
	}

	public SyncConfig toSyncConfig()
	{
		SyncConfig config = new SyncConfig();
		
		config.set("consolename", consoleName);
		
		config.set("pm-format-in", pmFormatIn);
		config.set("pm-format-out", pmFormatOut);
		
		config.set("afk-delay", afkDelay);
		config.set("afk-kick-enabled", afkKickEnabled);
		config.set("afk-kick-message", afkKickMessage);
		config.set("afk-kick-delay", afkKickDelay);
		config.set("debug", debug);
		
		config.set("socialspykeywords", socialSpyKeywords);
		config.set("mutedcommands", mutedCommands);

		config.set("highlight", keywordHighlighter);
		
		SyncConfig subChannels = config.createSection("channels");
		for(Entry<String, ChatChannel> channel : channels.entrySet())
			subChannels.set(channel.getKey(), channel.getValue());

		SyncConfig permLevels = config.createSection("perms");
		for(Entry<String, PermissionSetting> setting : permSettings.entrySet())
			permLevels.set(setting.getKey(), setting.getValue());
		
		return config;
	}
}
