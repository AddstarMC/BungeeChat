package au.com.addstar.bc.sync;

import net.md_5.bungee.api.config.ServerInfo;

public interface SyncMethod
{
	Object run(String name, ServerInfo server, Object... arguments);
}
