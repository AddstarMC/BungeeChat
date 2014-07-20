package au.com.addstar.bc.sync.packet;

import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketSchema;
import au.com.addstar.bc.sync.SyncConfig;

public class ConfigPacket extends Packet
{
	public static final PacketSchema schema = PacketSchema.from("name=String,config=SyncConfig");
	
	public ConfigPacket(String name, SyncConfig config)
	{
		super(name, config);
	}
	
	public String getName()
	{
		return (String)getData(0);
	}
	
	public SyncConfig getConfig()
	{
		return (SyncConfig)getData(1);
	}
	
}
