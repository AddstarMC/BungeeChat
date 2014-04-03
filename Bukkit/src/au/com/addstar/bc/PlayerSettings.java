package au.com.addstar.bc;

import java.io.DataInput;
import java.io.IOException;

public class PlayerSettings
{
	public int socialSpyState = 2;
	public boolean msgEnabled = true;
	public String lastMsgTarget = null;
	
	public void read(DataInput input)
	{
		try
		{
			lastMsgTarget = input.readUTF();
			socialSpyState = input.readByte();
			msgEnabled = input.readBoolean();
		}
		catch(IOException e)
		{
		}
	}
}
