package au.com.addstar.bc.sync;

import com.velocitypowered.api.proxy.server.RegisteredServer;

/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 6/06/2019.
 */
public interface IPacketHandler
{
    void handle(Packet packet, RegisteredServer sender);
}
