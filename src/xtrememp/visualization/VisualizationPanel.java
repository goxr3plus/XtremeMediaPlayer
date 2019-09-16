/**
 * Xtreme Media Player a cross-platform media player. Copyright (C) 2005-2014
 * Besmir Beqiri
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package xtrememp.visualization;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JPanel;
import org.pushingpixels.substance.api.ComponentState;
import org.pushingpixels.substance.api.SubstanceColorScheme;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.pushingpixels.substance.api.skin.SkinChangeListener;
import org.pushingpixels.substance.internal.utils.border.SubstanceBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xtrememp.Settings;
import xtrememp.XtremeMP;
import xtrememp.player.dsp.DigitalSignalProcessor;
import xtrememp.player.dsp.DssContext;

/**
 *
 * @author Besmir Beqiri
 */
public class VisualizationPanel extends JPanel implements DigitalSignalProcessor,
        Runnable, SkinChangeListener {
    
    private static final Logger logger = LoggerFactory.getLogger(VisualizationPanel.class);
    protected final GraphicsConfiguration gc;
    protected final List<VisualizationChangeListener> listeners;
    private AbstractVisualization currentVis;
    private List<AbstractVisualization> visList;
    private Frame fullscreenWindow;
    private GraphicsDevice device;
    private DisplayMode displayMode;
    private BufferStrategy bufferStrategy;
    private final int numBuffers = 2;
    private volatile boolean isFullScreen = false;
    
    public VisualizationPanel() {
        setOpaque(false);
        setBorder(new SubstanceBorder());
        
        gc = GraphicsEnvironment.getLocalGraphicsEnvironment().
                getDefaultScreenDevice().getDefaultConfiguration();
        listeners = new ArrayList<>();
        initVisualizations();
        initFullScreenWindow();
        addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    setFullScreen(true);
                }
            }
        });
        
        skinChanged();
        SubstanceLookAndFeel.registerSkinChangeListener(this);
    }
    
    private void initVisualizations() {
        visList = new ArrayList<>();
        visList.add(new SimpleOscilloscope());
        visList.add(new StereoOscilloscope());
        visList.add(new OscilloscopeLines());
        visList.add(new StereoWaveform());
        visList.add(new Waveform());
        visList.add(new VolumeMeter());
        visList.add(new WaterBalloons());
        visList.add(new Stereograph());
        visList.add(new SpectrumBars());
        visList.add(new FullSpectrum());
        visList.add(new WhiteFullSpectrum());
        visList.add(new QuarterSpectrum());
        visList.add(new WhiteQuarterSpectrum());
        visList.add(new FullSpectrogram());
        visList.add(new FullColorSpectrogram());
        visList.add(new QuarterSpectrogram());
        visList.add(new QuarterColorSpectrogram());
        visList.add(new PianoRoll());

        String visDisplayName = Settings.getVisualization();
        //Pick a default visualization in case we can't restaure to a previous one
        AbstractVisualization nextVis = visList.get(0);
        for (AbstractVisualization vis : visList) {
            if (vis.getDisplayName().equals(visDisplayName)) {
                nextVis = vis;
                break;
            }
        }
        showVisualization(nextVis, false);
    }
    
    private void initFullScreenWindow() {
        fullscreenWindow = new Frame();
        fullscreenWindow.setUndecorated(true);
        fullscreenWindow.setIgnoreRepaint(true);
        fullscreenWindow.setResizable(false);
        fullscreenWindow.setBackground(Color.black);
        fullscreenWindow.setAlwaysOnTop(true);
        fullscreenWindow.setFocusable(true);
        fullscreenWindow.addKeyListener(new KeyAdapter() {
            
            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ESCAPE:
                        setFullScreen(false);
                        break;
                    case KeyEvent.VK_LEFT:
                        prevVisualization();
                        break;
                    case KeyEvent.VK_RIGHT:
                        nextVisualization();
                        break;
                    default:
                        break;
                }
            }
        });
        fullscreenWindow.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    setFullScreen(false);
                }
            }
        });
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        device = env.getDefaultScreenDevice();
        displayMode = device.getDisplayMode();
    }
    
    public boolean isFullScreen() {
        return isFullScreen;
    }
    
    public void setFullScreen(boolean flag) {
        XtremeMP.getInstance().getMainFrame().setVisible(!flag);
        if (flag && device.isFullScreenSupported()) {
            device.setFullScreenWindow(fullscreenWindow);
            validate();
            fullscreenWindow.createBufferStrategy(numBuffers);
            if (device.isDisplayChangeSupported()) {
                device.setDisplayMode(displayMode);
                setSize(new Dimension(displayMode.getWidth(), displayMode.getHeight()));
            }
            isFullScreen = true;
        } else {
            isFullScreen = false;
            device.setFullScreenWindow(null);
            fullscreenWindow.dispose();
        }
    }
    
    public List<AbstractVisualization> getVisualizationSet() {
        return visList;
    }
    
    public void showVisualization(AbstractVisualization newVis, boolean fireEvent) {
        if (newVis == null) {
            throw new IllegalArgumentException("Visualization is null.");
        }
        currentVis = newVis;
        repaint();
        Settings.setVisualization(currentVis.getDisplayName());
        if (fireEvent) {
            fireVisualizationChangedEvent();
        }
    }
    
    public AbstractVisualization prevVisualization() {
        if (currentVis != null && !visList.isEmpty()) {
            int preVisIndex = visList.indexOf(currentVis) - 1;
            if (preVisIndex == -1) {
                preVisIndex = visList.size() - 1;
            }
            AbstractVisualization prevVis = visList.get(preVisIndex);
            showVisualization(prevVis, true);
            return prevVis;
        }
        return null;
    }
    
    public AbstractVisualization nextVisualization() {
        if (currentVis != null && !visList.isEmpty()) {
            int nextVisIndex = visList.indexOf(currentVis) + 1;
            if (nextVisIndex == visList.size()) {
                nextVisIndex = 0;
            }
            AbstractVisualization nextVis = visList.get(nextVisIndex);
            showVisualization(nextVis, true);
            return nextVis;
        }
        return null;
    }
    
    public void addVisualizationChangeListener(VisualizationChangeListener listener) {
        if (listener == null) {
            return;
        }
        listeners.add(listener);
        logger.info("Visualization change listener added");
    }
    
    public void removeVisualizationChangedListener(VisualizationChangeListener listener) {
        if (listener == null) {
            return;
        }
        listeners.remove(listener);
        logger.info("Visualization change listener removed");
    }

    /**
     * Notifies all listeners that selected visualization has changed.
     */
    private void fireVisualizationChangedEvent() {
        VisualizationEvent event = new VisualizationEvent(this, currentVis);
        listeners.stream().forEach(listener -> {
            listener.visualizationChanged(event);
        });
        logger.info("Visualization changed: {}", currentVis);
    }
    
    @Override
    public void init(int sampleSize, SourceDataLine sourceDataLine) {
        visList.stream().forEach(vis -> {
            vis.init(sampleSize, sourceDataLine);
        });
    }
    
    @Override
    public void process(DssContext dssContext) {
        int width = getWidth();
        int height = getHeight();
        currentVis.checkBuffImage(gc, width, height);
        currentVis.render(dssContext, currentVis.getBuffGraphics(), width, height);
        if (isFullScreen) {
//            Dimension size = fullscreenWindow.getSize();
            bufferStrategy = fullscreenWindow.getBufferStrategy();
            Graphics2D g2d = null;
            try {
                g2d = (Graphics2D) bufferStrategy.getDrawGraphics();
                for (int i = 0; i < numBuffers; i++) {
                    if (!bufferStrategy.contentsLost()) {
                        if (currentVis != null) {
//                            setSize(size);
                            g2d.drawImage(currentVis.getBuffImage(), 0, 0, this);
                        }
                    }
                    bufferStrategy.show();
                }
            } finally {
                if (g2d != null) {
                    g2d.dispose();
                }
            }
        } else {
            EventQueue.invokeLater(this);
        }
    }
    
    @Override
    public void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();
        currentVis.checkBuffImage(gc, width, height);
        g.drawImage(currentVis.getBuffImage(), 0, 0, this);
    }
    
    @Override
    public void run() {
        repaint();
    }
    
    @Override
    public final void skinChanged() {
        SubstanceColorScheme colorScheme = SubstanceLookAndFeel.getCurrentSkin().
                getColorScheme(this, ComponentState.ENABLED);
        
        boolean colorSchemeDark = colorScheme.isDark();
        Color bgColor = colorScheme.getBackgroundFillColor();
        Color fgColor = colorScheme.getForegroundColor();
        
        for (AbstractVisualization v : visList) {
            v.setColorSchemeDark(colorSchemeDark);
            v.setBackgroundColor(bgColor);
            v.setForegroundColor(fgColor);
            repaint();
        }
    }
}
