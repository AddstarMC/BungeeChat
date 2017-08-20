package au.com.addstar.bc;

import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;

import au.com.addstar.bc.sync.packet.PlayerSettingsPacket;
import net.cubespace.Yamler.Config.Config;
import net.cubespace.Yamler.Config.YamlConfig;

public class PlayerSettings extends YamlConfig
{
	public PlayerSettings(File file)
	{
		CONFIG_FILE = file;
	}
	
	public int socialSpyState = 2;
	public boolean msgEnabled = true;
	
	public String nickname = "";
	
	public String skin = null;
	
	@NoSave
	public UUID lastMsgTarget = null;
	
	public long muteTime = 0;
	
	@NoSave
	public String tabColor = "";
	@NoSave
	public boolean isAFK = false;

	@NoSave
	public String rolePlayPrefix = "";

	@NoSave
	public String defaultChannel = "";

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
		rolePlayPrefix = packet.getRPprefix();
		defaultChannel = packet.getDefaultChannel();
	}
	
	public PlayerSettingsPacket getUpdatePacket(UUID id)
	{
		return new PlayerSettingsPacket(id, nickname, lastMsgTarget, socialSpyState, msgEnabled, muteTime, isAFK, rolePlayPrefix,defaultChannel);
	}
}
