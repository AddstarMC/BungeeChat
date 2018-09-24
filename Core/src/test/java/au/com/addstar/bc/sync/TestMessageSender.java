package au.com.addstar.bc.sync;

/**
 * Created for the AddstarMC Project. Created by Narimm on 3/09/2018.
 */
public class TestMessageSender implements MessageSender {
    @Override
    public int getId() {
        return 1;
    }
    
    @Override
    public String getName() {
        return "TestServer";
    }
}
