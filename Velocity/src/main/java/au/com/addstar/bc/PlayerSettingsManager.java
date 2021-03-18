package au.com.addstar.bc;

import com.velocitypowered.api.proxy.Player;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 6/06/2019.
 */
public class PlayerSettingsManager
{
    private WeakHashMap<Player, PlayerSettings> mLoadedSettings = new WeakHashMap<>();
    private Path mFolder;
    private ProxyChat plugin;
    private YamlConfigurationLoader.Builder builder;

    public PlayerSettingsManager(ProxyChat plugin)
    {
        this.plugin = plugin;
        builder = YamlConfigurationLoader.builder().indent(2).nodeStyle(NodeStyle.BLOCK);
        mFolder = plugin.dataFolder.resolve("players");
        if(!mFolder.toFile().exists()) {
            mFolder.toFile().mkdirs();
        }
    }
    @Nullable
    public PlayerSettings getSettings(UUID player)
    {
        return getSettings(plugin.getServer().getPlayer(player).orElse(null));
    }

    public PlayerSettings getSettings(Player player)
    {
        if(mLoadedSettings.containsKey(player))
            return mLoadedSettings.get(player);
        YamlConfigurationLoader loader  = builder.path(mFolder.resolve(player.getUniqueId().toString() + ".yml")).build();
        PlayerSettings settings = loader.load().get(PlayerSettings.class,new PlayerSettings());
        PlayerSettings settings = new PlayerSettings(new File(mFolder, ));

        try
        {
            settings.init();
        }
        catch(InvalidConfigurationException e)
        {
            System.err.println("Could not load player data for " + player.getUsername());
            e.printStackTrace();
        }

        mLoadedSettings.put(player, settings);
        Debugger.log("Loaded player settings %s", player.getUsername());
        return settings;
    }

    public void savePlayer(UUID player)
    {
        savePlayer(plugin.getServer().getPlayer(player).orElse(null));
    }

    public void savePlayer(Player player)
    {
        try
        {
            getSettings(player).save();
            Debugger.log("Saved player settings %s", player.getUsername());
        }
        catch(InvalidConfigurationException ignored)
        {
        }
    }

    public void updateSettings(UUID player)
    {
        updateSettings(plugin.getServer().getPlayer(player).orElse(null));
    }

    public void updateSettings(Player player)
    {
        PlayerSettings settings = getSettings(player);
        plugin.getPacketManager().send(settings.getUpdatePacket(player.getUniqueId()), player.getCurrentServer().orElse(null).getServer());
        Debugger.log("Sending player settings update %s", player.getUsername());
    }

    public void unloadPlayer( UUID id )
    {
        Player player = plugin.getServer().getPlayer(id).get();
        mLoadedSettings.remove(player);
        plugin.getSubHandler().unSubscribe(id);
        Debugger.log("Unloaded player settings %s", player.getUsername());
        plugin.getSubHandler().unSubscribe(id);
    }
}

