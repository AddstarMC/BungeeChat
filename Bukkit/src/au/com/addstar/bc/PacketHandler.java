package au.com.addstar.bc;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import au.com.addstar.bc.event.ChatChannelEvent;
import au.com.addstar.bc.sync.IPacketHandler;
import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.packet.MirrorPacket;
import au.com.addstar.bc.sync.packet.SendPacket;
import au.com.addstar.bc.sync.packet.UpdateNamePacket;

public class PacketHandler implements IPacketHandler
{

	@Override
	public void handle( Packet packet )
	{
		if(packet instanceof MirrorPacket)
			handleMirror((MirrorPacket)packet);
		else if(packet instanceof SendPacket)
			handleSend((SendPacket)packet);
		else if(packet instanceof UpdateNamePacket)
			handleUpdateName((UpdateNamePacket)packet);
	}
	
	private void handleMirror(MirrorPacket packet)
	{
		ChannelType type = ChannelType.from(packet.getChannel());
		Bukkit.getPluginManager().callEvent(new ChatChannelEvent(packet.getChannel(), type, packet.getMessage()));
	}
	
	private void handleSend(SendPacket packet)
	{
		Player player = Bukkit.getPlayer(packet.getUUID());
		if(player != null)
			player.sendMessage(packet.getMessage());
	}
	
	private void handleUpdateName(UpdateNamePacket packet)
	{
		BungeeChat.getPlayerManager().onPlayerNameChange(packet.getID(), packet.getName());
	}
	
	
}
