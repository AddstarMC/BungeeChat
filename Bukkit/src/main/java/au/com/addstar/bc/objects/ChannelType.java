package au.com.addstar.bc.objects;

public enum ChannelType
{
	Default(""),
	KeywordHighlight("~"),
	SocialSpy("~SS"),
	Broadcast("~BC"),
	AFKKick("~AK"),
	Reserved(null),
	Custom(null);
	
	private String mChannelName;
	
	ChannelType(String name)
	{
		mChannelName = name;
	}
	
	public String getName()
	{
		return mChannelName;
	}
	
	public static ChannelType from(String name)
	{
		if(name.isEmpty())
			return Default;
		else if(name.startsWith("~"))
		{
			for(ChannelType type : values())
			{
				if(type.getName() != null && name.equals(type.getName()))
					return type;
			}
			return Reserved;
		}
		else
			return Custom;
	}
}
