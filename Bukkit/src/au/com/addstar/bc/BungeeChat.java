package au.com.addstar.bc;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

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
	}
	
	@Override
	public void onDisable()
	{
		mChatChannels.unregisterAll();
	}
	
	private void requestUpdate()
	{
		Player player = null;
		
		if(Bukkit.getOnlinePlayers().length > 0)
			player = Bukkit.getOnlinePlayers()[0];
		else
			return;
		
		new MessageOutput("BungeeChat", "Update").send(player, mInstance);
		
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
	
	private void update(DataInputStream input) throws IOException
	{
		mHasRequestedUpdate = true;
		mHasUpdated = true;
		
		serverName = input.readUTF();
		String consoleName = input.readUTF();
		if(consoleName.isEmpty())
			Formatter.consoleOverride = null;
		else
			Formatter.consoleOverride = ChatColor.translateAlternateColorCodes('&', consoleName);
		
		Formatter.mPMFormatInbound = ChatColor.translateAlternateColorCodes('&', input.readUTF());
		Formatter.mPMFormatOutbound = ChatColor.translateAlternateColorCodes('&', input.readUTF());
		
		Formatter.permissionLevels.clear();
		
		int count = input.readShort();
		for(int i = 0; i < count; ++i)
			Formatter.permissionLevels.add(new PermissionSetting(input.readUTF(), input.readShort(), input.readUTF(), input.readUTF()));
		
		Collections.sort(Formatter.permissionLevels);
		
		mChatChannels.unregisterAll();
		
		count = input.readShort();
		for(int i = 0; i < count; ++i)
			mChatChannels.register(input.readUTF(), input.readUTF(), input.readUTF(), input.readUTF(), input.readUTF());
		
		Formatter.keywordsEnabled = input.readBoolean();
		if(Formatter.keywordsEnabled)
		{
			Formatter.keywordPerm = input.readUTF();
			Formatter.keywordEnabledChannels.clear();
			Formatter.keywordPatterns.clear();
			
			count = input.readShort();
			for(int i = 0; i < count; ++i)
				Formatter.keywordEnabledChannels.add(input.readUTF());
			
			count = input.readShort();
			for(int i = 0; i < count; ++i)
			{
				try
				{
					Pattern pattern = Pattern.compile(input.readUTF(), Pattern.CASE_INSENSITIVE);
					Formatter.keywordPatterns.put(pattern, input.readUTF());
				}
				catch (PatternSyntaxException e)
				{
					// Cant happen
				}
			}
			
			try
			{
				Bukkit.getPluginManager().addPermission(new org.bukkit.permissions.Permission(Formatter.keywordPerm, PermissionDefault.OP));
			}
			catch(IllegalArgumentException e)
			{
			}
		}
		
		mSocialSpyHandler.clearKeywords();
		count = input.readShort();
		for(int i = 0; i < count; ++i)
			mSocialSpyHandler.addKeyword(input.readUTF());
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
				else if(subChannel.equals("Update"))
				{
					update(input);
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
	
	static BungeeChat getInstance()
	{
		return mInstance;
	}
}
