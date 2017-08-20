package au.com.addstar.bc;

import au.com.addstar.bc.config.ChatChannel;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

import java.util.Map;

public class ManagementCommand extends Command
{
	private BungeeChat mPlugin;
	
	public ManagementCommand(BungeeChat plugin)
	{
		super("bungeechat", "bungeechat.command.manage", "bchat");
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

		switch(args[0]){
			case "reload":

			if(mPlugin.loadConfig())
			{
				mPlugin.applySyncConfig();
				mPlugin.getSyncManager().sendConfig("bungeechat");
				sender.sendMessage(new TextComponent("BungeeChat config reloaded"));
			}
			else
			{
				sender.sendMessage(new TextComponent("An error occured while loading the BungeeChat config! Check the Proxy console"));
			}
			break;
			case "listchannel":
				sender.sendMessage(new TextComponent("List of Channels"));
				sender.sendMessage(new TextComponent("<Name> : <ListenPermission> : <SpeakPermission> : <command> "));

				for(Map.Entry<String, ChatChannel> entry: mPlugin.getChannels().entrySet()) {
					sender.sendMessage(new TextComponent(entry.getKey() + " : "+ entry.getValue().listenPermission +
							" : "+ entry.getValue().permission +" : "+entry.getValue().command));

				}
				sender.sendMessage(new TextComponent("------------------"));
			break;
			default:
				sender.sendMessage(new TextComponent("Usage: /bungeechat reload"));
				break;
		}
		}
	}
