package au.com.addstar.bc.config;

import au.com.addstar.bc.sync.SyncConfig;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

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
@ConfigSerializable
public class Config
{

    @Setting(comment = "You can override the name of the console here. Leave blank for no change")
    public String consoleName = "";

    @Setting(value = "PM-format-in")
    public String pmFormatIn = "[{DISPLAYNAME}&r -> Me]: {MESSAGE}";
    @Setting(value = "PM-format-out")
    public String pmFormatOut = "[Me -> {DISPLAYNAME}&r]: {MESSAGE}";

    @Setting(comment = "Here you can set up permission based formats")
    public Map<String, PermissionSetting> permSettings;

    @Setting(comment = "Channels allow you to set up layers of chat based on permissions")
    public Map<String, ChatChannel> channels;

    @Setting(comment = "Keyword highlighter allows you to show some words as highlighted to some users")
    public KeywordHighlighterSettings keywordHighlighter;

    public List<String> socialSpyKeywords;

    @Setting(comment = "List of commands to be included in mute")
    public List<String> mutedCommands;

    @Setting(comment = "The time in seconds of no activity that someone is considered afk.", value = "afk-delay")
    public int afkDelay = 30;

    @Setting(value = "afk-kick-enabled")
    public boolean afkKickEnabled = false;

    @Setting(value = "debug")
    public boolean debug = false;

    @Setting(comment = "The time in minutes of being afk that someone is kicked", value = "afk-kick-delay")
    public int afkKickDelay = 30;

    @Setting(comment = "afk-kick-message")
    public String afkKickMessage = "You have been kicked for idling more than %d minutes.";

    @Setting(comment = "Settings for redis so that")
    public RedisSettings redis = new RedisSettings();

    public Config(File file)
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
