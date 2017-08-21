package au.com.addstar.bc;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 21/08/2017.
 */
public class BungeeChatTest extends BungeeChat{

    private static BungeeChatTest mInstance;
    private ChatChannelManager mChatChannels;



    @Override
    public FileConfiguration getConfig() {
        return super.getConfig();
    }

    @Override
    public void onEnable() {
        mInstance = this;
        mChatChannels = mock(ChatChannelManager.class);

    }

    @Override
    public ChatChannelManager getChatChannelsManager() {
        return super.getChatChannelsManager();
    }

    public PluginCommand getCommand(String command){
        PluginCommand pcommand = mock(PluginCommand.class);
        return pcommand;
    }

    public static BungeeChatTest getInstance(){
        return mInstance;
    }



}
