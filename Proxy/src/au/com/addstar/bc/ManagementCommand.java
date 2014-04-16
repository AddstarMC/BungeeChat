package au.com.addstar.bc;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class ManagementCommand extends Command
{
	private BungeeChat mPlugin;
	
	public ManagementCommand(BungeeChat plugin)
	{
		super("bungeechat", "bungeechat.command.manage", new String[] {"bchat"});
		mPlugin = plugin;
	}

	@Override
	public void execute( CommandSender sender, String[] args )
	{
		if(args.length != 1)
		{
			sender.sendMessage(new TextComponent("Usage: /bungeechat reload"));
			return;
		}

		if(args[0].equalsIgnoreCase("reload"))
		{
			if(mPlugin.loadConfig())
			{
				mPlugin.getSyncManager().sendConfig("bungeechat");
				sender.sendMessage(new TextComponent("BungeeChat config reloaded"));
			}
			else
			{
				sender.sendMessage(new TextComponent("An error occured while loading the BungeeChat config! Check the Proxy console"));
			}
		}
		else
		{
			sender.sendMessage(new TextComponent("Usage: /bungeechat reload"));
			return;
		}
	}

}
