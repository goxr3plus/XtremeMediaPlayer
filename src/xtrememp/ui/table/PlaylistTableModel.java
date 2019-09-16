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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import xtrememp.playlist.Playlist;
import xtrememp.playlist.PlaylistItem;
import xtrememp.playlist.filter.Predicate;
import xtrememp.util.Utilities;

/**
 * Playlist table model.
 *
 * @author Besmir Beqiri
 */
public class PlaylistTableModel extends AbstractTableModel {

    private final Playlist playlist;
    private final PlaylistTableColumnModel playlistTableColumnModel;

    public PlaylistTableModel(Playlist playlist, PlaylistTableColumnModel playlistTableColumnModel) {
        this.playlist = playlist;
        this.playlistTableColumnModel = playlistTableColumnModel;
    }

    public void add(List<PlaylistItem> newItems) {
        int first = playlist.size();
        int last = first + newItems.size() - 1;
        playlist.addAll(newItems);
        fireTableRowsInserted(first, last);
    }

    public void add(PlaylistItem item) {
        int index = playlist.size();
        playlist.addItem(item);
        fireTableRowsInserted(index, index);
    }

    public void removeItemAt(int index) {
        playlist.removeItemAt(index);
        fireTableRowsDeleted(index, index);
    }

    public void removeAll(Collection<? extends PlaylistItem> c) {
        playlist.removeAll(c);
        fireTableDataChanged();
    }

    public void clear() {
        playlist.clear();
        fireTableDataChanged();
    }

    public void filter(Predicate<PlaylistItem> filterPredicate) {
        playlist.filter(filterPredicate);
        fireTableDataChanged();
    }

    public void sort(Comparator<PlaylistItem> comparator) {
        playlist.sort(comparator);
        fireTableDataChanged();
    }

    public void randomize() {
        playlist.randomize();
        fireTableDataChanged();
    }

    public void moveItem(int fromIndex, int toIndex) {
        playlist.moveItem(fromIndex, toIndex);
        if (fromIndex < toIndex) {
            fireTableRowsUpdated(fromIndex, toIndex);
        } else {
            fireTableRowsUpdated(toIndex, fromIndex);
        }
    }

    @Override
    public String getColumnName(int columnIndex) {
        return playlistTableColumnModel.getColumn(columnIndex).getName();
    }

    @Override
    public int getRowCount() {
        return playlist.size();
    }

    @Override
    public int getColumnCount() {
        return playlistTableColumnModel.getColumnCount();
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (!playlist.isEmpty()) {
            PlaylistTableColumn playlistTableColumn = playlistTableColumnModel.getColumn(columnIndex);
            PlaylistColumn playlistColumn = playlistTableColumn.getPlaylistColumn();
            PlaylistItem item = playlist.getItemAt(rowIndex);
            StringBuilder sb = new StringBuilder();
            if (item.isFile()) {
                switch (playlistColumn) {
                    case TRACK:
                        String trackStr = item.getTagInfo().getTrack();
                        int trackNum = -1;
                        if (!Utilities.isNullOrEmpty(trackStr)) {
                            try {
                                trackNum = Integer.parseInt(trackStr);
                            } catch (NumberFormatException ex) {
                            }
                        }
                        sb.append(trackNum < 0 ? "" : trackNum);
                        break;
                    case TITLE:
                        sb.append(" ");
                        String title = item.getTagInfo().getTitle();
                        sb.append(Utilities.isNullOrEmpty(title) ? item.getFormattedName() : title);
                        break;
                    case DURATION:
                        sb.append(item.getFormattedLength()).append(" ");
                        break;
                    case ARTIST:
                        sb.append(" ").append(item.getTagInfo().getArtist());
                        break;
                    case ALBUM:
                        sb.append(" ").append(item.getTagInfo().getAlbum());
                        break;
                    case GENRE:
                        sb.append(" ").append(item.getTagInfo().getGenre());
                        break;
                }
            } else {
                switch (playlistColumn) {
                    case TITLE:
                        sb.append(" ");
                        sb.append(item.getFormattedName());
                        break;
                    default:
                        sb.append(" ");
                        break;
                }
            }
            return sb.toString();
        }
        return null;
    }
}
