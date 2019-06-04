package au.com.addstar.bc.sync;

/*-
 * #%L
 * BungeeChat-Bukkit
 * %%
 * Copyright (C) 2015 - 2019 AddstarMC
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

import org.bukkit.Bukkit;

import au.com.addstar.bc.BungeeChat;
import org.bukkit.scheduler.BukkitTask;

public class BukkitComLink extends ServerComLink
{
	private HashMap<Integer, RemoteServer> mServers;
	private BukkitTask task;
	
	public BukkitComLink()
	{
		mServers = new HashMap<>();
	}
	
	@Override
	protected void initializeQueueHandler( BlockingQueue<Entry<String, byte[]>> queue )
	{
		task = Bukkit.getScheduler().runTaskAsynchronously(BungeeChat.getInstance(),
				new DataSender(queue));
	}
	
	@Override
	public Future<Void> listenToChannel( final String channel, final IDataReceiver receiver )
	{
		final SubscribeFuture future = new SubscribeFuture();
		Thread thread = new Thread(() -> subscribeChannel(channel, receiver, future));
		
		thread.start();
		
		return future;
	}

	@Override
	protected MessageSender getSender( int id )
	{
		if (id == 0)
			return null;

		RemoteServer server = mServers.computeIfAbsent(id, RemoteServer::new);

		return server;
	}
	@Override
	public void disable(){
		super.disable();
		Bukkit.getScheduler().cancelTask(task.getTaskId());
	}

	@Override
	public int getServerId()
	{
		return Bukkit.getPort();
	}

	public void sendMessage( String channel, byte[] data)
	{
		super.sendMessage(channel, data, null);
	}
	
	private static class RemoteServer implements MessageSender
	{
		private int mId;
		
		public RemoteServer(int id)
		{
			mId = id;
		}
		
		@Override
		public int getId()
		{
			return mId;
		}

		@Override
		public String getName()
		{
			return null;
		}
		
	}
	
	private class DataSender implements Runnable
	{
		private BlockingQueue<Entry<String, byte[]>> mQueue;
		public DataSender(BlockingQueue<Entry<String, byte[]>> queue)
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
					Entry<String, byte[]> item = mQueue.take();
					publish(item.getKey(), item.getValue());
				}
			}
			catch(InterruptedException ignored)
			{
			}
		}
	}
}
