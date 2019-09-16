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
package xtrememp.ui.table;

import java.util.Comparator;
import xtrememp.playlist.PlaylistItem;
import xtrememp.playlist.sort.AlbumComparator;
import xtrememp.playlist.sort.ArtistComparator;
import xtrememp.playlist.sort.DurationComparator;
import xtrememp.playlist.sort.GenreComparator;
import xtrememp.playlist.sort.TitleComparator;
import xtrememp.playlist.sort.TrackComparator;
import static xtrememp.util.Utilities.tr;

/**
 *
 * @author Besmir Beqiri
 */
public enum PlaylistColumn {

    TRACK(tr("MainFrame.PlaylistManager.PlaylistColumn.Track"), 100, new TrackComparator()),
    TITLE(tr("MainFrame.PlaylistManager.PlaylistColumn.Title"), 850, new TitleComparator()),
    DURATION(tr("MainFrame.PlaylistManager.PlaylistColumn.Duration"), 150, new DurationComparator()),
    ARTIST(tr("MainFrame.PlaylistManager.PlaylistColumn.Artist"), 500, new ArtistComparator()),
    ALBUM(tr("MainFrame.PlaylistManager.PlaylistColumn.Album"), 300, new AlbumComparator()),
    GENRE(tr("MainFrame.PlaylistManager.PlaylistColumn.Genre"), 200, new GenreComparator());
    private String displayName;
    private int width;
    private Comparator<PlaylistItem> comparator;

    PlaylistColumn(String displayName, int width, Comparator<PlaylistItem> comparator) {
        this.displayName = displayName;
        this.width = width;
        this.comparator = comparator;
    }

    /**
     * @return the displayName
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }

    public Comparator<PlaylistItem> getComparator() {
        return comparator;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name());
        sb.append("[").append(displayName);
        sb.append(",").append(width);
        sb.append(",").append(comparator).append("]");
        return sb.toString();
    }
}
