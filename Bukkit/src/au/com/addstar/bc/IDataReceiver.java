package au.com.addstar.bc;

import java.io.DataInput;
import java.io.IOException;

public interface IDataReceiver
{
	public void onMessage(String channel, DataInput data) throws IOException;
}
