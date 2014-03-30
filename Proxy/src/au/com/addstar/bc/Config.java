package au.com.addstar.bc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
}
