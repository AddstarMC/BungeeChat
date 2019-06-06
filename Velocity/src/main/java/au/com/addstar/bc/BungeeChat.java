package au.com.addstar.bc;

/*-
 * #%L
 * BungeeChat-Velocity
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
import com.google.inject.Inject;

import au.com.addstar.bc.config.ChatChannel;
import au.com.addstar.bc.config.Config;
import au.com.addstar.bc.sync.Packet;
import au.com.addstar.bc.sync.PacketManager;
import au.com.addstar.bc.sync.ProxyComLink;
import au.com.addstar.bc.sync.SyncManager;

import com.sun.org.glassfish.external.statistics.annotations.Reset;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.kyori.text.TextComponent;

import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import net.kyori.text.format.TextFormat;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.soap.Text;

/**
 * Created for the AddstarMC IT Project.
 * Created by Narimm on 4/06/2019.
 */
@Plugin(id = "BungeeChat", name = "ProxyChat", version = "1.0-SNAPSHOT",
        description = "A centralized Chat Plugin", authors = {"Me"})
public class BungeeChat {
    private Config mConfig;
    private HashMap<String, List<TextFormat>> mKeywordSettings = new HashMap<>();

    protected static BungeeChat instance;
    private PacketManager mPacketManager;
    public ProxyServer getServer() {
        return server;
    }

    private final ProxyServer server;

    public Logger getLogger() {
        return logger;
    }

    private final Logger logger;
    private ProxyComLink mComLink;
    private Path dataFolder;

    public static BungeeChat getInstance() {
        return instance;
    }

