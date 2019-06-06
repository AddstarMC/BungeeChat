package au.com.addstar.bc.events;

import com.velocitypowered.api.proxy.Player;

/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 6/06/2019.
 */
public class PropertyChangeEvent {
    private Player mPlayer;
    private String mProperty;
    private Object mOldValue;
    private Object mNewValue;
    public PropertyChangeEvent(Player player, String property, Object oldVal, Object newVal)
    {
        mPlayer = player;
        mProperty = property;
        mOldValue = oldVal;
        mNewValue = newVal;
    }

    public Player getPlayer()
    {
        return mPlayer;
    }

    public String getProperty()
    {
        return mProperty;
    }

    public Object getOldValue()
    {
        return mOldValue;
    }

    public Object getNewValue()
    {
        return mNewValue;
    }
}
