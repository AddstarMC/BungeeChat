package au.com.addstar.bc;

public class ChatChannel
{
	public String name;
	public String command;
	public String format;
	public String permission;
	public String listenPerm;
	
	public ChatChannel(String name, String cmd, String prefix, String perm, String listenPerm)
	{
		this.name = name;
		this.command = cmd;
		this.format = prefix;
		this.permission = perm;
		this.listenPerm = listenPerm;
	}
}
