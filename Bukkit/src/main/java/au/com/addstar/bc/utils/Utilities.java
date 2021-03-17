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

package au.com.addstar.bc.utils;

/*-
 * #%L
 * BungeeChat-Bukkit
 * %%
 * Copyright (C) 2015 - 2020 AddstarMC
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

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.com.addstar.bc.BungeeChat;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;

public class Utilities {
    public static final NoConsoleChecker NO_CONSOLE = new NoConsoleChecker();
    public static final SocialSpyChecker SOCIAL_SPY_ENABLED = new SocialSpyChecker();
    public static final GsonComponentSerializer SERIALIZER = GsonComponentSerializer.gson();

    public static BukkitAudiences getAudienceProvider() {
        return BungeeChat.getAudiences();
    }


    public static String serializeComponent(Component comp) {
        return SERIALIZER.serialize(comp);
    }

    public static Component deserialize(String comp) {
        return SERIALIZER.deserialize(comp);
    }

    public static void localBroadCast(Component message, Collection<CommandSender> targets) {
        sendWithPermission(targets,null,null,message,object -> true);
    }

    public static void localBroadCast(Component message, String permission) {
        Collection<Permissible> targets;
        if(permission != null) {
            targets = Bukkit.getPluginManager().getPermissionSubscriptions(permission);
        } else {
            targets = (Collection<Permissible>) Bukkit.getOnlinePlayers();
        }
        sendWithPermission(targets,null,permission,message,object -> true);
    }
    public static void localBroadCast(Component message, String permission, @NotNull ValidChecker<Permissible> checker) {
        Collection<Permissible> targets;
        if(permission != null) {
            targets = Bukkit.getPluginManager().getPermissionSubscriptions(permission);
        } else {
            //noinspection unchecked
            targets = (Collection<Permissible>) Bukkit.getOnlinePlayers();
        }
        sendWithPermission(targets,null,permission,message,checker);
    }

    public static void localBroadCast(Component message, String permission, CommandSender except, @NotNull ValidChecker<Permissible> checker) {
        localBroadCast(message, permission, null, except, checker);
    }

    public static void localBroadCast(Component message, String permission, Collection<Permissible> recepients, CommandSender except, @NotNull ValidChecker<Permissible> checker) {
        sendWithPermission(recepients,except,permission,message,checker);
    }

    private static void sendWithPermission(Collection<? extends Permissible> targets, CommandSender except, String permission, Component message, @NotNull ValidChecker<Permissible> checker) {
        targets.stream()
                .filter(checker::isValid)
                .filter(permissible -> {
                    if (permission != null && except != null) {
                        return permissible != except && permissible.hasPermission(permission);
                    } else if ( permission == null && except != null) {
                        return permissible != except;
                    } else if (permission != null ){
                        return permissible.hasPermission(permission);
                    }
                    return false;
                }).forEach(permissible -> getAudienceProvider().sender((CommandSender) permissible).sendMessage(message));
    }

    /**
     * Converts color place-holders. Can take either Legacy(ie Ampersand &)
     * Or a {@link MiniMessage} formatted string.
     *
     * @param text text to color
     * @return A String
     */
    public static Component colorize(String text) {
        if (text.contains("&")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        } else {
            return MiniMessage.get().parse(text);
        }
    }

    public static long parseDateDiff(String dateDiff) {
        if (dateDiff == null)
            return 0;

        Pattern dateDiffPattern = Pattern.compile("^\\s*(\\-|\\+)?\\s*(?:([0-9]+)y)?\\s*(?:([0-9]+)mo)?\\s*(?:([0-9]+)w)?\\s*(?:([0-9]+)d)?\\s*(?:([0-9]+)h)?\\s*(?:([0-9]+)m)?\\s*(?:([0-9]+)s)?\\s*$");
        dateDiff = dateDiff.toLowerCase();

        Matcher m = dateDiffPattern.matcher(dateDiff);

        if (m.matches()) {
            int years, months, weeks, days, hours, minutes, seconds;
            boolean negative;

            if (m.group(1) != null)
                negative = (m.group(1).compareTo("-") == 0);
            else
                negative = false;

            if (m.group(2) != null)
                years = Integer.parseInt(m.group(2));
            else
                years = 0;

            if (m.group(3) != null)
                months = Integer.parseInt(m.group(3));
            else
                months = 0;

            if (m.group(4) != null)
                weeks = Integer.parseInt(m.group(4));
            else
                weeks = 0;

            if (m.group(5) != null)
                days = Integer.parseInt(m.group(5));
            else
                days = 0;

            if (m.group(6) != null)
                hours = Integer.parseInt(m.group(6));
            else
                hours = 0;

            if (m.group(7) != null)
                minutes = Integer.parseInt(m.group(7));
            else
                minutes = 0;

            if (m.group(8) != null)
                seconds = Integer.parseInt(m.group(8));
            else
                seconds = 0;

            // Now calculate the time
            long time = 0;
            time += seconds * 1000L;
            time += minutes * 60000L;
            time += hours * 3600000L;
            time += days * 72000000L;
            time += weeks * 504000000L;
            time += months * 2191500000L;
            time += years * 26298000000L;

            if (negative)
                time *= -1;

            return time;
        }

        return 0;
    }

    public static String timeDiffToString(long time) {
        StringBuilder builder = new StringBuilder();
        if (time >= 26298000000L) {
            int number = (int) (time / 26298000000L);
            time -= number * 26298000000L;

            builder.append(number);
            builder.append(" ");
            if (number == 1)
                builder.append("year");
            else
                builder.append("years");
        }

        if (time >= 2191500000L) {
            if (builder.length() != 0)
                builder.append(" ");

            int number = (int) (time / 2191500000L);
            time -= number * 2191500000L;

            builder.append(number);
            builder.append(" ");
            if (number == 1)
                builder.append("month");
            else
                builder.append("months");
        }

        if (time >= 504000000L) {
            if (builder.length() != 0)
                builder.append(" ");

            int number = (int) (time / 504000000L);
            time -= number * 504000000L;

            builder.append(number);
            builder.append(" ");
            if (number == 1)
                builder.append("week");
            else
                builder.append("weeks");
        }

        if (time >= 72000000L) {
            if (builder.length() != 0)
                builder.append(" ");

            int number = (int) (time / 72000000L);
            time -= number * 72000000L;

            builder.append(number);
            builder.append(" ");
            if (number == 1)
                builder.append("day");
            else
                builder.append("days");
        }

        if (time >= 3600000L) {
            if (builder.length() != 0)
                builder.append(" ");

            int number = (int) (time / 3600000L);
            time -= number * 3600000L;

            builder.append(number);
            builder.append(" ");
            if (number == 1)
                builder.append("hour");
            else
                builder.append("hours");
        }

        if (time >= 60000L) {
            if (builder.length() != 0)
                builder.append(" ");

            int number = (int) (time / 60000L);
            time -= number * 60000L;

            builder.append(number);
            builder.append(" ");
            if (number == 1)
                builder.append("minute");
            else
                builder.append("minutes");
        }

        if (time >= 1000L) {
            if (builder.length() != 0)
                builder.append(" ");

            int number = (int) (time / 1000L);

            builder.append(number);
            builder.append(" ");
            if (number == 1)
                builder.append("second");
            else
                builder.append("seconds");
        }

        return builder.toString();
    }

    public static String timeDiffToStringShort(long time) {
        StringBuilder builder = new StringBuilder();
        if (time >= 26298000000L) {
            int number = (int) (time / 26298000000L);
            time -= number * 26298000000L;

            builder.append(number);
            builder.append("y");
        }

        if (time >= 2191500000L) {
            if (builder.length() != 0)
                builder.append(" ");

            int number = (int) (time / 2191500000L);
            time -= number * 2191500000L;

            builder.append(number);
            builder.append("mo");
        }

        if (time >= 504000000L) {
            if (builder.length() != 0)
                builder.append(" ");

            int number = (int) (time / 504000000L);
            time -= number * 504000000L;

            builder.append(number);
            builder.append("w");
        }

        if (time >= 72000000L) {
            if (builder.length() != 0)
                builder.append(" ");

            int number = (int) (time / 72000000L);
            time -= number * 72000000L;

            builder.append(number);
            builder.append("d");
        }

        if (time >= 3600000L) {
            if (builder.length() != 0)
                builder.append(" ");

            int number = (int) (time / 3600000L);
            time -= number * 3600000L;

            builder.append(number);
            builder.append("h");
        }

        if (time >= 60000L) {
            if (builder.length() != 0)
                builder.append(" ");

            int number = (int) (time / 60000L);
            time -= number * 60000L;

            builder.append(number);
            builder.append("m");
        }

        if (time >= 1000L) {
            if (builder.length() != 0)
                builder.append(" ");

            int number = (int) (time / 1000L);

            builder.append(number);
            builder.append("s");
        }

        return builder.toString();
    }

    /**
     * Takes a string and colorizes it with the permissions from the sender.
     *
     * @param message
     * @param sender
     * @return
     */
    public static Component colorize(String message, CommandSender sender) {
        TextComponent.Builder result;
        if (message.contains("&")) {
            result = LegacyComponentSerializer.legacyAmpersand().deserialize(message).toBuilder();
        } else {
            return TextComponent.of(message);
        }
        final boolean hasColor = sender.hasPermission("bungeechat.color");
        final boolean hasBold = sender.hasPermission("bungeechat.format.bold");
        final boolean hasItalic = sender.hasPermission("bungeechat.format.italic");
        final boolean hasUnderline = sender.hasPermission("bungeechat.format.underline");
        final boolean hasStrikethrough = sender.hasPermission("bungeechat.format.strikethrough");
        final boolean hasMagic = sender.hasPermission("bungeechat.format.magic");
        if (hasColor && hasBold && hasItalic && hasUnderline && hasStrikethrough && hasMagic) {
            return result.build();
        }
        if (!hasItalic) {
            result.mapChildren(buildableComponent
                    -> buildableComponent.toBuilder().decoration(TextDecoration.ITALIC, false).build());
        }
        if (!hasBold) {
            result.mapChildren(buildableComponent
                    -> buildableComponent.toBuilder().decoration(TextDecoration.BOLD, false).build());
        }
        if (!hasUnderline) {
            result.mapChildren(buildableComponent
                    -> buildableComponent.toBuilder().decoration(TextDecoration.UNDERLINED, false).build());
        }
        if (!hasStrikethrough) {
            result.mapChildren(buildableComponent
                    -> buildableComponent.toBuilder().decoration(TextDecoration.STRIKETHROUGH, false).build());
        }
        if (!hasMagic) {
            result.mapChildren(buildableComponent
                    -> buildableComponent.toBuilder().decoration(TextDecoration.OBFUSCATED, false).build());
        }
        if (!hasColor) {
            result.mapChildren(buildableComponent
                    -> buildableComponent.toBuilder().color(NamedTextColor.WHITE).build());
        }
        return result.build();
    }

    public static Component parseChatColors(String input) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
    }


}
