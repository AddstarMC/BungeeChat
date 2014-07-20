package au.com.addstar.bc;

import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;

import au.com.addstar.bc.sync.packet.PlayerSettingsPacket;
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
	
	public void read(PlayerSettingsPacket packet)
	{
		lastMsgTarget = packet.getLastMessageTarget();
		nickname = packet.getNickname();
		socialSpyState = packet.getSocialSpyState();
		msgEnabled = packet.getMsgToggle();
		muteTime = packet.getMuteTime();
		isAFK = packet.getAFK();
	}
	
	public PlayerSettingsPacket getUpdatePacket(UUID id)
	{
		return new PlayerSettingsPacket(id, nickname, lastMsgTarget, socialSpyState, msgEnabled, muteTime, isAFK);
	}
}