    @Inject
    public BungeeChat(ProxyServer server, Logger logger, @DataDirectory Path dataFolder) {
        this.server = server;
        String name;
        this.logger = logger;
        this.dataFolder = dataFolder;
        Path configPath = Paths.get(dataFolder.toString(),"config.yml");
        if(Files.notExists(configPath)) {
            try {
                Files.createFile(configPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        saveResource(dataFolder,"keyword.txt",false);
        ColourTabList.initialize(this);
        mConfig = new Config(configPath.toFile());
        loadConfig();
        instance =  this;
        mComLink = new ProxyComLink(server);
        final CountDownLatch setupWait = new CountDownLatch(1);
        server.getScheduler().buildTask(this, () -> {
            try
            {
                mComLink.init(mConfig.redis.host, mConfig.redis.port, mConfig.redis.password);
            }
            finally
            {
                setupWait.countDown();
            }
        });

        try
        {
            setupWait.await();
        }
        catch(InterruptedException ignored)
        {
        }
        mPacketManager = new PacketManager(this);
        mPacketManager.initialize();
        mPacketManager.addHandler(new PacketHandler(this), (Class<? extends Packet>[])null);

    }

    public boolean loadConfig()
    {
        try
        {
            mConfig.init();

            for(ChatChannel channel : mConfig.channels.values())
            {
                if(channel.listenPermission == null)
                    channel.listenPermission = channel.permission;
                else if(channel.listenPermission.equals("*"))
                    channel.listenPermission = "";
            }

            if(mConfig.keywordHighlighter.enabled)
            {
                loadKeywordFile(dataFolder,mConfig.keywordHighlighter.keywordFile);
            }

            ColourTabList.updateAll();
            if(mMuteHandler != null)
                mMuteHandler.updateSettings(mConfig);
            return true;
        }
        catch ( InvalidConfigurationException e )
        {
            logger.warn("Could not load config");
            e.printStackTrace();
            return false;
        }
        catch (IOException e)
        {
            logger.warn("Could not load " + mConfig.keywordHighlighter.keywordFile);
            e.printStackTrace();
            return false;
        }
    }
    private void loadKeywordFile(Path dataFolder, String file) throws IOException
    {
        mKeywordSettings.clear();
        File onDisk = new File(dataFolder.toFile(), file);

        try (InputStream input = new FileInputStream(onDisk)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            int lineNo = 0;
            while (reader.ready()) {
                ++lineNo;
                String line = reader.readLine();
                if (line.startsWith("#") || line.trim().isEmpty())
                    continue;

                String regex, colourString;

                if (line.contains(">")) {
                    int pos = line.lastIndexOf('>');
                    regex = line.substring(0, pos).trim();
                    colourString = line.substring(pos + 1).trim();
                } else {
                    regex = line.trim();
                    colourString = "c";
                }
                try {
                    Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                } catch (PatternSyntaxException e) {
                    logger.warn("[" + file + "] Invalid regex: \"" + regex + "\" at line " + lineNo);
                    continue;
                }
                List<TextFormat> formats = fromCharArray(colourString.toCharArray());
                mKeywordSettings.put(regex,formats);
            }
            reader.close();
        }
    }

    protected List<TextFormat> fromCharArray(char[] string){
        List<TextFormat> formats = new ArrayList<>();
        for (int i = 0; i < string.length; ++i) {
            char c = string[i];
            TextFormat form = TextFormatConverter.getByChar(c);
            formats.add(form);
            if (form == null) {
                logger.warn("[CONVERTER] Invalid colour code: \'" + c + "\' at position " + i);
                continue;
            }
        }
        return formats;
    };
    static enum TextFormatConverter{
        BLACK('0',TextColor.BLACK),
        DARK_GRAY('8',TextColor.DARK_GRAY),
        GRAY('7',TextColor.GRAY),
        GREEN('a',TextColor.GREEN),
        DARK_GREEN('2',TextColor.DARK_GREEN),
        GOLD('6',TextColor.GOLD),
        YELLOW('e',TextColor.YELLOW),
        WHITE('f',TextColor.WHITE),
        RED('c',TextColor.RED),
        DARK_RED('4',TextColor.DARK_RED),
        BLUE('9',TextColor.BLUE),
        DARK_BLUE('1',TextColor.DARK_BLUE),
        AQUA('b',TextColor.AQUA),
        DARK_AQUA('3',TextColor.DARK_AQUA),
        DARK_PURPLE('5',TextColor.DARK_PURPLE),
        LIGHT_PURPLE('d',TextColor.LIGHT_PURPLE),
        BOLD('l', TextDecoration.BOLD),
        OBFUSCATED('k',TextDecoration.OBFUSCATED),
        ITALIC('o',TextDecoration.ITALIC),
        STRIKETHROUGH('m',TextDecoration.STRIKETHROUGH),
        UNDERLINED('n',TextDecoration.UNDERLINED);
        private char oldchar;
        private net.kyori.text.format.TextFormat format;
        private final static HashMap<Character,TextFormat> chartoFormat = new HashMap<>();
        static{
            int i = 0;
            while(i<values().length){
                chartoFormat.put(values()[i].oldchar,values()[i].format);
                i++;
            }
        }
        private static void add(char c,TextFormat format){
            chartoFormat.put(c,format);
        }


        TextFormatConverter(char oldchar, TextFormat format) {
            this.oldchar = oldchar;
            this.format = format;
            add(oldchar,format);
        }
        public static TextFormat getByChar(char c){
            return chartoFormat.get(c);
        }

        public char getOldchar() {
            return oldchar;
        }

        public TextFormat getFormat() {
            return format;
        }
    }


    private void saveResource(Path dataFolder,String resource, boolean overwrite)  {
        Path path = Paths.get(dataFolder.toString(),resource);
        if(Files.exists(path) && !overwrite)
            return;
        FileOutputStream output = null;
        try (
                InputStream input = getClass().getResourceAsStream(resource);
        ){
            Files.createFile(path);

            if(input == null)
            {
                logger.warn("Could not save resource " + resource + ". It does not exist in the jar.");
                return;
            }
            output = new FileOutputStream(path.toFile());
            byte[] buffer = new byte[1024];
            int read;

            while((read = input.read(buffer)) != -1)
            {
                output.write(buffer, 0, read);
            }
        } catch (IOException e) {
            logger.warn("Could not save resource " + resource + ". An IOException occured:");
            e.printStackTrace();
        } finally {
            if(output !=null)
                try{
                    output.close();
                }catch (IOException ignored){};
        }
    }
    public ChatChannel getChannel(String name)
    {
        return mConfig.channels.get(name);
    }

    public PacketManager getPacketManager()
    {
        return mPacketManager;
    }

    public PlayerSettingsManager getManager()
    {
        return mSettings;
    }

    public SyncManager getSyncManager()
    {
        return mSyncManager;
    }

    public ProxyComLink getComLink()
    {
        return mComLink;
    }

    public MuteHandler getMuteHandler()
    {
        return mMuteHandler;
    }

    public SkinLibrary getSkinLibrary()
    {
        return mSkins;
    }
}
