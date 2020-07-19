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

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 19/07/2020.
 */
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionUtil {

    public static final BukkitVersion v1_8_8_R01 = BukkitVersion.fromString("1.8.8-R0.1-SNAPSHOT");
    public static final BukkitVersion v1_9_R01 = BukkitVersion.fromString("1.9-R0.1-SNAPSHOT");
    public static final BukkitVersion v1_9_4_R01 = BukkitVersion.fromString("1.9.4-R0.1-SNAPSHOT");
    public static final BukkitVersion v1_10_R01 = BukkitVersion.fromString("1.10-R0.1-SNAPSHOT");
    public static final BukkitVersion v1_10_2_R01 = BukkitVersion.fromString("1.10.2-R0.1-SNAPSHOT");
    public static final BukkitVersion v1_11_R01 = BukkitVersion.fromString("1.11-R0.1-SNAPSHOT");
    public static final BukkitVersion v1_11_2_R01 = BukkitVersion.fromString("1.11.2-R0.1-SNAPSHOT");
    public static final BukkitVersion v1_12_0_R01 = BukkitVersion.fromString("1.12.0-R0.1-SNAPSHOT");
    public static final BukkitVersion v1_12_2_R01 = BukkitVersion.fromString("1.12.2-R0.1-SNAPSHOT");
    public static final BukkitVersion v1_13_0_R01 = BukkitVersion.fromString("1.13.0-R0.1-SNAPSHOT");
    public static final BukkitVersion v1_13_2_R01 = BukkitVersion.fromString("1.13.2-R0.1-SNAPSHOT");
    public static final BukkitVersion v1_14_R01 = BukkitVersion.fromString("1.14-R0.1-SNAPSHOT");
    public static final BukkitVersion v1_14_4_R01 = BukkitVersion.fromString("1.14.4-R0.1-SNAPSHOT");
    public static final BukkitVersion v1_15_R01 = BukkitVersion.fromString("1.15-R0.1-SNAPSHOT");
    public static final BukkitVersion v1_15_2_R01 = BukkitVersion.fromString("1.15.2-R0.1-SNAPSHOT");
    public static final BukkitVersion v1_16_1_R01 = BukkitVersion.fromString("1.16.1-R0.1-SNAPSHOT");

    private static BukkitVersion serverVersion = null;


    public static class BukkitVersion implements Comparable<BukkitVersion> {
        private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.?([0-9]*)?(?:-pre(\\d))?(?:-?R?([\\d.]+))?(?:-SNAPSHOT)?");

        private final int major;
        private final int minor;
        private final int prerelease;
        private final int patch;
        private final double revision;

        public static BukkitVersion fromString(String string) {
            Preconditions.checkNotNull(string, "string cannot be null.");
            Matcher matcher = VERSION_PATTERN.matcher(string);
            if (!matcher.matches()) {
                matcher = VERSION_PATTERN.matcher(v1_16_1_R01.toString());
                Preconditions.checkArgument(matcher.matches(), string + " is not in valid version format. e.g. 1.8.8-R0.1");
            }
            return from(matcher.group(1), matcher.group(2), matcher.group(3), matcher.groupCount() < 5 ? "" : matcher.group(5), matcher.group(4));
        }

        private static BukkitVersion from(String major, String minor, String patch, String revision, String prerelease) {
            if (patch == null || patch.isEmpty()) patch = "0";
            if (revision == null || revision.isEmpty()) revision = "0";
            if (prerelease == null || prerelease.isEmpty()) prerelease = "-1";
            return new BukkitVersion(Integer.parseInt(major),
                  Integer.parseInt(minor),
                  Integer.parseInt(patch),
                  Double.parseDouble(revision),
                  Integer.parseInt(prerelease));
        }

        private BukkitVersion(int major, int minor, int patch, double revision, int prerelease) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.revision = revision;
            this.prerelease = prerelease;
        }

        public boolean isHigherThan(BukkitVersion o) {
            return compareTo(o) > 0;
        }

        public boolean isHigherThanOrEqualTo(BukkitVersion o) {
            return compareTo(o) >= 0;
        }

        public boolean isLowerThan(BukkitVersion o) {
            return compareTo(o) < 0;
        }

        public boolean isLowerThanOrEqualTo(BukkitVersion o) {
            return compareTo(o) <= 0;
        }

        public int getMajor() {
            return major;
        }

        public int getMinor() {
            return minor;
        }

        public int getPatch() {
            return patch;
        }

        public double getRevision() {
            return revision;
        }

        public int getPrerelease() {
            return prerelease;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BukkitVersion that = (BukkitVersion) o;
            return major == that.major &&
                  minor == that.minor &&
                  patch == that.patch &&
                  revision == that.revision &&
                  prerelease == that.prerelease;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(major, minor, patch, revision, prerelease);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(major + "." + minor);
            if (patch != 0) {
                sb.append(".").append(patch);
            }
            if (prerelease != -1) {
                sb.append("-pre").append(prerelease);
            }
            return sb.append("-R").append(revision).toString();
        }

        @Override
        public int compareTo(BukkitVersion o) {
            if (major < o.major) {
                return -1;
            } else if (major > o.major) {
                return 1;
            } else { // equal major
                if (minor < o.minor) {
                    return -1;
                } else if (minor > o.minor) {
                    return 1;
                } else { // equal minor
                    if (patch < o.patch) {
                        return -1;
                    } else if (patch > o.patch) {
                        return 1;
                    } else { // equal patch
                        if (prerelease < o.prerelease) {
                            return -1;
                        } else if (prerelease > o.prerelease) {
                            return 1;
                        } else { // equal prerelease
                            return Double.compare(revision, o.revision);
                        }
                    }
                }
            }
        }
    }
}
