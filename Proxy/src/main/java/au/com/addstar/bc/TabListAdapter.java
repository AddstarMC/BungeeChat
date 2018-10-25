package au.com.addstar.bc;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.tab.TabListHandler;
import net.md_5.bungee.protocol.packet.PlayerListHeaderFooter;

/**
 * Created for the AddstarMC Project. Created by Narimm on 25/10/2018.
 */
public abstract class TabListAdapter implements TabListHandler {
    
    
    private final ProxiedPlayer player;
    
    protected TabListAdapter(ProxiedPlayer player) {this.player = player;}
    
    @Override
    public void onConnect() {
    }
    
    @Override
    public void onServerChange() {
    }
    
    @Override
    public void onPingChange(int ping) {
    }
    
    @Override
    public void onDisconnect() {
    }
    
    @Override
    public void onUpdate(PlayerListHeaderFooter packet) {
    }
    
    @Override
    public void onUpdateName() {
    }
    
    public ProxiedPlayer getPlayer() {
        return player;
    }
}