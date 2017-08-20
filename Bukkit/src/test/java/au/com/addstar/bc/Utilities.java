package au.com.addstar.bc;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 21/08/2017.
 */
public class Utilities {

    public Player createPlayer(){
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("TestPlayer");
        when(player.getName()).thenReturn("TestPlayerDisplayName");
        when(player.spigot()).thenReturn(null);
        return player;
    }

    public BungeeChat createPlugin(){
        BungeeChat plugin = mock(BungeeChat.class);

    }


    private class BungeeChatTest extends BungeeChat{


        @Override
        public FileConfiguration getConfig() {
            return super.getConfig();
        }

        @Override
        public void onEnable() {
            super.onEnable();
        }

        public PluginCommand getCommand(String command){
            PluginCommand pcommand = mock(PluginCommand.class);
            when(pcommand.setExecutor(CommandExecutor.))
            return pcommand;
        }


    }

}
