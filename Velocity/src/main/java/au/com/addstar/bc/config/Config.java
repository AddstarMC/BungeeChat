package au.com.addstar.bc.config;

import au.com.addstar.bc.sync.SyncConfig;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 4/06/2019.
 */
public class Config
{

    @Comment("You can override the name of the console here. Leave blank for no change")
    public String consoleName = "";

    @Setting("PM-format-in")
    public String pmFormatIn = "[{DISPLAYNAME}&r -> Me]: {MESSAGE}";
    @Setting("PM-format-out")
    public String pmFormatOut = "[Me -> {DISPLAYNAME}&r]: {MESSAGE}";

    @Comment("Here you can set up permission based formats")
    public Map<String, PermissionSetting> permSettings;

    @Comment("Channels allow you to set up layers of chat based on permissions")
    public Map<String, ChatChannel> channels;

    @Comment("Keyword highlighter allows you to show some words as highlighted to some users")
    public KeywordHighlighterSettings keywordHighlighter;

    public List<String> socialSpyKeywords;

    @Comment("List of commands to be included in mute")
    public List<String> mutedCommands;

    @Comment("The time in seconds of no activity that someone is considered afk.")
    @Setting("afk-delay")
    public int afkDelay = 30;

    @Setting("afk-kick-enabled")
    public boolean afkKickEnabled = false;

    @Setting("debug")
    public boolean debug = false;

    @Comment("The time in minutes of being afk that someone is kicked")
    @Setting("afk-kick-delay")
    public int afkKickDelay = 30;

    @Setting("afk-kick-message")
    public String afkKickMessage = "You have been kicked for idling more than %d minutes.";

    @Comment("Settings for redis so that")
    public RedisSettings redis = new RedisSettings();

    public Config()
    {
        permSettings = new LinkedHashMap<>();
        channels = new LinkedHashMap<>();

        permSettings.put("default", new PermissionSetting("<{DISPLAYNAME}>: {MESSAGE}", "f", 0));
        channels.put("BCast", new ChatChannel("bcast", "&6[&4Broadcast&6] &a{MESSAGE}", "bungeechat.broadcast", "*"));

        keywordHighlighter = new KeywordHighlighterSettings();

        socialSpyKeywords = new ArrayList<>(Arrays.asList("msg", "m", "w", "whisper", "t", "tell", "r", "reply"));
        mutedCommands = new ArrayList<>(Arrays.asList("msg", "m", "w", "whisper", "t", "tell", "r", "reply", "me", "afk"));
    }

    public SyncConfig toSyncConfig()
    {
        SyncConfig config = new SyncConfig();

        config.set("consolename", consoleName);

        config.set("pm-format-in", pmFormatIn);
        config.set("pm-format-out", pmFormatOut);

        config.set("afk-delay", afkDelay);
        config.set("afk-kick-enabled", afkKickEnabled);
        config.set("afk-kick-message", afkKickMessage);
        config.set("afk-kick-delay", afkKickDelay);
        config.set("debug", debug);

        config.set("socialspykeywords", socialSpyKeywords);
        config.set("mutedcommands", mutedCommands);

        config.set("highlight", keywordHighlighter);

        SyncConfig subChannels = config.createSection("channels");
        for(Map.Entry<String, ChatChannel> channel : channels.entrySet())
            subChannels.set(channel.getKey(), channel.getValue());

        SyncConfig permLevels = config.createSection("perms");
        for(Map.Entry<String, PermissionSetting> setting : permSettings.entrySet())
            permLevels.set(setting.getKey(), setting.getValue());

        return config;
    }
}
