package au.com.addstar.bc;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import au.com.addstar.bc.config.ChatChannelConfig;
import au.com.addstar.bc.config.KeywordHighlighterConfig;
import au.com.addstar.bc.config.PermissionSettingConfig;
import au.com.addstar.bc.sync.ConfigReceiveEvent;
import au.com.addstar.bc.sync.IMethodCallback;
import au.com.addstar.bc.sync.SyncConfig;
import au.com.addstar.bc.sync.SyncManager;
import au.com.addstar.bc.sync.SyncUtil;

public class BungeeChat extends JavaPlugin implements PluginMessageListener, Listener
{
	static Permission permissionManager;
	
	public static String serverName = "ERROR";
	private static BungeeChat mInstance;
	
	private boolean mHasRequestedUpdate = false;
	private boolean mHasUpdated = false;
	
	
	private ChatChannelManager mChatChannels;
	private SocialSpyHandler mSocialSpyHandler;
	
	private ArrayList<IDataReceiver> mReceivers = new ArrayList<IDataReceiver>();
	
	private PlayerManager mPlayerManager;
	
	private AFKHandler mAfkHandler;
	private SystemMessagesHandler mMsgHandler;
	
	private SyncManager mSyncManager;
	
	@Override
	public void onEnable()
	{
		mInstance = this;
		
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(Permission.class);
		if (permissionProvider != null)
			permissionManager = permissionProvider.getProvider();
		else
			permissionManager = null;
		
		Bukkit.getPluginManager().registerEvents(new ChatHandler(), this);
		Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeChat", this);
		Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeChat");
		
		Bukkit.getPluginManager().registerEvents(this, this);
		
		mChatChannels = new ChatChannelManager(this);
		mSocialSpyHandler = new SocialSpyHandler(this);
		mPlayerManager = new PlayerManager(this);
		mMsgHandler = new SystemMessagesHandler(this);
		mSyncManager = new SyncManager(this);
		SyncUtil.addSerializer(ChatChannelConfig.class, "ChatChannel");
		SyncUtil.addSerializer(KeywordHighlighterConfig.class, "KHSettings");
		SyncUtil.addSerializer(PermissionSettingConfig.class, "PermSetting");
		
		addListener(mPlayerManager);
		
		requestUpdate();
		
		MessageCommand cmd = new MessageCommand();
		Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeChat", cmd);
		
		getCommand("tell").setExecutor(cmd);
		getCommand("tell").setTabCompleter(cmd);
		getCommand("reply").setExecutor(cmd);
		getCommand("reply").setTabCompleter(cmd);
		getCommand("msgtoggle").setExecutor(cmd);
		getCommand("socialspy").setExecutor(mSocialSpyHandler);
		
		NicknameCommand nickname = new NicknameCommand();
		getCommand("nickname").setExecutor(nickname);
		getCommand("nickname").setTabCompleter(nickname);
		
		MuteHandler mute = new MuteHandler(this);
		getCommand("mute").setExecutor(mute);
		getCommand("mute").setTabCompleter(mute);
		getCommand("unmute").setExecutor(mute);
		getCommand("unmute").setTabCompleter(mute);
		
		RealnameCommand realname = new RealnameCommand();
		getCommand("realname").setExecutor(realname);
		getCommand("realname").setTabCompleter(realname);
		
		mAfkHandler = new AFKHandler(this);
		getCommand("afk").setExecutor(mAfkHandler);
		getCommand("afk").setTabCompleter(mAfkHandler);
	}
	
	@Override
	public void onDisable()
	{
		mChatChannels.unregisterAll();
	}
	
	private void requestUpdate()
	{
		if(Bukkit.getOnlinePlayers().length == 0)
			return;
		
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
		
		mHasRequestedUpdate = true;
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	private void onPlayerJoin(PlayerJoinEvent event)
	{
		if(!mHasRequestedUpdate || !mHasUpdated)
		{
			Bukkit.getScheduler().runTaskLater(this, new Runnable()
			{
				@Override
				public void run()
				{
					requestUpdate();
				}
			}, 2);
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	private void onConfigUpdate(ConfigReceiveEvent event)
	{
		if(event.getName().equals("bungeechat"))
		{
			mHasRequestedUpdate = true;
			mHasUpdated = true;
			SyncConfig config = event.getConfig();
			Formatter.load(config);
			mChatChannels.load(config);
			mSocialSpyHandler.load(config);
			mAfkHandler.load(config);
		}
	}

	public static void sendMessage(RemotePlayer player, String message)
	{
		new MessageOutput("BungeeChat", "Send")
			.writeUTF(player.getName())
			.writeUTF(message)
			.send(mInstance);
	}
	
	public static void mirrorChat(String fullChat, String channel)
	{
		new MessageOutput("BungeeChat", "Mirror")
			.writeUTF(channel)
			.writeUTF(fullChat)
			.send(mInstance);
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

	@Override
	public void onPluginMessageReceived( String channel, Player sender, byte[] data )
	{
		ByteArrayInputStream stream = new ByteArrayInputStream(data);
		DataInputStream input = new DataInputStream(stream);
		try
		{
			String subChannel = input.readUTF();
			if(channel.equals("BungeeChat"))
			{
				if(subChannel.equals("Mirror"))
				{
					String chatChannel = input.readUTF();
					String message = input.readUTF();
					
					ChannelType type = ChannelType.from(chatChannel);
					
					Bukkit.getPluginManager().callEvent(new ChatChannelEvent(chatChannel, type, message));
				}
				else if(subChannel.equals("Message"))
				{
					Player ply = Bukkit.getPlayerExact(input.readUTF());
					String message = input.readUTF();
					if(ply != null)
						ply.sendMessage(message);
				}
				
				for(IDataReceiver receiver : mReceivers)
					receiver.onMessage(subChannel, input);
			}
		}
		catch(IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	public void addListener(IDataReceiver receiver)
	{
		mReceivers.add(receiver);
	}
	
	public static String colorize(String message, CommandSender sender)
	{
		int pos = -1;
		char colorChar = '&';
		
		StringBuffer buffer = new StringBuffer(message);
		
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
			getPlayerManager().getPlayerSettings(sender).lastMsgTarget = target.getName();
			getPlayerManager().updatePlayerSettings(sender);
		}
		else if(sender instanceof RemotePlayer)
		{
			new MessageOutput("BungeeChat", "MsgTarget")
				.writeUTF(sender.getName())
				.writeUTF(target.getName())
				.send(mInstance);
		}
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
	
	static BungeeChat getInstance()
	{
		return mInstance;
	}
}
