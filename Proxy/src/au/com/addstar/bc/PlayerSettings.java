package au.com.addstar.bc;

import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.UUID;

import net.cubespace.Yamler.Config.Config;

public class PlayerSettings extends Config
{
	public PlayerSettings(File file)
	{
		CONFIG_FILE = file;
	}
	
	public int socialSpyState = 2;
	public boolean msgEnabled = true;
	
	public String nickname = "";
	
	@NoSave
	public UUID lastMsgTarget = null;
	
	public long muteTime = 0;
	
	@NoSave
	public String tabColor = "";
	@NoSave
	public boolean isAFK = false;
	
	@Override
	protected boolean doSkip( Field field )
	{
		return (field.getAnnotation(NoSave.class) != null);
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
		}
		catch(IOException e)
		{
		}
	}
}
