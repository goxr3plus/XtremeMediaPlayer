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

import java.awt.Color;
import java.awt.Component;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.pushingpixels.substance.api.ComponentState;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.pushingpixels.substance.api.skin.SkinChangeListener;
import xtrememp.playlist.PlaylistItem;

/**
 *
 * @author Besmir Beqiri
 */
public class PlaylistTableColumn extends TableColumn implements TableCellRenderer,
        SkinChangeListener {

    private Icon UP_ICON;
    private Icon DOWN_ICON;
    private PlaylistColumn playlistColumn;
    private JLabel headerLabel;
    private boolean sortOrderUp = false;

    public PlaylistTableColumn(PlaylistColumn playlistColumn, int index) {
        super(index);
        this.playlistColumn = playlistColumn;

        setPreferredWidth(playlistColumn.getWidth());
        setHeaderRenderer(this);

        Border headerBorder = UIManager.getBorder("TableHeader.cellBorder");
        headerLabel = new JLabel(playlistColumn.getDisplayName(), JLabel.CENTER);
        headerLabel.setBorder(headerBorder);
        setHeaderValue(headerLabel);

        skinChanged();
        SubstanceLookAndFeel.registerSkinChangeListener(this);
    }

    /**
     * @return the name
     */
    public String getName() {
        return playlistColumn.getDisplayName();
    }

    /**
     * @return the playlistColumn
     */
    public PlaylistColumn getPlaylistColumn() {
        return playlistColumn;
    }

    /**
     * @return the sortOrderUp
     */
    public boolean isSortOrderUp() {
        return sortOrderUp;
    }

    /**
     * @param sortOrderUp the sortOrderUp to set
     */
    public void setSortOrderUp(boolean sortOrderUp) {
        this.sortOrderUp = sortOrderUp;

        if (sortOrderUp) {
            headerLabel.setIcon(UP_ICON);
        } else {
            headerLabel.setIcon(DOWN_ICON);
        }
    }

    /**
     * @return the comparator
     */
    public Comparator<PlaylistItem> getComparator() {
        Comparator<PlaylistItem> comparator = getPlaylistColumn().getComparator();
        return (sortOrderUp) ? comparator : Collections.reverseOrder(comparator);
    }

    /**
     * Reset the current state.
     */
    public void reset() {
        headerLabel.setIcon(null);
        sortOrderUp = false;
    }
    
    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        playlistColumn.setWidth(width);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        return (JComponent) value;
    }

    @Override
    public final void skinChanged() {
        Color iconColor = SubstanceLookAndFeel.getCurrentSkin().
                getColorScheme(headerLabel, ComponentState.ENABLED).getForegroundColor();
        UP_ICON = new SortOrderIcon(iconColor, true);
        DOWN_ICON = new SortOrderIcon(iconColor, false);

        if (headerLabel.getIcon() != null) {
            if (sortOrderUp) {
                headerLabel.setIcon(UP_ICON);
            } else {
                headerLabel.setIcon(DOWN_ICON);
            }
        }
    }
}
