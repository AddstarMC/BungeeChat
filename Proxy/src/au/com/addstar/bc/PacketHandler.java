package au.com.addstar.bc;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import au.com.addstar.bc.event.BCChatEvent;
import au.com.addstar.bc.sync.IPacketHandler;
import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketManager;
import au.com.addstar.bc.sync.packet.GlobalMutePacket;
import au.com.addstar.bc.sync.packet.MirrorPacket;
import au.com.addstar.bc.sync.packet.PlayerListRequestPacket;
import au.com.addstar.bc.sync.packet.PlayerSettingsPacket;
import au.com.addstar.bc.sync.packet.UpdateNamePacket;

public class PacketHandler implements IPacketHandler
{
	private PacketManager getPacketManager()
	{
		return BungeeChat.instance.getPacketManager();
	}
	
	private PlayerSettingsManager getManager()
	{
		return BungeeChat.instance.getManager();
	}
	
	@Override
	public void handle( Packet packet, ServerInfo sender )
	{
		if(packet instanceof MirrorPacket)
			handleMirror((MirrorPacket)packet, sender);
		else if(packet instanceof PlayerSettingsPacket)
			handlePlayerSettings((PlayerSettingsPacket)packet);
		else if(packet instanceof UpdateNamePacket)
			handleUpdateName((UpdateNamePacket)packet);
		else if(packet instanceof GlobalMutePacket)
			handleGMute((GlobalMutePacket)packet);
		else if(packet instanceof PlayerListRequestPacket)
			handlePlayerListRequest((PlayerListRequestPacket)packet, sender);
	}
	
	private void handleMirror(MirrorPacket packet, ServerInfo sender)
	{
		ProxyServer.getInstance().getPluginManager().callEvent(new BCChatEvent(packet.getChannel(), packet.getMessage()));
		if(!packet.getChannel().startsWith("~"))
			ProxyServer.getInstance().getConsole().sendMessage(TextComponent.fromLegacyText(packet.getMessage()));
	}
	
	private void handlePlayerSettings(PlayerSettingsPacket packet)
	{
		ProxiedPlayer player = ProxyServer.getInstance().getPlayer(packet.getID());
		if (player == null)
		{
			BungeeChat.instance.getLogger().severe("!!!!! A request to update settings for offline player " + packet.getID() + " was made. This is an error !!!!");
			return;
		}
		
		Debugger.log("Updating player settings %s", player.getName());
		PlayerSettings settings = getManager().getSettings(player);
		settings.read(packet);
		getManager().savePlayer(player);
		
		String oldName = settings.nickname;
		if(settings.nickname.isEmpty())
			player.setDisplayName(player.getName());
		else
			player.setDisplayName(settings.nickname);
		
		if(!oldName.equals(settings.nickname))
		{
			Debugger.log("Updating player name %s to '%s'", player.getName(), settings.nickname);
			getPacketManager().broadcast(new UpdateNamePacket(packet.getID(), settings.nickname));
		}
	}
	
	private void handleUpdateName(UpdateNamePacket packet)
	{
		ProxiedPlayer player = ProxyServer.getInstance().getPlayer(packet.getID());
		PlayerSettings settings = getManager().getSettings(player);
		settings.nickname = packet.getName();
		
		getManager().savePlayer(player);
		
		String oldName = settings.nickname;
		if(settings.nickname.isEmpty())
			player.setDisplayName(player.getName());
		else
			player.setDisplayName(settings.nickname);
		
		if(!oldName.equals(settings.nickname))
		{
			Debugger.log("Updating player name %s to '%s'", player.getName(), settings.nickname);
			getPacketManager().broadcast(new UpdateNamePacket(packet.getID(), settings.nickname));
		}
	}
	
	private void handleGMute(GlobalMutePacket packet)
	{
		BungeeChat.instance.getMuteHandler().setGMute(packet.getTime());
	}
	
	private void handlePlayerListRequest( PlayerListRequestPacket packet, ServerInfo sender )
	{
		Debugger.log("Server %s requested player list", sender.getName());
		BungeeChat.instance.sendPlayerUpdates(sender);
	}
}
