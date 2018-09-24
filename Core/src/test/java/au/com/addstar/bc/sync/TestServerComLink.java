package au.com.addstar.bc.sync;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * Created for the AddstarMC Project. Created by Narimm on 3/09/2018.
 */
public class TestServerComLink extends ServerComLink {
    @Override
    protected void initializeQueueHandler(BlockingQueue<Map.Entry<String, byte[]>> queue) {
        
        DataSender sender = new DataSender(queue);
        Thread t = new Thread(sender);
        t.start();
    }
    
    @Override
    public Future<Void> listenToChannel(String channel, IDataReceiver receiver) {
        return null;
    }
    
    @Override
    protected MessageSender getSender(int id) {
        return new TestMessageSender();
    }
    
    @Override
    public int getServerId() {
        return 0;
    }
    
    private class DataSender implements Runnable{
    
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
}
