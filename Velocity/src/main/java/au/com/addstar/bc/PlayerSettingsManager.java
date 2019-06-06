package au.com.addstar.bc;

import com.velocitypowered.api.proxy.Player;

import net.cubespace.Yamler.Config.InvalidConfigurationException;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 6/06/2019.
 */
public class PlayerSettingsManager
{
    private WeakHashMap<Player, PlayerSettings> mLoadedSettings = new WeakHashMap<>();
    private File mFolder;
    private BungeeChat plugin;

    public PlayerSettingsManager(File folder,BungeeChat plugin)
    {
        this.plugin = plugin;
        mFolder = folder;
        mFolder.mkdirs();
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

        PlayerSettings settings = new PlayerSettings(new File(mFolder, player.getUniqueId().toString() + ".yml"));

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

