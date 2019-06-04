package au.com.addstar.bc.sync;

import au.com.addstar.bc.BungeeChat;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 4/06/2019.
 */
public class ProxyComLink extends ServerComLink {

    private HashMap<RegisteredServer, MessageSender> mRevServers;
    private HashMap<Integer,BukkitMessageSender> mServers;
    private ProxyServer server;

    public final ScheduledTask getTask() {
        return task;
    }

    private ScheduledTask task;

    public ProxyComLink(ProxyServer server)
    {
        this.server = server;
        mServers = new HashMap<>();
        mRevServers = new HashMap<>();
        for(RegisteredServer registeredServer : server.getAllServers())
        {
            int id = registeredServer.getServerInfo().getAddress().getPort();
            BukkitMessageSender sender = new BukkitMessageSender(id, registeredServer.getServerInfo().getName());
            mServers.put(id, sender);
            mRevServers.put(registeredServer, sender);
        }
    }

    @Override
    protected void initializeQueueHandler(BlockingQueue<Map.Entry<String, byte[]>> queue) {
        task = server.getScheduler().buildTask(BungeeChat.getInstance(),new DataSender(queue)).schedule();

    }

    @Override
    public Future<Void> listenToChannel(String channel, IDataReceiver receiver) {
        return null;
    }

    @Override
    protected MessageSender getSender(int id) {
        return mServers.get(id);
    }

    @Override
    public int getServerId() {
        return 0;
    }
    private class DataSender implements Runnable
    {
        private BlockingQueue<Map.Entry<String, byte[]>> mQueue;
        public DataSender(BlockingQueue<Map.Entry<String, byte[]>> queue)
        {
            mQueue = queue;
        }

        @Override
        public void run()
        {
            try
            {
                while(true)
                {
                    Map.Entry<String, byte[]> item = mQueue.take();
                    publish(item.getKey(), item.getValue());
                }
            }
            catch(InterruptedException ignored)
            {
            }
        }
    }
    private static class BukkitMessageSender implements MessageSender
    {
        private final int mId;
        private final String mName;

        public BukkitMessageSender(int id, String name)
        {
            mId = id;
            mName = name;
        }

        @Override
        public int getId()
        {
            return mId;
        }

        @Override
        public String getName()
        {
            return mName;
        }

    }
}
