package au.com.addstar.bc.events;

import net.kyori.adventure.text.Component;

/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 6/06/2019.
 */
public class ChatEvent{
    private String mChannel;
    private Component mMessage;

    public ChatEvent(String channel, Component message)
    {
        mChannel = channel;
        mMessage = message;
    }

    public String getChannel()
    {
        return mChannel;
    }

    public Component getMessage()
    {
        return mMessage;
    }
}
