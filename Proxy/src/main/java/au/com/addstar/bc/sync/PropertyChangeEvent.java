package au.com.addstar.bc.sync;

/*-
 * #%L
 * BungeeChat-Proxy
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

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;

public class PropertyChangeEvent extends Event
{
	private ProxiedPlayer mPlayer;
	private String mProperty;
	private Object mOldValue;
	private Object mNewValue;
	
	public PropertyChangeEvent(ProxiedPlayer player, String property, Object oldVal, Object newVal)
	{
		mPlayer = player;
		mProperty = property;
		mOldValue = oldVal;
		mNewValue = newVal;
	}
	
	public ProxiedPlayer getPlayer()
	{
		return mPlayer;
	}
	
	public String getProperty()
	{
		return mProperty;
	}
	
	public Object getOldValue()
	{
		return mOldValue;
	}
	
	public Object getNewValue()
	{
		return mNewValue;
	}
}
