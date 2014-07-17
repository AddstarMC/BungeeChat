package au.com.addstar.bc;

import java.io.DataInput;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.command.CommandSender;

public class PlayerSettings
{
	public int socialSpyState = 2;
	public boolean msgEnabled = true;
	public UUID lastMsgTarget = null;
	
	public String nickname = "";
	
	public long muteTime = 0;
	
	public String tabFormat = "";
	
	public long lastActiveTime = Long.MAX_VALUE;
	public long afkStartTime = Long.MAX_VALUE;
	
	public boolean isAFK = false;
	
	public CommandSender getLastMsgTarget()
	{
		return BungeeChat.getPlayerManager().getPlayer(lastMsgTarget);
	}
	
	public void read(DataInput input)
	{
		try
		{
			lastMsgTarget = UUID.fromString(input.readUTF());
			nickname = input.readUTF();
			socialSpyState = input.readByte();
			msgEnabled = input.readBoolean();
			muteTime = input.readLong();
			isAFK = input.readBoolean();
		}
		catch(IOException e)
		{
		}
	}
}
