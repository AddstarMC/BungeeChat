/*
 * BungeeChat
 *
 * Copyright (c) 2015 - 2020.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy   of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is *
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR  IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package au.com.addstar.bc.objects;

import au.com.addstar.bc.utils.Utilities;
import junit.framework.TestCase;

/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 17/07/2020.
 */
public class ChatChannelTest extends TestCase {

    public void testParseChatColors() {
        String input = "&r&c[Admin]#54A4B5testcolors add something funcky #5 or more #454545 asadsdas asd#FFF asdasdasd s\n";
        String out = Utilities.parseChatColors(input);
        assertEquals("§r§c[Admin]§x§5§4§a§4§b§5testcolors add something funcky #5 or more §x§4§5§4§5§4§5 asadsdas asd§x§0§0§0§f§f§f asdasdasd s\n",out);
        String input2 = "#000000Black#FFFFFFWhite";
        String out2 = Utilities.parseChatColors(input2);
        assertEquals("§x§0§0§0§0§0§0Black§x§f§f§f§f§f§fWhite",out2);
    }
}