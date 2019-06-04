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
import au.com.addstar.bc.sync.ProxyComLink;

import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextFormat;
import net.kyori.text.serializer.ComponentSerializer;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;

import org.slf4j.Logger;

import sun.security.provider.DSAKeyPairGenerator;

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
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created for the AddstarMC IT Project.
 * Created by Narimm on 4/06/2019.
 */
@Plugin(id = "BungeeChat", name = "ProxyChat", version = "1.0-SNAPSHOT",
        description = "A centralized Chat Plugin", authors = {"Me"})
public class BungeeChat {
    private Config mConfig;
    private HashMap<String, String> mKeywordSettings = new HashMap<>();

    private static BungeeChat instance;
    private final ProxyServer server;
    private final Logger logger;
    private ProxyComLink mComLink;
    private Path dataFolder;

    public static BungeeChat getInstance() {
        return instance;
    }

    @Inject
    public BungeeChat(ProxyServer server, Logger logger, @DataDirectory Path dataFolder) {
        this.server = server;
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
        mConfig = new Config(configPath.toFile());
        loadConfig();
        instance =  this;
        mComLink = new ProxyComLink(server);
        server.getScheduler().buildTask(this, new Runnable() {
            @Override
            public void run() {
                mComLink.init(mConfig.redis.host, mConfig.redis.port, mConfig.redis.password);
            }
        });

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
                    colourString = "e";
                }
                try {
                    Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                } catch (PatternSyntaxException e) {
                    logger.warn("[" + file + "] Invalid regex: \"" + regex + "\" at line " + lineNo);
                    continue;
                }

                StringBuilder colour = new StringBuilder();
                for (int i = 0; i < colourString.length(); ++i) {
                    char c = colourString.charAt(i);
                    TextComponent comp = LegacyComponentSerializer.

                    if (format == null) {
                        logger.warning("[" + file + "] Invalid colour code: \'" + c + "\' at line " + lineNo);
                        continue;
                    }

                    colour.append(component.toString());
                }

                mKeywordSettings.put(regex, component.toString());
            }
            reader.close();
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
}
