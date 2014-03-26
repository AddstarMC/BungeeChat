package au.com.addstar.bc;

import java.util.ArrayList;
import java.util.List;

import net.cubespace.Yamler.Config.Config;

public class KeywordHighlighterSettings extends Config
{
	public String permission = "bungeechat.keywords";
	public boolean enabled = false;
	public List<String> allowInChannels = new ArrayList<String>();
	public String keywordFile = "keywords.txt";
}
