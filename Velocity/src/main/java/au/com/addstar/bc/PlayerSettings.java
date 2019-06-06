package au.com.addstar.bc;

import au.com.addstar.bc.sync.packet.PlayerSettingsPacket;

import net.cubespace.Yamler.Config.YamlConfig;
import net.kyori.text.format.TextFormat;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 6/06/2019.
 */
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
    public List<TextFormat> tabColor = new ArrayList<>();

    @NoSave
    public boolean isAFK = false;

    @NoSave
    public String chatName = "";

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
        chatName = packet.getChatName();
        defaultChannel = packet.getDefaultChannel();
    }

    public PlayerSettingsPacket getUpdatePacket(UUID id)
    {
        return new PlayerSettingsPacket(id, nickname, lastMsgTarget, socialSpyState, msgEnabled, muteTime, isAFK, chatName,defaultChannel);
    }
}
