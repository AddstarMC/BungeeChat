package au.com.addstar.bc.sync;

import net.md_5.bungee.api.config.ServerInfo;

public interface IPacketHandler
{
	public void handle(Packet packet, ServerInfo sender);
}
