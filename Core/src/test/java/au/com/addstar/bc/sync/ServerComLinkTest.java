package au.com.addstar.bc.sync;

/*-
 * #%L
 * BungeeChat-Core
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
