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

import java.awt.Color;

/**
 * Created for the AddstarMC Project.
 * Created by Narimm on 17/07/2020.
 */
public class ChatChannelTest extends TestCase {

    public void testParseChatColors() {
        String input = "<red>[Admin]</red><#54A4B5>testcolors</#54A4B5> add something funcky #5 or more <#454545>asadsdas</#454545> asd#FFF asdasdasd s\n";
        String out = Utilities.parseChatColors(input);
        assertEquals("<red>[Admin]<color:#54a4b5>testcolors</color:#54a4b5> add something funcky #5 or more <color:#454545>asadsdas</color:#454545> asd#FFF asdasdasd s\n",out);
        String input2 = "<#000000>Black <#FFFFFF>White";
        String out2 = Utilities.parseChatColors(input2);
        assertEquals("<black>Black </black>White",out2);
    }
}