package au.com.addstar.bc;

import java.io.DataInput;
import java.io.IOException;

import org.bukkit.command.CommandSender;

public class PlayerSettings
{
	public int socialSpyState = 2;
	public boolean msgEnabled = true;
	public String lastMsgTarget = null;
	
	public String nickname = "";
	
	public long muteTime = 0;
	
	public String tabFormat = "";
	
	public CommandSender getLastMsgTarget()
	{
		return BungeeChat.getPlayerManager().getPlayerExact(lastMsgTarget);
	}
	
	public void read(DataInput input)
	{
		try
		{
			lastMsgTarget = input.readUTF();
			nickname = input.readUTF();
			socialSpyState = input.readByte();
			msgEnabled = input.readBoolean();
			muteTime = input.readLong();
		}
		catch(IOException e)
		{
		}
	}
}
