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
package xtrememp;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import xtrememp.player.dsp.DigitalSignalSynchronizer;
import xtrememp.ui.button.PopupButton;
import xtrememp.util.Utilities;
import static xtrememp.util.Utilities.tr;
import xtrememp.visualization.AbstractVisualization;
import xtrememp.visualization.VisualizationChangeListener;
import xtrememp.visualization.VisualizationEvent;
import xtrememp.visualization.VisualizationPanel;

/**
 *
 * @author Besmir Beqiri
 */
public final class VisualizationManager extends JPanel implements ActionListener,
        VisualizationChangeListener {

    private final String PREV_VIS_ACTION = "prevVisAction";
    private final String NEXT_VIS_ACTION = "nextVisAction";
    private DigitalSignalSynchronizer dss;
    private JPopupMenu selectionMenu;
    private JButton fullScreenButton;
    private JButton prevVisButton;
    private JButton nextVisButton;
    private PopupButton visMenuButton;
    private ButtonGroup visButtonGroup;
    private VisualizationPanel visPanel;
    private Map<String, AbstractVisualization> visMap;

    public VisualizationManager(DigitalSignalSynchronizer dss) {
        super(new BorderLayout());
        this.dss = dss;
        initComponents();
    }

    private void initComponents() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        fullScreenButton = new JButton(Utilities.VIEW_FULLSCREEN_ICON);
        fullScreenButton.setToolTipText(tr("MainFrame.VisualizationManager.ViewFullscreen"));
        fullScreenButton.addActionListener(this);
        toolBar.add(fullScreenButton);
        toolBar.addSeparator();
        prevVisButton = new JButton(Utilities.GO_PREVIOUS_ICON);
        prevVisButton.setToolTipText(tr("MainFrame.VisualizationManager.PreviousVisualization"));
        prevVisButton.addActionListener(this);
        prevVisButton.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK), PREV_VIS_ACTION);
        prevVisButton.getActionMap().put(PREV_VIS_ACTION, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                visPanel.prevVisualization();
            }
        });
        toolBar.add(prevVisButton);
        nextVisButton = new JButton(Utilities.GO_NEXT_ICON);
        nextVisButton.setToolTipText(tr("MainFrame.VisualizationManager.NextVisualization"));
        nextVisButton.addActionListener(this);
        nextVisButton.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK), NEXT_VIS_ACTION);
        nextVisButton.getActionMap().put(NEXT_VIS_ACTION, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                visPanel.nextVisualization();
            }
        });
        toolBar.add(nextVisButton);
        toolBar.addSeparator();
        visMenuButton = new PopupButton(Utilities.MENU_ICON);
        visMenuButton.setToolTipText(tr("MainFrame.VisualizationManager.VisualizationsMenu"));
        selectionMenu = visMenuButton.getPopupMenu();
        visButtonGroup = new ButtonGroup();
        visPanel = new VisualizationPanel();
        visPanel.addVisualizationChangeListener(this);
        visMap = new HashMap<String, AbstractVisualization>();
        for (AbstractVisualization vis : visPanel.getVisualizationSet()) {
            String visDisplayName = vis.getDisplayName();
            visMap.put(visDisplayName, vis);
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(visDisplayName);
            menuItem.setSelected(visDisplayName.equals(Settings.getVisualization()));
            menuItem.addActionListener(this);
            visButtonGroup.add(menuItem);
            selectionMenu.add(menuItem);
        }
        toolBar.add(visMenuButton);
        this.add(toolBar, BorderLayout.NORTH);
        this.add(visPanel, BorderLayout.CENTER);
    }

    public void setDssEnabled(boolean flag) {
        if (flag) {
            dss.add(visPanel);
        } else {
            dss.remove(visPanel);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source.equals(fullScreenButton)) {
            visPanel.setFullScreen(true);
        } else if (source.equals(prevVisButton)) {
            visPanel.prevVisualization();
        } else if (source.equals(nextVisButton)) {
            visPanel.nextVisualization();
        } else {
            visPanel.showVisualization(visMap.get(e.getActionCommand()), false);
        }
    }

    @Override
    public void visualizationChanged(VisualizationEvent e) {
        String visDisplayName = e.getVisualization().getDisplayName();
        for (Enumeration<AbstractButton> abEnum = visButtonGroup.getElements(); abEnum.hasMoreElements();) {
            AbstractButton aButton = abEnum.nextElement();
            if (aButton.getText().equals(visDisplayName)) {
                aButton.setSelected(true);
                break;
            }
        }
    }
}
