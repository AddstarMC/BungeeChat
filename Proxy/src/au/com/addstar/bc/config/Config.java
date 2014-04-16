package au.com.addstar.bc.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import au.com.addstar.bc.sync.SyncConfig;

import net.cubespace.Yamler.Config.Comment;
import net.cubespace.Yamler.Config.Path;

public class Config extends net.cubespace.Yamler.Config.Config
{
	public Config(File file)
	{
		super(file.getPath(), "BungeeChat config");
		
		permSettings = new LinkedHashMap<String, PermissionSetting>();
		channels = new LinkedHashMap<String, ChatChannel>();
		
		permSettings.put("default", new PermissionSetting("<{DISPLAYNAME}>: {MESSAGE}", "f", 0));
		channels.put("BCast", new ChatChannel("bcast", "&6[&4Broadcast&6] &a{MESSAGE}", "bungeechat.broadcast", "*"));
		
		keywordHighlighter = new KeywordHighlighterSettings();
		
		socialSpyKeywords = new ArrayList<String>(Arrays.asList("msg", "m", "w", "whisper", "t", "tell", "r", "reply"));
	}
	
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
	
	@Comment("The time in seconds of no activity that someone is considered afk.")
	@Path("afk-delay")
	public int afkDelay = 30;
	
	@Path("afk-kick-enabled")
	public boolean afkKickEnabled = false;
	
	@Comment("The time in minutes of being afk that someone is kicked")
	@Path("afk-kick-delay")
	public int afkKickDelay = 30;
	
	@Path("afk-kick-message")
	public String afkKickMessage = "You have been kicked for idling for %d minutes";
	
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
		
		config.set("socialspykeywords", socialSpyKeywords);

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
