package au.com.addstar.bc.sync;

import java.io.DataInput;

public interface IDataReceiver
{
	/**
	 * Called when a message is received.
	 * @param channel The channel the message came on
	 * @param in A data input to read with
	 * @param sender The sender of the message. Null if this is on bukkit side
	 */
    void onReceive(String channel, DataInput in, MessageSender sender);
}
