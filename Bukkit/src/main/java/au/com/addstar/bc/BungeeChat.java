package au.com.addstar.bc;

import au.com.addstar.bc.commands.*;
import au.com.addstar.bc.listeners.ChatHandler;
import au.com.addstar.bc.listeners.SystemMessagesHandler;
import au.com.addstar.bc.objects.ChannelType;
import au.com.addstar.bc.objects.Formatter;
import au.com.addstar.bc.objects.RemotePlayer;
import au.com.addstar.bc.sync.packet.*;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import au.com.addstar.bc.config.ChatChannelConfig;
import au.com.addstar.bc.config.KeywordHighlighterConfig;
import au.com.addstar.bc.config.PermissionSettingConfig;
import au.com.addstar.bc.event.ConfigReceiveEvent;
import au.com.addstar.bc.sync.BukkitComLink;
import au.com.addstar.bc.sync.IMethodCallback;
import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketManager;
import au.com.addstar.bc.sync.SyncConfig;
import au.com.addstar.bc.sync.SyncManager;
import au.com.addstar.bc.sync.SyncUtil;
import au.com.addstar.bc.utils.Utilities;

public class BungeeChat extends JavaPlugin implements Listener
{
	public static Permission permissionManager;
	
	public static String serverName = "ERROR";
	private static BungeeChat mInstance;

	private ChatChannelManager mChatChannels;
	private SocialSpyHandler mSocialSpyHandler;
	
	private PlayerManager mPlayerManager;
	private PacketManager mPacketManager;
	
	private AFKHandler mAfkHandler;
	private SystemMessagesHandler mMsgHandler;
	private MuteHandler mMuteHandler;
	
	private SyncManager mSyncManager;
	private BukkitComLink mComLink;
	public boolean debug;
	public static String forceGlobalprefix = "!";
	@SuppressWarnings( "unchecked" )
	@Override
	public void onEnable()
	{
		mInstance = this;
		debug = false;
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(Permission.class);
		if (permissionProvider != null)
			permissionManager = permissionProvider.getProvider();
		else
			permissionManager = null;
		
		Bukkit.getPluginManager().registerEvents(new ChatHandler(this), this);
		
		Bukkit.getPluginManager().registerEvents(this, this);
		
		mComLink = setupComLink();
		mPacketManager = new PacketManager(this);
		mChatChannels = new ChatChannelManager(this);
		mSocialSpyHandler = new SocialSpyHandler(this);
		mPlayerManager = new PlayerManager(this);
		mMsgHandler = new SystemMessagesHandler(this);
		mSyncManager = new SyncManager();
		SyncUtil.addSerializer(ChatChannelConfig.class, "ChatChannel");
		SyncUtil.addSerializer(KeywordHighlighterConfig.class, "KHSettings");
		SyncUtil.addSerializer(PermissionSettingConfig.class, "PermSetting");
		
		mPacketManager.addHandler(new PacketHandler(), MirrorPacket.class, SendPacket.class, UpdateNamePacket.class);
		mPacketManager.addHandler(mPlayerManager, (Class<? extends Packet>[])null);
		
		Bukkit.getScheduler().runTask(this, new Runnable()
		{
			@Override
			public void run()
			{
				mPacketManager.initialize();
				requestUpdate();
			}
		});
		
		MessageCommand cmd = new MessageCommand();
		
		getCommand("tell").setExecutor(cmd);
		getCommand("tell").setTabCompleter(cmd);
		getCommand("reply").setExecutor(cmd);
		getCommand("reply").setTabCompleter(cmd);
		getCommand("msgtoggle").setExecutor(cmd);
		getCommand("socialspy").setExecutor(mSocialSpyHandler);
		
		NicknameCommand nickname = new NicknameCommand();
		getCommand("nickname").setExecutor(nickname);
		getCommand("nickname").setTabCompleter(nickname);
		
		SkinCommand skinCommand = new SkinCommand();
		getCommand("skin").setExecutor(skinCommand);
		getCommand("skin").setTabCompleter(skinCommand);
		
		mMuteHandler = new MuteHandler(this);
		getCommand("mute").setExecutor(mMuteHandler);
		getCommand("mute").setTabCompleter(mMuteHandler);
		getCommand("unmute").setExecutor(mMuteHandler);
		getCommand("unmute").setTabCompleter(mMuteHandler);
		getCommand("ipmute").setExecutor(mMuteHandler);
		getCommand("ipmute").setTabCompleter(mMuteHandler);
		getCommand("ipunmute").setExecutor(mMuteHandler);
		getCommand("ipunmute").setTabCompleter(mMuteHandler);
		getCommand("mutelist").setExecutor(mMuteHandler);
		getCommand("globalmute").setExecutor(mMuteHandler);
		
		RealnameCommand realname = new RealnameCommand();
		getCommand("realname").setExecutor(realname);
		getCommand("realname").setTabCompleter(realname);
		
		mAfkHandler = new AFKHandler(this);
		getCommand("afk").setExecutor(mAfkHandler);
		getCommand("afk").setTabCompleter(mAfkHandler);
		
		getCommand("runchat").setExecutor(mChatChannels);
		getCommand("bchatdebug").setExecutor(new Debugger());
		getCommand("chat").setExecutor(new SubscribeCommand(mInstance));
		getCommand("chanlist").setExecutor(new ChannelListCommand(mInstance));
		getCommand("chatname").setExecutor(new SetChatNameCommand());
	}
	
