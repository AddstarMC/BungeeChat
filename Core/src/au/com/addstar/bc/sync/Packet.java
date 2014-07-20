package au.com.addstar.bc.sync;

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
}
