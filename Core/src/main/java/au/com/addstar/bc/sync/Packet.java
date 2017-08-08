package au.com.addstar.bc.sync;

import java.util.Arrays;

public class Packet
{
	private Object[] mData;
	
	protected Packet(Object... data)
	{
		mData = data;
	}
	
	public Object getData(int id)
	{
		return mData[id];
	}
	
	public Object[] getData()
	{
		return mData;
	}
	
	@Override
	public String toString()
	{
		return String.format("%s: %s", getClass().getSimpleName(), Arrays.toString(mData));
	}
}
