package au.com.addstar.bc.sync;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

/**
 * Created for the AddstarMC Project. Created by Narimm on 3/09/2018.
 */

public class ServerComLinkTest {
    ServerComLink link;
    
    @Before
    public void setup(){
        link = new TestServerComLink();
    }
    
    public void init() {
        setup();
        link.init("localhost",6379,null);
    }
    
    public void publish() {
        setup();
        link.init("localhost",6379,null);
        byte[] b = "Test".getBytes();
        link.publish("Test",b );
    }
 
}