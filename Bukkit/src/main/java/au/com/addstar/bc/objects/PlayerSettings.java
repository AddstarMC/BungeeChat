package au.com.addstar.bc.objects;

import java.util.UUID;

import au.com.addstar.bc.BungeeChat;
import org.bukkit.command.CommandSender;

import au.com.addstar.bc.sync.packet.PlayerSettingsPacket;

public class PlayerSettings
{
	public int socialSpyState = 2;
	public boolean msgEnabled = true;
	public UUID lastMsgTarget = null;
	
	public String nickname = "";
	
	public long muteTime = 0;
	
	public String tabFormat = "";
	
	public long lastActiveTime = Long.MAX_VALUE;
	public long afkStartTime = 0;
	
	public boolean isAFK = false;

	public String rolePlayPrefix;
	
	public CommandSender getLastMsgTarget()
	{
		return BungeeChat.getPlayerManager().getPlayer(lastMsgTarget);
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
	}
	
	public PlayerSettingsPacket toPacket(UUID id)
	{
		return new PlayerSettingsPacket(id, nickname, lastMsgTarget, socialSpyState, msgEnabled, muteTime, isAFK, rolePlayPrefix);
	}
}
