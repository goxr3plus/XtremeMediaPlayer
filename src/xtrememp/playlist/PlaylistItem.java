/**
 * Xtreme Media Player a cross-platform media player.
 * Copyright (C) 2005-2011 Besmir Beqiri
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package xtrememp.playlist;

import java.util.concurrent.TimeUnit;
import xtrememp.tag.TagInfo;
import xtrememp.tag.TagInfoFactory;
import xtrememp.util.Utilities;

/**
 * This class implements item for playlist.
 * 
 * @author Besmir Beqiri
 */
public class PlaylistItem {

//    private static final Logger logger = LoggerFactory.getLogger(PlaylistItem.class);
    private String name;
    private String location;
    private long duration = 0;
    private boolean isFile = false;
    private TagInfo tagInfo;
    private String formattedName;
    private String formatedLength;

    /**
     * Default constructor.
     *
     * @param name Track name.
     * @param location File or URL location.
     * @param duration Duration in seconds.
     * @param isFile <true>true</true> for File instance, else <true>false</true>.
     */
    public PlaylistItem(String name, String location, long duration, boolean isFile) {
        this.name = name;
        this.location = location;
        this.duration = duration;
        this.isFile = isFile;
        this.formatedLength = getFormattedLength(this.duration);
    }

    /**
     * Returns the name of this playlist item.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the location of this playlist item.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the duration (in seconds) of this playlist item.
     */
    public void setDuration(long duration) {
        this.duration = duration;
    }

    /**
     * Returns the duration (in seconds) of this playlist item.
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Returns the file instance flag.
     *
     * @return <code>true</code> if item to play is a file, else <code>false</code>.
     */
    public boolean isFile() {
        return isFile;
    }

    /**
     * Load and return TagInfo instance.
     *
     * @return A {@link TagInfo} instance.
     */
    public TagInfo getTagInfo() {
        if ((tagInfo == null) && (!Utilities.isNullOrEmpty(location))) {
            tagInfo = TagInfoFactory.getInstance().getTagInfo(location);
            if (isFile) {
                duration = tagInfo.getTrackLength();
                formatedLength = getFormattedLength(duration);
            }
        }
        return tagInfo;
    }

    public void setFormattedName(String formattedName) {
        this.name = formattedName;
        this.formattedName = formattedName;
    }

    /**
     * Returns a formatted name such as "Title - Artist" if possible.
     *
     * @return A formatted string.
     */
    public String getFormattedName() {
        if (tagInfo == null) {
            formattedName = name;
        } else if (formattedName == null) {
            String title = tagInfo.getTitle();
            if (!Utilities.isNullOrEmpty(title)) {
                StringBuilder sb = new StringBuilder(title);
                String artist = tagInfo.getArtist();
                if (!Utilities.isNullOrEmpty(artist)) {
                    sb.append(" - ");
                    sb.append(artist);
                }
                formattedName = sb.toString();
            } else {
                formattedName = name;
            }
        }
        return formattedName;
    }

    /**
     * Returns a human-readable version such as "hh:mm:ss" of this item duration.
     *
     * @return A formatted string.
     */
    public String getFormattedLength() {
        return formatedLength;
    }

    /**
     * Returns a human-readable version such as "h:mm:ss" of a given duration
     * value.
     *
     * @param duration Duration in seconds.
     * @return A formatted string.
     */
    public final String getFormattedLength(long duration) {
        String result = "";
        if (duration > -1) {
            long hours = TimeUnit.SECONDS.toHours(duration);
            long min = TimeUnit.SECONDS.toMinutes(duration)
                    - TimeUnit.HOURS.toMinutes(hours);
            long sec = duration - TimeUnit.MINUTES.toSeconds(min)
                    - TimeUnit.HOURS.toSeconds(hours);
            if (hours > 0) {
                result = String.format("%d:%02d:%02d", hours, min, sec);
            } else {
                result = String.format("%02d:%02d", min, sec);
            }
        }
        return result;
    }

    /**
     * Returns a formatted string such as Seconds,Title,Artist used for saving
     * in M3U format.
     *
     * @return A formatted string.
     */
    public String getM3UExtInf() {
        StringBuilder sb = new StringBuilder(String.valueOf(duration));
        if (tagInfo == null) {
            sb.append(',');
            sb.append(formattedName);
        } else {
            String title = tagInfo.getTitle();
            if (!Utilities.isNullOrEmpty(title)) {
                sb.append(',');
                sb.append(title);
                String artist = tagInfo.getArtist();
                if (!Utilities.isNullOrEmpty(artist)) {
                    sb.append(" - ");
                    sb.append(artist);
                }
            } else {
                sb.append(',');
                sb.append(formattedName);
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return location;
    }
}
