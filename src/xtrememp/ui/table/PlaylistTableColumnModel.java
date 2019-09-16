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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

/**
 *
 * @author Besmir Beqiri
 */
public class PlaylistTableColumnModel extends DefaultTableColumnModel implements ActionListener {

    private Map<JCheckBoxMenuItem, PlaylistTableColumn> map;
    private JPopupMenu popupMenu;

    public PlaylistTableColumnModel() {
        super();

        this.map = new HashMap<JCheckBoxMenuItem, PlaylistTableColumn>();
        this.popupMenu = new JPopupMenu();

        // Init popup menu.
        PlaylistColumn[] playlistColumns = PlaylistColumn.values();
        for (PlaylistColumn playlistColumn : playlistColumns) {
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(playlistColumn.getDisplayName(), false);
            menuItem.setName(playlistColumn.name());
            if (playlistColumn == PlaylistColumn.TITLE) {
                menuItem.setEnabled(false);
            } else {
                menuItem.addActionListener(this);
            }
            popupMenu.add(menuItem);
        }
    }

    public JPopupMenu getPopupMenu() {
        return popupMenu;
    }

    public void resetAll(int modelIndex) {
        for (Enumeration<TableColumn> columnEnum = getColumns(); columnEnum.hasMoreElements();) {
            TableColumn tableColumn = columnEnum.nextElement();
            if (tableColumn instanceof PlaylistTableColumn) {
                PlaylistTableColumn playlistTableColumn = (PlaylistTableColumn) tableColumn;
                if (playlistTableColumn.getModelIndex() != modelIndex) {
                    playlistTableColumn.reset();
                }
            }
        }
    }

    public PlaylistColumn[] getPlaylistColumns() {
        PlaylistColumn[] playlistColumns = new PlaylistColumn[getColumnCount()];
        List<PlaylistColumn> columnsList = new ArrayList<PlaylistColumn>(getColumnCount());
        for (Enumeration<TableColumn> columnEnum = getColumns(); columnEnum.hasMoreElements();) {
            TableColumn tableColumn = columnEnum.nextElement();
            if (tableColumn instanceof PlaylistTableColumn) {
                PlaylistTableColumn playlistTableColumn = (PlaylistTableColumn) tableColumn;
                columnsList.add(playlistTableColumn.getModelIndex(), playlistTableColumn.getPlaylistColumn());
            }
        }
        return columnsList.toArray(playlistColumns);
    }

    @Override
    public void addColumn(TableColumn tableColumn) {
        super.addColumn(tableColumn);

        if (tableColumn instanceof PlaylistTableColumn) {
            PlaylistTableColumn playlistTableColumn = (PlaylistTableColumn) tableColumn;
            Component[] components = popupMenu.getComponents();
            for (Component component : components) {
                JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) component;
                if (menuItem.getText().equals(playlistTableColumn.getName())) {
                    map.put(menuItem, playlistTableColumn);
                    menuItem.setSelected(true);
                    break;
                }
            }
        }
    }

    @Override
    public PlaylistTableColumn getColumn(int columnIndex) {
        return (PlaylistTableColumn) super.getColumn(columnIndex);
    }

    @Override
    public void moveColumn(int columnIndex, int newIndex) {
        TableColumn fromTableColumn = getColumn(columnIndex);
        TableColumn toTableColumn = getColumn(newIndex);
        super.moveColumn(columnIndex, newIndex);
        fromTableColumn.setModelIndex(newIndex);
        toTableColumn.setModelIndex(columnIndex);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source instanceof JCheckBoxMenuItem) {
            JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) source;
            PlaylistTableColumn playlistTableColumn = map.get(menuItem);
            if (menuItem.isSelected()) {
                int columnCount = getColumnCount();
                if (playlistTableColumn == null) {
                    PlaylistColumn playlistColumn = PlaylistColumn.valueOf(menuItem.getName());
                    playlistTableColumn = new PlaylistTableColumn(playlistColumn, columnCount);
                    map.put(menuItem, playlistTableColumn);
                }
                super.addColumn(playlistTableColumn);
                playlistTableColumn.setModelIndex(columnCount);
            } else {
                for (Enumeration<TableColumn> columnEnum = getColumns(); columnEnum.hasMoreElements();) {
                    TableColumn tableColumn = columnEnum.nextElement();
                    int modelIndex = tableColumn.getModelIndex();
                    if (modelIndex > playlistTableColumn.getModelIndex()) {
                        tableColumn.setModelIndex(modelIndex - 1);
                    }
                }
                removeColumn(playlistTableColumn);
            }
        }
    }
}
