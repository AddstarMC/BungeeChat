package au.com.addstar.bc.config;

import net.cubespace.Yamler.Config.Comments;
import net.cubespace.Yamler.Config.Path;
import net.cubespace.Yamler.Config.YamlConfig;

public class ServerConfig extends YamlConfig
{
	@Comments({"Changes what text appears in the tab header. This may contain tokens:", "{PLAYER} The players name", "{DISPLAYNAME} The players display name", "{TABNAME} The players tab display name (includes colour)", "{COUNT} The player count", "{MAX} The max player count", "{SERVER} The servers name"})
	@Path("tab.header")
	public String tabListHeader = "Welcome";
	@Comments({"Changes what text appears in the tab footer. This may contain tokens:", "{PLAYER} The players name", "{DISPLAYNAME} The players display name", "{TABNAME} The players tab display name (includes colour)", "{COUNT} The player count", "{MAX} The max player count", "{SERVER} The servers name"})
	@Path("tab.footer")
	public String tabListFooter = "&l{COUNT}/{MAX}";
}
