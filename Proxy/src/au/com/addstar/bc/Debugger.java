package au.com.addstar.bc;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class Debugger extends Command
{
	private static boolean mDebugEnabled = false;
	private static boolean mPacketDebugEnabled = false;
	
	public static void setGeneralDebugState(boolean on)
	{
		mDebugEnabled = on;
	}
	
	public static void setPacketDebugState(boolean on)
	{
		mPacketDebugEnabled = on;
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
		else
			return false;
		
		return true;
	}
}
