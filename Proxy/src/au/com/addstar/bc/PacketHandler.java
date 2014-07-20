package au.com.addstar.bc;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import au.com.addstar.bc.sync.IPacketHandler;
import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketManager;
import au.com.addstar.bc.sync.packet.GlobalMutePacket;
import au.com.addstar.bc.sync.packet.MirrorPacket;
import au.com.addstar.bc.sync.packet.PlayerListRequestPacket;
import au.com.addstar.bc.sync.packet.PlayerSettingsPacket;
import au.com.addstar.bc.sync.packet.QuitMessagePacket;
import au.com.addstar.bc.sync.packet.SendPacket;
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
		else if(packet instanceof SendPacket)
			handleSend((SendPacket)packet);
		else if(packet instanceof PlayerSettingsPacket)
			handlePlayerSettings((PlayerSettingsPacket)packet);
		else if(packet instanceof UpdateNamePacket)
			handleUpdateName((UpdateNamePacket)packet);
		else if(packet instanceof GlobalMutePacket)
			handleGMute((GlobalMutePacket)packet);
		else if(packet instanceof QuitMessagePacket)
			handleQuitMessage((QuitMessagePacket)packet);
		else if(packet instanceof PlayerListRequestPacket)
			handlePlayerListRequest((PlayerListRequestPacket)packet, sender);
	}
	
	private void handleMirror(MirrorPacket packet, ServerInfo sender)
	{
		for(ServerInfo server : ProxyServer.getInstance().getServers().values())
		{
			if(!server.equals(sender) && !server.getPlayers().isEmpty())
				BungeeChat.instance.getPacketManager().send(packet, server);
		}
		
		if(!packet.getChannel().startsWith("~"))
			ProxyServer.getInstance().getConsole().sendMessage(TextComponent.fromLegacyText(packet.getMessage()));
	}
	
	private void handleSend(SendPacket packet)
	{
		ProxiedPlayer player = ProxyServer.getInstance().getPlayer(packet.getUUID()); 
		if(player != null)
			BungeeChat.instance.getPacketManager().send(packet, player.getServer().getInfo());
	}
	
	private void handlePlayerSettings(PlayerSettingsPacket packet)
	{
		ProxiedPlayer player = ProxyServer.getInstance().getPlayer(packet.getID());
		PlayerSettings settings = getManager().getSettings(player);
		settings.read(packet);
		getManager().savePlayer(player);
		
		String oldName = settings.nickname;
		if(settings.nickname.isEmpty())
			player.setDisplayName(player.getName());
		else
			player.setDisplayName(settings.nickname);
		
		ColourTabList.readdPlayer(player);
		
		if(!oldName.equals(settings.nickname))
			getPacketManager().broadcast(new UpdateNamePacket(packet.getID(), settings.nickname));
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
		
		ColourTabList.readdPlayer(player);
		
		if(!oldName.equals(settings.nickname))
			getPacketManager().broadcast(new UpdateNamePacket(packet.getID(), settings.nickname));
	}
	
	private void handleGMute(GlobalMutePacket packet)
	{
		BungeeChat.instance.setGlobalMute(packet.getTime());
		getPacketManager().broadcast(packet);
	}
	
	private void handleQuitMessage(QuitMessagePacket packet)
	{
		BungeeChat.instance.handleQuitMessage(packet);
	}
	
	private void handlePlayerListRequest( PlayerListRequestPacket packet, ServerInfo sender )
	{
		BungeeChat.instance.sendPlayerUpdates(sender);
	}
}