	@Override
	public void onDisable()
	{
		mChatChannels.unregisterAll();
	}
	
	private BukkitComLink setupComLink()
	{
		saveDefaultConfig();
		String host = getConfig().getString("redis.host", "localhost");
		int port = getConfig().getInt("redis.host", 6379);
		String password = getConfig().getString("redis.password", "");
		
		BukkitComLink link = new BukkitComLink();
		link.init(host, port, password);
		return link;
	}
	
	public void requestUpdate()
	{
		mSyncManager.requestConfigUpdate("bungeechat");
		mSyncManager.callSyncMethod("bungee:getServerName", new IMethodCallback<String>()
		{
			@Override
			public void onFinished( String data )
			{
				serverName = data;
			}
			
			@Override
			public void onError( String type, String message )
			{
				throw new RuntimeException(type + ": " + message);
			}
		});
		
		mPacketManager.send(new PlayerListRequestPacket());
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	private void onConfigUpdate(ConfigReceiveEvent event)
	{
		if(event.getName().equals("bungeechat"))
		{
			SyncConfig config = event.getConfig();
			Formatter.load(config);
			mChatChannels.load(config);
			mSocialSpyHandler.load(config);
			mAfkHandler.load(config);
			debug = config.getBoolean("debug", false);
			forceGlobalprefix = config.getString("forceGlobalPrefix", "!");
		}
	}

	public static void sendMessage(RemotePlayer player, String message)
	{
		getPacketManager().broadcast(new SendPacket(player.getUniqueId(), message));
	}
	
	public static void mirrorChat(String fullChat, String channel)
	{
		getPacketManager().broadcast(new MirrorPacket(channel, fullChat));
	}
	
	public static void broadcast(String message)
	{
		Utilities.broadcast(message, null, null);
		BungeeChat.mirrorChat(message, ChannelType.Broadcast.getName());
	}
	
	public static String getPrimaryGroup(Player player)
	{
		if(permissionManager == null)
			return null;
		
		try
		{
			return permissionManager.getPrimaryGroup(player);
		}
		catch(UnsupportedOperationException e)
		{
			return null;
		}
	}
	
	public static String colorize(String message, CommandSender sender)
	{
		int pos = -1;
		char colorChar = '&';
		
		StringBuilder buffer = new StringBuilder(message);
		
		boolean hasColor = sender.hasPermission("bungeechat.color");
		boolean hasReset = sender.hasPermission("bungeechat.format.reset");
		boolean hasBold = sender.hasPermission("bungeechat.format.bold");
		boolean hasItalic = sender.hasPermission("bungeechat.format.italic");
		boolean hasUnderline = sender.hasPermission("bungeechat.format.underline");
		boolean hasStrikethrough = sender.hasPermission("bungeechat.format.strikethrough");
		boolean hasMagic = sender.hasPermission("bungeechat.format.magic");
		
		while((pos = message.indexOf(colorChar, pos+1)) != -1)
		{
			if(message.length() > pos + 1)
			{
				char atPos = Character.toLowerCase(message.charAt(pos+1));
				
				boolean allow = false;
				if(((atPos >= '0' && atPos <= '9') || (atPos >= 'a' && atPos <= 'f')) && hasColor)
					allow = true;
				else if(atPos == 'r' && hasReset)
					allow = true;
				else if(atPos == 'l' && hasBold)
					allow = true;
				else if(atPos == 'm' && hasStrikethrough)
					allow = true;
				else if(atPos == 'n' && hasUnderline)
					allow = true;
				else if(atPos == 'o' && hasItalic)
					allow = true;
				else if(atPos == 'k' && hasMagic)
					allow = true;
				
				if(allow)
					buffer.setCharAt(pos, ChatColor.COLOR_CHAR);
			}
		}
		
		return buffer.toString();
	}
	
	public static void setLastMsgTarget(CommandSender sender, CommandSender target)
	{
		if(sender instanceof Player)
		{
			getPlayerManager().getPlayerSettings(sender).lastMsgTarget = PlayerManager.getUniqueId(target);
			getPlayerManager().updatePlayerSettings(sender);
		}
		else if(sender instanceof RemotePlayer)
			getSyncManager().callSyncMethod("bchat:setMsgTarget", null, PlayerManager.getUniqueId(sender), PlayerManager.getUniqueId(target));
	}
	
	public static PlayerManager getPlayerManager()
	{
		return mInstance.mPlayerManager;
	}
	
	public static boolean isSocialSpyEnabled( CommandSender player )
	{
		return mInstance.mSocialSpyHandler.isEnabled(player);
	}
	
	public static AFKHandler getAFKHandler()
	{
		return mInstance.mAfkHandler;
	}
	
	public static SystemMessagesHandler getSysMsgHandler()
	{
		return mInstance.mMsgHandler;
	}
	
	public static SyncManager getSyncManager()
	{
		return mInstance.mSyncManager;
	}
	
	public static PacketManager getPacketManager()
	{
		return mInstance.mPacketManager;
	}
	
	public static BukkitComLink getComLink()
	{
		return mInstance.mComLink;
	}
	
	public static BungeeChat getInstance()
	{
		return mInstance;
	}

	public ChatChannelManager getChatChannelsManager() {
		return mChatChannels;
	}
}
