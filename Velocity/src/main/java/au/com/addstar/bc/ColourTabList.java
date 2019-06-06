package au.com.addstar.bc;

import com.google.common.collect.Sets;

import au.com.addstar.bc.events.PropertyChangeEvent;
import au.com.addstar.bc.sync.SyncManager;

import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;

import net.kyori.text.Component;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 5/06/2019.
 */
public class ColourTabList {
    private static final int PING_THRESHOLD = 20;
    private static ListUpdater mUpdater = new ListUpdater();
    private static Set<ColourTabList> mTabLists = Sets.newConcurrentHashSet();
    private int lastPing;
    private Set<Player> mVisiblePlayers = Sets.newConcurrentHashSet();
    private String mHeaderContents;
    private String mFooterContents;
    private boolean mHasInited;
    private SkinData mForcedSkinData;
    // ==== 1.7 compat ====
    private String mLastName;
    private Player player;
    private static BungeeChat plugin;

    public ColourTabList(Player player) {
        this.player = player;
        synchronized(mTabLists)
        {
            mTabLists.add(this);
        }
        player.getTabList().getEntries().iterator().next().
    }

    public static void initialize(BungeeChat plugin)
    {
        ColourTabList.plugin = plugin;
        plugin.getServer().getEventManager().register(plugin,PropertyChangeEvent.class, mUpdater);
    }

    public static class ListUpdater implements EventHandler<PropertyChangeEvent>
    {
        @Subscribe
        public void execute(PropertyChangeEvent event)
        {
            if(!event.getProperty().startsWith("TL:"))
                return;

            updateAll();
        }
    }

    public static void updateAll()
    {
        synchronized(mTabLists)
        {
            for(ColourTabList list : mTabLists)
                list.updateList();
        }
    }
    public void updateList()
    {
        ArrayList<TabListEntry> toAdd = new ArrayList<>();
        ArrayList<TabListEntry> toRemove = new ArrayList<>();
        if(player == null){

        }
        for (Player p : plugin.getServer().getAllPlayers())
        {
            if(isVisible(player, p))
            {
                if(!mVisiblePlayers.contains(p))
                {
                    toAdd.add(createItem(p));
                    mVisiblePlayers.add(p);
                }
            }
            else
            {
                mVisiblePlayers.remove(p);
                toRemove.add(createItem(p));
            }
        }

        if (isNewTab(player))
        {
            if (!toAdd.isEmpty())
            {
                player.getTabList().addEntry(player.getTabList().buildEntry());
                PlayerListItem packetAdd = createPacket(Action.ADD_PLAYER, toAdd.toArray(new Item[0]));
                PlayerListItem packetUpdate = createPacket(Action.UPDATE_DISPLAY_NAME, toAdd.toArray(new Item[0]));

                sendPacket(packetAdd, getPlayer());
                sendPacket(packetUpdate, getPlayer());
            }

            if (!toRemove.isEmpty())
            {
                PlayerListItem packetRemove = createPacket(Action.REMOVE_PLAYER, toRemove.toArray(new Item[0]));
                sendPacket(packetRemove, getPlayer());
            }
        }
        else
        {
            for (Item item : toAdd)
            {
                PlayerListItem packet = createPacket(Action.ADD_PLAYER, item);
                sendPacket(packet, getPlayer());
            }

            for (Item item : toRemove)
            {
                PlayerListItem packet = createPacket(Action.REMOVE_PLAYER, item);
                sendPacket(packet, player);
            }
        }

        updateTabHeaders();
    }

    private void sendPacket(PlayerListItem packet, Player player) {

    }

    private TabListEntry createItem(Player p, TabList t) {
        TabListEntry.Builder builder = TabListEntry.builder();
        builder.profile(p.getGameProfile()).tabList(t).

    }

    public static boolean isVisible(Player to, Player player) {
        if (to == player)
            return true;

        if (player.getTabList() instanceof ColourTabList) {
            if (!((ColourTabList) player.getTabList()).mHasInited)
                return false;
        }

        SyncManager manager = plugin.getSyncManager();

        boolean canSeeAll = manager.getPropertyBoolean(to, "TL:seeall", false);
        if (canSeeAll){
            Debugger.logt(player.getGameProfile().getName() + " visible because " + to.getGameProfile().getName() + " can SEEALL...");
            return true;
        }
        Collection<String> names = manager.getPropertyNames(player, "TL:group:");

        if(names.isEmpty()) {
            Debugger.logt(player.getGameProfile().getName() + " visible to "+to.getGameProfile().getName()+" because TL group is empty");

            return true;
        }

        for(String name : names)
        {
            String group = name.split(":")[2];
            if(manager.getPropertyBoolean(player, "TL:group:" + group, false) && !manager.getPropertyBoolean(to, "TL:see:" + group, false)){
                Debugger.logt(player.getGameProfile().getName() + " hidden to "+to.getGameProfile().getName()+" because they cant see "+ group);
                return false;
            }
        }
        Debugger.logt(player.getGameProfile().getName() + " visible to "+to.getGameProfile().getName()+" because no group hides the player ");
        return true;
    }

    private static PlayerListItem createPacket(Action action, Item... items)
    {
        PlayerListItem packet = new PlayerListItem();
        packet.setAction(action);
        packet.setItems(items);
        return packet;
    }
}
