/*
 * BungeeChat
 *
 * Copyright (c) 2015 - 2020.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy   of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is *
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR  IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package au.com.addstar.bc.sync;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

import au.com.addstar.bc.BungeeChat;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

public class ProxyComLink extends ServerComLink
{
	private HashMap<ServerInfo, MessageSender> mRevServers;
	private HashMap<Integer, BukkitMessageSender> mServers;
	
	public ProxyComLink()
	{
		mServers = new HashMap<>();
		mRevServers = new HashMap<>();
		for(ServerInfo server : ProxyServer.getInstance().getServersCopy().values())
		{
			int id = server.getAddress().getPort();
			BukkitMessageSender sender = new BukkitMessageSender(id, server.getName()); 
			mServers.put(id, sender);
			mRevServers.put(server, sender);
		}
	}
	
	@Override
	protected void initializeQueueHandler( BlockingQueue<Entry<String, byte[]>> queue )
	{
		ProxyServer.getInstance().getScheduler().runAsync(BungeeChat.instance, new DataSender(queue));
	}

	@Override
	protected MessageSender getSender( int id )
	{
		return mServers.get(id);
	}

	@Override
	public int getServerId()
	{
		return 0;
	}
	
	@Override
	public Future<Void> listenToChannel( final String channel, final IDataReceiver receiver )
	{
		final SubscribeFuture future = new SubscribeFuture();
		ProxyServer.getInstance().getScheduler().runAsync(BungeeChat.instance, () -> subscribeChannel(channel, receiver, future));
		return future;
	}
	
	public void sendMessage( String channel, byte[] data, ServerInfo target )
	{
		sendMessage(channel, data, mRevServers.get(target));
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
