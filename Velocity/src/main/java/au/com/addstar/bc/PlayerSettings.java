package au.com.addstar.bc;

import au.com.addstar.bc.sync.packet.PlayerSettingsPacket;

import net.kyori.text.Component;
import net.kyori.text.format.TextFormat;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 6/06/2019.
 */
@ConfigSerializable
public class PlayerSettings {

    public int socialSpyState = 2;

    public boolean msgEnabled = true;

    public Component nickname = Component.empty();

    public String skin = null;

    public transient UUID lastMsgTarget = null;

    public long muteTime = 0;

    public transient List<TextFormat> tabColor = new ArrayList<>();

    public transient boolean isAFK = false;

    public Component chatName = Component.empty();
    public transient String defaultChannel = "";

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
