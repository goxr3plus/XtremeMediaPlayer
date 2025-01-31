/**
 * Xtreme Media Player a cross-platform media player.
 * Copyright (C) 2005-2010 Besmir Beqiri
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
package xtrememp.ui.button;

import java.awt.Dimension;
import javax.swing.JButton;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import xtrememp.util.Utilities;
import xtrememp.ui.button.shaper.PlayPauseButtonShaper;

/**
 *
 * @author Besmir Beqiri
 */
public class PlayPauseButton extends JButton {

    public PlayPauseButton() {
        super();
        setIcon(Utilities.MEDIA_PLAY_ICON);
        setPreferredSize(new Dimension(40, 40));
        putClientProperty(SubstanceLookAndFeel.BUTTON_SHAPER_PROPERTY, new PlayPauseButtonShaper());
    }
    
    public void setPlayIcon() {
        setIcon(Utilities.MEDIA_PLAY_ICON);
    }
    
    public void setPauseIcon() {
        setIcon(Utilities.MEDIA_PAUSE_ICON);
    }
}
