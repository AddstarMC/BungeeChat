package au.com.addstar.bc;

import java.io.File;
import java.lang.reflect.Field;

import net.cubespace.Yamler.Config.Config;

public class PlayerSettings extends Config
{
	public PlayerSettings(File file)
	{
		CONFIG_FILE = file;
	}
	
	public int socialSpyState = 2;
	public boolean msgEnabled = true;
	
	@NoSave
	public String lastMsgTarget = null;
	
	@Override
	protected boolean doSkip( Field field )
	{
		return (field.getAnnotation(NoSave.class) != null);
	}
}
