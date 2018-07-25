package au.com.addstar.bc.sync;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SubscribeFuture implements Future<Void>
{
	private CountDownLatch mLatch;
	
	public SubscribeFuture()
	{
		mLatch = new CountDownLatch(1);
	}

	public void setDone()
	{
		mLatch.countDown();
	}
	
	@Override
	public boolean cancel( boolean mayInterruptIfRunning )
	{
		return false;
	}

	@Override
	public boolean isCancelled()
	{
		return false;
	}

	@Override
	public boolean isDone()
	{
		return mLatch.getCount() == 0;
	}

	@Override
	public Void get() throws InterruptedException, ExecutionException
	{
		mLatch.await();
		return null;
	}

	@Override
	public Void get(long timeout, @NotNull TimeUnit unit ) throws InterruptedException, ExecutionException, TimeoutException
	{
		if (!mLatch.await(timeout, unit))
			throw new TimeoutException();
		return null;
	}
	
	
}
