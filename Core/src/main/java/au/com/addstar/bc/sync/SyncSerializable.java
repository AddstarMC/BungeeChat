package au.com.addstar.bc.sync;

import java.util.Map;

public interface SyncSerializable
{
	/**
	 * Must also implement: public static T fromMap(Map<String, Object>)
	 */
    Map<String, Object> toMap();
}
