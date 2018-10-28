package au.com.addstar.bc;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import net.md_5.bungee.protocol.packet.PlayerListItem.Action;
import net.md_5.bungee.protocol.packet.PlayerListItem.Item;

public class Debugger extends Command
{
	private static boolean mDebugEnabled = false;
	private static boolean mPacketDebugEnabled = false;
	private static boolean mTabDebugEnabled = false;
	
	public static void setGeneralDebugState(boolean on)
	{
		mDebugEnabled = on;
	}
	
	public static void setPacketDebugState(boolean on)
	{
		mPacketDebugEnabled = on;
	}
	
	public static void setTabDebugState(boolean on)
	{
		mTabDebugEnabled = on;
	}
	
	private static void log0(String category, String message)
	{
		if (category == null || category.isEmpty())
			BungeeChat.instance.getLogger().info(String.format("[Debug] %s", message));
		else
			BungeeChat.instance.getLogger().info(String.format("[Debug-%s] %s", category, message));
	}
	
	/**
	 * General logging
	 */
	public static void log(String message, Object... params)
	{
		if (mDebugEnabled)
			log0(null, String.format(message, params));
	}
	
	/**
	 * Packet logging
	 */
	public static void logp(String message, Object... params)
	{
		if (mPacketDebugEnabled)
			log0("Packet", String.format(message, params));
	}
	
	/**
	 * TabList logging
	 */
	public static void logt(String message, Object... params)
	{
		if (mTabDebugEnabled)
			log0("TabList", String.format(message, params).replace(ChatColor.COLOR_CHAR, '&'));
	}
	
	public static void logTabItem(PlayerListItem packet, ProxiedPlayer to)
	{
		if (!mTabDebugEnabled)
			return;
		
		boolean newTab = ColourTabList.isNewTab(to);
		for (Item item : packet.getItems())
		{
			String name = item.getUsername();
			if (!newTab)
				name = item.getDisplayName();
			
			String message = null;
			switch(packet.getAction())
			{
			case ADD_PLAYER:
				message = String.format("%d,%d,%s", item.getPing(), item.getGamemode(), item.getDisplayName());
				break;
			case REMOVE_PLAYER:
				message = item.getUuid().toString();
				break;
			case UPDATE_DISPLAY_NAME:
				message = item.getDisplayName();
				break;
			case UPDATE_GAMEMODE:
				message = String.valueOf(item.getGamemode());
				break;
			case UPDATE_LATENCY:
				message = String.valueOf(item.getPing());
				break;
			}
			
			if (packet.getAction() == Action.ADD_PLAYER)
				message = String.format("%s %s-%s: %s", packet.getAction().name(), name, item.getUuid().toString(), message);
			else if (name == null)
				message = String.format("%s %s: %s", packet.getAction().name(), item.getUuid().toString(), message);
			else
				message = String.format("%s %s: %s", packet.getAction().name(), name, message);
			
			log0("TabList", String.format("to %s: %s", to.getName(), message).replace(ChatColor.COLOR_CHAR, '&'));
		}
	}
	
	public static void logTrue(boolean expression, String message, Object... params)
	{
		if (!expression)
			log(message, params);
	}
	
	public Debugger()
	{
		super("!bchatdebug", "bungeechat.debug");
	}
	
	@Override
	public void execute( CommandSender sender, String[] args )
	{
		if (!onCommand(sender, args))
		{
			sender.sendMessage(TextComponent.fromLegacyText("/!bchatdebug general <true|false>"));
			sender.sendMessage(TextComponent.fromLegacyText("/!bchatdebug packet <true|false>"));
			sender.sendMessage(TextComponent.fromLegacyText("/!bchatdebug tab <true|false>"));
			sender.sendMessage(TextComponent.fromLegacyText("/!bchatdebug player <name>"));
			sender.sendMessage(TextComponent.fromLegacyText("/!bchatdebug allplayers"));
		}
	}
	
	public boolean onCommand( CommandSender sender, String[] args )
	{
		if (args.length == 0)
			return false;
		
		if (args[0].equalsIgnoreCase("general"))
		{
			if (args.length != 2)
				return false;
			
			boolean on = Boolean.valueOf(args[1]);
			setGeneralDebugState(on);
			
			sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GOLD + "General debug is now " + (on ? "on" : "off")));
		}
		else if (args[0].equalsIgnoreCase("packet"))
		{
			if (args.length != 2)
				return false;
			
			boolean on = Boolean.valueOf(args[1]);
			setPacketDebugState(on);
			
			sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GOLD + "Packet debug is now " + (on ? "on" : "off")));
		}
		else if (args[0].equalsIgnoreCase("tab"))
		{
			if (args.length != 2)
				return false;
			
			boolean on = Boolean.valueOf(args[1]);
			setTabDebugState(on);
			
			sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GOLD + "TabList debug is now " + (on ? "on" : "off")));
		}
		else
			return false;
		
		return true;
	}
}
