/**
 * Xtreme Media Player a cross-platform media player. 
 * Copyright (C) 2005-2014 Besmir Beqiri
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
package xtrememp;

import java.awt.CardLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.miginfocom.swing.MigLayout;
import xtrememp.player.audio.AudioPlayer;
import xtrememp.player.audio.PlaybackEvent;
import xtrememp.player.audio.PlaybackListener;
import xtrememp.player.audio.PlayerException;
import xtrememp.playlist.Playlist;
import xtrememp.playlist.PlaylistEvent;
import xtrememp.playlist.PlaylistException;
import xtrememp.playlist.PlaylistIO;
import xtrememp.playlist.PlaylistItem;
import xtrememp.playlist.PlaylistListener;
import xtrememp.tag.TagInfo;
import xtrememp.ui.button.NextButton;
import xtrememp.ui.button.PlayPauseButton;
import xtrememp.ui.button.PreviousButton;
import xtrememp.ui.button.StopButton;
import xtrememp.ui.button.VolumeButton;
import xtrememp.ui.label.BusyLabel;
import xtrememp.ui.skin.GFXUIListener;
import xtrememp.ui.slider.SeekSlider;
import xtrememp.update.SoftwareUpdate;
import xtrememp.update.Version;
import xtrememp.util.AbstractSwingWorker;
import xtrememp.util.LanguageBundle;
import xtrememp.util.OSUtils;
import xtrememp.util.Utilities;
import static xtrememp.util.Utilities.tr;
import xtrememp.util.file.AudioFileFilter;
import xtrememp.util.file.PlaylistFileFilter;

import javax.sound.sampled.AudioSystem;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.pushingpixels.lafwidget.animation.AnimationConfigurationManager;
import org.pushingpixels.lafwidget.animation.AnimationFacet;
import org.pushingpixels.substance.api.DecorationAreaType;
import org.pushingpixels.substance.api.SubstanceConstants;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.melloware.jintellitype.IntellitypeListener;
import com.melloware.jintellitype.JIntellitype;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

/**
 * XtremeMP main class. Special thanks to rom1dep for the changes applied to
 * this class.
 *
 * @author Besmir Beqiri
 */
public final class XtremeMP implements ActionListener, ControlListener,
        PlaybackListener, PlaylistListener, IntellitypeListener, GFXUIListener {

    private static final Logger logger = LoggerFactory.getLogger(XtremeMP.class);
    private final AudioFileFilter audioFileFilter = AudioFileFilter.INSTANCE;
    private final PlaylistFileFilter playlistFileFilter = PlaylistFileFilter.INSTANCE;
    private final Version currentVersion = Version.getCurrentVersion();
    private static XtremeMP instance;
    private JFrame mainFrame;
    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenu playerMenu;
    private JMenu playModeSubMenu;
    private JMenu viewMenu;
    private JMenu helpMenu;
    private JMenuItem openMenuItem;
    private JMenuItem openURLMenuItem;
    private JMenuItem openPlaylistMenuItem;
    private JMenuItem savePlaylistMenuItem;
    private JMenuItem preferencesMenuItem;
    private JMenuItem exitMenuItem;
    private JMenuItem nextMenuItem;
    private JMenuItem playPauseMenuItem;
    private JMenuItem stopMenuItem;
    private JMenuItem previousMenuItem;
    private JMenuItem randomizePlaylistMenuItem;
    private JRadioButtonMenuItem playlistManagerMenuItem;
    private JRadioButtonMenuItem visualizationMenuItem;
    private JRadioButtonMenuItem playModeRepeatNoneMenuItem;
    private JRadioButtonMenuItem playModeRepeatOneMenuItem;
    private JRadioButtonMenuItem playModeRepeatAllMenuItem;
    private JRadioButtonMenuItem playModeShuffleMenuItem;
    private JMenuItem updateMenuItem;
    private JMenuItem aboutMenuItem;
    private BusyLabel busyLabel;
    private JPanel mainPanel;
    private VisualizationManager visualizationManager;
    private JPanel controlPanel;
    private AudioPlayer audioPlayer;
    private Playlist playlist;
    private PlaylistManager playlistManager;
    private StopButton stopButton;
    private PreviousButton previousButton;
    private PlayPauseButton playPauseButton;
    private NextButton nextButton;
    private VolumeButton volumeButton;
    private JSlider volumeSlider;
    private JLabel timeLabel;
    private JLabel statusLabel;
    private JLabel playModeLabel;
    private SeekSlider seekSlider;
    private PlaylistItem currentPli;

    private XtremeMP() {
    }

    public static XtremeMP getInstance() {
        if (instance == null) {
            instance = new XtremeMP();
        }
        return instance;
    }

    public JFrame getMainFrame() {
        return mainFrame;
    }

    public BusyLabel getBusyLabel() {
        return busyLabel;
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public void init(String[] args) {
        // Process arguments
        Option urlOption = OptionBuilder.withArgName("URL").hasArg().withDescription(
                "Open from URL").create("url");
        Options options = new Options();
        options.addOption("help", false, "print help information");
        options.addOption("version", false, "print the version information");
        options.addOption(urlOption);
        CommandLineParser parser = new PosixParser();
        try {
            final CommandLine line = parser.parse(options, args);
            if (line.hasOption("h") || line.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("xtrememp", options);
                System.exit(0);
            }
            if (line.hasOption("version")) {
                StringBuilder appVersion = new StringBuilder(tr("Application.title"));
                appVersion.append(" ").append(currentVersion);
                appVersion.append(" [").append(tr("Dialog.About.Codename"));
                appVersion.append(" ").append(tr("Application.version.codename"));
                appVersion.append("]");
                System.out.println(appVersion.toString());
                System.exit(0);
            }

            // Initialize JIntellitype
            if (OSUtils.IS_OS_WINDOWS) {
                String libraryLocation = (OSUtils.IS_ARCH_X64)
                        ? "\\native\\JIntellitype64.dll" : "\\native\\JIntellitype.dll";
                JIntellitype.setLibraryLocation(OSUtils.USER_DIR + libraryLocation);
                if (JIntellitype.isJIntellitypeSupported()) {
                    JIntellitype.getInstance().addIntellitypeListener(this);
                }
            }

            // Initialize audio engine
            audioPlayer = new AudioPlayer();
            audioPlayer.addPlaybackListener(this);
            String mixerName = Settings.getMixerName();
            if (!Utilities.isNullOrEmpty(mixerName)) {
                audioPlayer.setMixerName(mixerName);
            }

            // Launch gui
            EventQueue.invokeLater(() -> {
                if (!OSUtils.IS_OS_WINDOWS_7 && !OSUtils.IS_OS_WINDOWS_VISTA) {
                    JFrame.setDefaultLookAndFeelDecorated(true);
                    JDialog.setDefaultLookAndFeelDecorated(true);
                }
                UIManager.put(SubstanceLookAndFeel.FOCUS_KIND, SubstanceConstants.FocusKind.NONE);
                SubstanceLookAndFeel.setSkin(Settings.getSkin());
                
                //Turn on Substance animations if required
                guiEffectsStateChanged(Settings.isUIEffectsEnabled());
                
                StringBuilder appTitle = new StringBuilder(tr("Application.title"));
                appTitle.append(" ").append(currentVersion);
                mainFrame = new JFrame(appTitle.toString());
                mainFrame.setIconImages(Utilities.getIconImages());
                
                Rectangle boundsRect = Settings.getMainFrameBounds();
                if (boundsRect.x <= 0 || boundsRect.y <= 0) {
                    mainFrame.setSize(boundsRect.width, boundsRect.height);
                    // Center the frame.
                    mainFrame.setLocationRelativeTo(null);
                } else {
                    mainFrame.setBounds(boundsRect);
                }
                mainFrame.addWindowListener(new WindowAdapter() {
                    
                    @Override
                    public void windowClosing(WindowEvent ev) {
                        exit();
                    }
//                    @Override
//                    public void windowIconified(WindowEvent e) {
//                        if (Toolkit.getDefaultToolkit().isFrameStateSupported(JFrame.ICONIFIED)) {
//                            getMainFrame().setExtendedState(JFrame.ICONIFIED);
//                        }
//                        getMainFrame().setVisible(false);
//                    }
//
//                    @Override
//                    public void windowDeiconified(WindowEvent e) {
//                        if (Toolkit.getDefaultToolkit().isFrameStateSupported(JFrame.NORMAL)) {
//                            getMainFrame().setExtendedState(JFrame.NORMAL);
//                        }
//                        getMainFrame().setVisible(true);
//                    }
                });
                createMenuBar();
                createPanels();
                
                // Set a minimum size for this frame.
                mainFrame.setMinimumSize(new Dimension(532, 244));
                
                playlist = playlistManager.getPlaylist();
                playlist.addPlaylistListener(XtremeMP.this);
                
                // Restore playlist settings : play mode
                playlist.setPlayMode(Settings.getPlayMode());
                
                List<String> argList = Arrays.asList(line.getArgs());
                List<File> fileList = new ArrayList<>();
                if (argList.isEmpty()) {
                    File playlistFile = new File(Settings.getCacheDir(), Utilities.DEFAULT_PLAYLIST);
                    if (playlistFile.exists()) {
                        playlistManager.loadPlaylist(playlistFile.getAbsolutePath());
                    }
                } else {
                    playlistManager.setFirstLoad(true);
                    if (argList.size() == 1) {
                        File file = new File(argList.get(0));
                        if (audioFileFilter.accept(file)) {
                            fileList.add(file);
                            playlistManager.addFiles(fileList, true);
                        } else if (playlistFileFilter.accept(file)) {
                            playlistManager.loadPlaylist(file.getAbsolutePath());
                        }
                    } else {
                        for (String arg : argList) {
                            File file = new File(arg);
                            if (audioFileFilter.accept(file)) {
                                fileList.add(file);
                            }
                        }
                        playlistManager.addFiles(fileList, true);
                    }
                }
                
                mainFrame.setVisible(true);
            });
        } catch (ParseException exp) {
            logger.error("Parsing failed.", exp.getMessage());
        }
    }

    public static void main(String... args) throws Exception {
        // Load Settings
        Settings.loadSettings();
        // Configure logback
        Settings.configureLogback();

        // Enable uncaught exception catching.
        Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable e) -> {
            logger.error(t.getName(), e);
        });

        // Close error stream
        System.err.close();
        // First, look up for other instances running
        if (!MultipleInstancesHandler.getInstance().isFirstInstance()) {
            MultipleInstancesHandler.getInstance().sendArgumentsToFirstInstance(args);
        } else {
            // NORMAL APPLICATION STARTUP
            // Set language
            Locale locale = Utilities.getLanguages()[Settings.getLanguageIndex()];
            Locale.setDefault(locale);
            LanguageBundle.setLanguage(locale);

            // Animation configurations
            AnimationConfigurationManager.getInstance().disallowAnimations(AnimationFacet.ICON_GLOW, JTable.class);
            AnimationConfigurationManager.getInstance().disallowAnimations(AnimationFacet.ROLLOVER, JTable.class);
            AnimationConfigurationManager.getInstance().disallowAnimations(AnimationFacet.SELECTION, JTable.class);

            // Init
            getInstance().init(args);

            // Check for updates
            if (Settings.isAutomaticUpdatesEnabled()) {
                // wait 5 sec
                SoftwareUpdate.scheduleCheckForUpdates(5 * 1000);
            }
        }
    }

    protected void exit() {
        // Save current settings
        Settings.setMainFrameBounds(mainFrame.getBounds());
        Settings.setPlaylistPosition(playlist.getCursorPosition());
        Settings.setPlaylistColumns(playlistManager.getPlaylistColumns());
        Settings.storeSettings();
        try {
            File playlistFile = new File(Settings.getCacheDir(), Utilities.DEFAULT_PLAYLIST);
            PlaylistIO.saveXSPF(playlist, playlistFile.getAbsolutePath());
        } catch (PlaylistException ex) {
            logger.error("Can't save default playlist", ex);
        }
        // Release audio engine resources
        audioPlayer.stop();
        // Clean up all resources used by JIntellitype
        if (JIntellitype.isJIntellitypeSupported()) {
            JIntellitype.getInstance().cleanUp();
        }
        MultipleInstancesHandler.getInstance().dispose();
        logger.info("Exit application...");
        System.exit(0);
    }

    protected void createMenuBar() {
        menuBar = new JMenuBar();

        // File Menu
        String fileMenuStr = tr("MainFrame.Menu.File");
        fileMenu = new JMenu(fileMenuStr);
        fileMenu.setMnemonic(fileMenuStr.charAt(0));

        openMenuItem = new JMenuItem(tr("MainFrame.Menu.File.OpenFile"));
        openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openMenuItem.setIcon(Utilities.FOLDER_ICON);
        openMenuItem.addActionListener(this);
        fileMenu.add(openMenuItem);

        openURLMenuItem = new JMenuItem(tr("MainFrame.Menu.File.OpenURL"));
        openURLMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK));
        openURLMenuItem.setIcon(Utilities.FOLDER_REMOTE_ICON);
        openURLMenuItem.addActionListener(this);
        fileMenu.add(openURLMenuItem);

        fileMenu.addSeparator();

        openPlaylistMenuItem = new JMenuItem(tr("MainFrame.Menu.File.OpenPlaylist"));
        openPlaylistMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        openPlaylistMenuItem.setIcon(Utilities.DOCUMENT_OPEN_ICON);
        openPlaylistMenuItem.addActionListener(this);
        fileMenu.add(openPlaylistMenuItem);

        savePlaylistMenuItem = new JMenuItem(tr("MainFrame.Menu.File.SavePlaylist"));
        savePlaylistMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        savePlaylistMenuItem.setIcon(Utilities.DOCUMENT_SAVE_ICON);
        savePlaylistMenuItem.addActionListener(this);
        fileMenu.add(savePlaylistMenuItem);

        fileMenu.addSeparator();

        preferencesMenuItem = new JMenuItem(tr("MainFrame.Menu.File.Preferences"));
        preferencesMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));
        preferencesMenuItem.addActionListener(this);
        fileMenu.add(preferencesMenuItem);

        fileMenu.addSeparator();

        exitMenuItem = new JMenuItem(tr("MainFrame.Menu.File.Exit"));
        exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        exitMenuItem.addActionListener(this);
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        // Player Menu
        String playerMenuStr = tr("MainFrame.Menu.Player");
        playerMenu = new JMenu(playerMenuStr);
        playerMenu.setMnemonic(playerMenuStr.charAt(0));

        playPauseMenuItem = new JMenuItem(tr("MainFrame.Menu.Player.Play"));
        playPauseMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
        playPauseMenuItem.addActionListener(this);
        playerMenu.add(playPauseMenuItem);

        stopMenuItem = new JMenuItem(tr("MainFrame.Menu.Player.Stop"));
        stopMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK));
//        stopMenuItem.setIcon(Utilities.MEDIA_STOP_ICON);
        stopMenuItem.setEnabled(false);
        stopMenuItem.addActionListener(this);
        playerMenu.add(stopMenuItem);

        previousMenuItem = new JMenuItem(tr("MainFrame.Menu.Player.Previous"));
        previousMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
//        previousMenuItem.setIcon(Utilities.MEDIA_PREVIOUS_ICON);
        previousMenuItem.setEnabled(false);
        previousMenuItem.addActionListener(this);
        playerMenu.add(previousMenuItem);

        nextMenuItem = new JMenuItem(tr("MainFrame.Menu.Player.Next"));
        nextMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
//        nextMenuItem.setIcon(Utilities.MEDIA_NEXT_ICON);
        nextMenuItem.setEnabled(false);
        nextMenuItem.addActionListener(this);
        playerMenu.add(nextMenuItem);

        playerMenu.addSeparator();

        //PlayMode submenu
        String playModeSubMenuStr = tr("MainFrame.Menu.Player.PlayMode");
        playModeSubMenu = new JMenu(playModeSubMenuStr);

        playModeRepeatNoneMenuItem = new JRadioButtonMenuItem(tr("MainFrame.Menu.Player.PlayMode.RepeatNone"));
        playModeRepeatNoneMenuItem.setIcon(Utilities.PLAYLIST_REPEAT_NONE_ICON);
        playModeRepeatNoneMenuItem.addActionListener(this);
        playModeSubMenu.add(playModeRepeatNoneMenuItem);
        playModeRepeatOneMenuItem = new JRadioButtonMenuItem(tr("MainFrame.Menu.Player.PlayMode.RepeatOne"));
        playModeRepeatOneMenuItem.setIcon(Utilities.PLAYLIST_REPEAT_ONE_ICON);
        playModeRepeatOneMenuItem.addActionListener(this);
        playModeSubMenu.add(playModeRepeatOneMenuItem);
        playModeRepeatAllMenuItem = new JRadioButtonMenuItem(tr("MainFrame.Menu.Player.PlayMode.RepeatAll"));
        playModeRepeatAllMenuItem.setIcon(Utilities.PLAYLIST_REPEAT_ALL_ICON);
        playModeRepeatAllMenuItem.addActionListener(this);
        playModeSubMenu.add(playModeRepeatAllMenuItem);
        playModeShuffleMenuItem = new JRadioButtonMenuItem(tr("MainFrame.Menu.Player.PlayMode.Shuffle"));
        playModeShuffleMenuItem.setIcon(Utilities.PLAYLIST_SHUFFLE_ICON);
        playModeShuffleMenuItem.addActionListener(this);
        playModeSubMenu.add(playModeShuffleMenuItem);

        ButtonGroup playModeBG = new ButtonGroup();
        playModeBG.add(playModeRepeatNoneMenuItem);
        playModeBG.add(playModeRepeatOneMenuItem);
        playModeBG.add(playModeRepeatAllMenuItem);
        playModeBG.add(playModeShuffleMenuItem);

        playerMenu.add(playModeSubMenu);
        playerMenu.addSeparator();

        randomizePlaylistMenuItem = new JMenuItem(tr("MainFrame.Menu.Player.Randomize"));
        randomizePlaylistMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
        randomizePlaylistMenuItem.setEnabled(false);
        randomizePlaylistMenuItem.addActionListener(this);
        playerMenu.add(randomizePlaylistMenuItem);

        menuBar.add(playerMenu);

        // View Menu
        String viewMenuStr = tr("MainFrame.Menu.View");
        viewMenu = new JMenu(viewMenuStr);
        viewMenu.setMnemonic(viewMenuStr.charAt(0));

        playlistManagerMenuItem = new JRadioButtonMenuItem(tr("MainFrame.Menu.View.PlaylistManager"));
        playlistManagerMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
        playlistManagerMenuItem.addActionListener(this);
        viewMenu.add(playlistManagerMenuItem);

        visualizationMenuItem = new JRadioButtonMenuItem(tr("MainFrame.Menu.View.Visualizations"));
        visualizationMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK));
        visualizationMenuItem.addActionListener(this);
        viewMenu.add(visualizationMenuItem);

        ButtonGroup viewBG = new ButtonGroup();
        viewBG.add(playlistManagerMenuItem);
        viewBG.add(visualizationMenuItem);

        menuBar.add(viewMenu);

        // Help menu
        String helpMenuStr = tr("MainFrame.Menu.Help");
        helpMenu = new JMenu(helpMenuStr);
        helpMenu.setMnemonic(helpMenuStr.charAt(0));

        updateMenuItem = new JMenuItem(tr("MainFrame.Menu.Help.CheckForUpdates"));
        updateMenuItem.addActionListener(this);
        helpMenu.add(updateMenuItem);
        helpMenu.addSeparator();

        aboutMenuItem = new JMenuItem(tr("MainFrame.Menu.Help.About"));
        aboutMenuItem.addActionListener(this);
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        menuBar.add(Box.createHorizontalGlue());
        busyLabel = new BusyLabel(new Dimension(20, 20));
        menuBar.add(busyLabel);
        menuBar.add(Box.createHorizontalStrut(8));

        mainFrame.setJMenuBar(menuBar);
    }

    protected void createPanels() {
        JPanel framePanel = new JPanel(new MigLayout("fill"));

        mainPanel = new JPanel(new CardLayout());
        playlistManager = new PlaylistManager(this);
        visualizationManager = new VisualizationManager(audioPlayer.getDSS());
        if (Settings.getLastView().equals(Utilities.VISUALIZATION_PANEL)) {
            visualizationManager.setDssEnabled(true);
            mainPanel.add(visualizationManager, Utilities.VISUALIZATION_PANEL);
            mainPanel.add(playlistManager, Utilities.PLAYLIST_MANAGER);
            visualizationMenuItem.setSelected(true);
        } else {
            mainPanel.add(playlistManager, Utilities.PLAYLIST_MANAGER);
            mainPanel.add(visualizationManager, Utilities.VISUALIZATION_PANEL);
            playlistManagerMenuItem.setSelected(true);
        }
        framePanel.add(mainPanel, "grow");

        JPanel southPanel = new JPanel(new MigLayout("fill", "[center]"));
        SubstanceLookAndFeel.setDecorationType(southPanel, DecorationAreaType.TOOLBAR);
        seekSlider = new SeekSlider(this);
        seekSlider.setEnabled(false);
        southPanel.add(seekSlider, "north, gap 4 4 1 0");

        controlPanel = new JPanel(new MigLayout("gap 0, ins 0", "[center]"));
        controlPanel.setOpaque(false);
        stopButton = new StopButton();
        stopButton.setEnabled(false);
        stopButton.addActionListener(this);
        controlPanel.add(stopButton);
        previousButton = new PreviousButton();
        previousButton.setEnabled(false);
        previousButton.addActionListener(this);
        controlPanel.add(previousButton);
        playPauseButton = new PlayPauseButton();
        playPauseButton.addActionListener(this);
        controlPanel.add(playPauseButton, "height pref!");
        nextButton = new NextButton();
        nextButton.setEnabled(false);
        nextButton.addActionListener(this);
        controlPanel.add(nextButton);
        volumeButton = new VolumeButton(Utilities.MIN_GAIN,
                Utilities.MAX_GAIN, Settings.getGain(), Settings.isMuted());
        volumeButton.addMouseWheelListener((MouseWheelEvent e) -> {
            try {
                int volumeValue = volumeSlider.getValue() - 5 * e.getWheelRotation();
                int volumeMin = volumeSlider.getMinimum();
                int volumeMax = volumeSlider.getMaximum();
                if (volumeValue < volumeMin) {
                    volumeValue = volumeMin;
                } else if (volumeValue > volumeMax) {
                    volumeValue = volumeMax;
                }
                volumeButton.setVolumeIcon(volumeValue);
                volumeSlider.setValue(volumeValue);
                audioPlayer.setGain(volumeValue / 100.0F);
                Settings.setGain(volumeValue);
            } catch (PlayerException ex) {
                logger.debug(ex.getMessage(), ex);
            }
        });
        JPopupMenu volumePopupMenu = volumeButton.getPopupMenu();
        volumeSlider = new JSlider(JSlider.VERTICAL, Utilities.MIN_GAIN,
                Utilities.MAX_GAIN, Settings.getGain());
        volumeSlider.setMajorTickSpacing(25);
        volumeSlider.setMinorTickSpacing(5);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(true);
        volumeSlider.addChangeListener((ChangeEvent e) -> {
            if (volumeSlider.getValueIsAdjusting()) {
                try {
                    int volumeValue = volumeSlider.getValue();
                    volumeButton.setVolumeIcon(volumeValue);
                    audioPlayer.setGain(volumeValue / 100.0F);
                    Settings.setGain(volumeValue);
                } catch (PlayerException ex) {
                    logger.debug(ex.getMessage(), ex);
                }
            }
        });
        volumeSlider.setEnabled(!Settings.isMuted());
        JPanel volumePanel = new JPanel(new MigLayout("fill"));
        JLabel volumeLabel = new JLabel(tr("MainFrame.Menu.Player.Volume"), JLabel.CENTER);
        volumeLabel.setFont(volumeLabel.getFont().deriveFont(Font.BOLD));
        volumePanel.add(volumeLabel, "north");
        volumePanel.add(volumeSlider);
        JCheckBox muteCheckBox = new JCheckBox(tr("MainFrame.Menu.Player.Mute"));
        muteCheckBox.setSelected(Settings.isMuted());
        muteCheckBox.addItemListener((ItemEvent e) -> {
            try {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    volumeSlider.setEnabled(false);
                    volumeButton.setVolumeMutedIcon();
                    audioPlayer.setMuted(true);
                    Settings.setMuted(true);
                } else {
                    volumeSlider.setEnabled(true);
                    volumeButton.setVolumeIcon(Settings.getGain());
                    audioPlayer.setMuted(false);
                    Settings.setMuted(false);
                }
            } catch (PlayerException ex) {
                logger.debug(ex.getMessage(), ex);
            }
        });
        volumePanel.add(muteCheckBox, "south");
        volumePopupMenu.add(volumePanel);
        controlPanel.add(volumeButton);
        southPanel.add(controlPanel, "gap 0 0 2 5");

        JPanel statusBar = new JPanel(new MigLayout("ins 2 0 2 0"));
        SubstanceLookAndFeel.setDecorationType(statusBar, DecorationAreaType.FOOTER);
        timeLabel = new JLabel(Utilities.ZERO_TIMER);
        timeLabel.setFont(timeLabel.getFont().deriveFont(Font.BOLD));
        statusBar.add(timeLabel, "gap 6 6 0 0, west");
        statusBar.add(new JSeparator(SwingConstants.VERTICAL), "hmin 16");
        statusLabel = new JLabel();
        statusBar.add(statusLabel, "gap 0 2 0 0, wmin 0, push");
        statusBar.add(new JSeparator(SwingConstants.VERTICAL), "hmin 16");
        playModeLabel = new JLabel();
        playModeLabel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                Playlist.PlayMode[] playModes = Playlist.PlayMode.values();
                Playlist.PlayMode playMode = playlist.getPlayMode();
                int ordinal = playMode.ordinal();
                playlist.setPlayMode(playModes[(ordinal == playModes.length - 1) ? 0 : ordinal + 1]);
            }
        });
        statusBar.add(playModeLabel, "east, gap 2 2 2 2, width 18!, height 18!");
        southPanel.add(statusBar, "south");
        framePanel.add(southPanel, "south");
        mainFrame.setContentPane(framePanel);
    }

    protected void addToPlaylistAndPlay(List<PlaylistItem> pliList) {
        playlistManager.clearPlaylist();
        playlistManager.add(pliList);
        playlist.begin();
        acOpenAndPlay();
    }

    protected void setStatus(String text) {
        final String status = (text != null) ? text : "";
        if (EventQueue.isDispatchThread()) {
            statusLabel.setText(status);
        } else {
            EventQueue.invokeLater(() -> {
                statusLabel.setText(status);
            });
        }
    }

    protected void enableControlButtons(boolean flag) {
        previousButton.setEnabled(flag);
        previousMenuItem.setEnabled(flag);
        nextButton.setEnabled(flag);
        nextMenuItem.setEnabled(flag);
        randomizePlaylistMenuItem.setEnabled(flag);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source == openMenuItem) {
            playlistManager.addFilesDialog(true);
        } else if (source == openURLMenuItem) {
            String url = JOptionPane.showInputDialog(mainFrame,
                    tr("Dialog.OpenURL.Message"),
                    tr("Dialog.OpenURL"),
                    JOptionPane.INFORMATION_MESSAGE);
            if (url != null && Utilities.startWithProtocol(url)) {
                boolean isPlaylistFile = false;
                for (String ext : PlaylistFileFilter.PlaylistFileExt) {
                    if (url.endsWith(ext)) {
                        isPlaylistFile = true;
                    }
                }
                if (isPlaylistFile) {
                    playlistManager.clearPlaylist();
                    playlistManager.loadPlaylist(url);
                    playlist.begin();
                } else {
                    PlaylistItem newPli = new PlaylistItem(url, url, -1, false);
                    playlistManager.add(newPli);
                    playlist.setCursor(newPli);
                }
                acOpenAndPlay();
            }
        } else if (source == openPlaylistMenuItem) {
            playlistManager.openPlaylistDialog();
        } else if (source == savePlaylistMenuItem) {
            playlistManager.savePlaylistDialog();
        } else if (source == preferencesMenuItem) {
            PreferencesDialog preferencesDialog = new PreferencesDialog(audioPlayer, this);
            preferencesDialog.setVisible(true);
        } else if (source == exitMenuItem) {
            exit();
        } else if (source == playPauseMenuItem || source == playPauseButton) {
            acPlayPause();
        } else if (source == previousMenuItem || source == previousButton) {
            acPrevious();
        } else if (source == nextMenuItem || source == nextButton) {
            acNext();
        } else if (source == randomizePlaylistMenuItem) {
            playlistManager.randomizePlaylist();
        } else if (source == stopMenuItem || source == stopButton) {
            acStop();
        } else if (source == playlistManagerMenuItem) {
            if (visualizationManager.isVisible()) {
                visualizationManager.setDssEnabled(false);
                CardLayout cardLayout = (CardLayout) (mainPanel.getLayout());
                cardLayout.show(mainPanel, Utilities.PLAYLIST_MANAGER);
                playlistManagerMenuItem.setSelected(true);
                Settings.setLastView(Utilities.PLAYLIST_MANAGER);
            }
        } else if (source == visualizationMenuItem) {
            if (playlistManager.isVisible()) {
                visualizationManager.setDssEnabled(true);
                CardLayout cardLayout = (CardLayout) (mainPanel.getLayout());
                cardLayout.show(mainPanel, Utilities.VISUALIZATION_PANEL);
                visualizationMenuItem.setSelected(true);
                Settings.setLastView(Utilities.VISUALIZATION_PANEL);
            }
        } else if (source == updateMenuItem) {
            SoftwareUpdate.checkForUpdates(true);
            SoftwareUpdate.showCheckForUpdatesDialog();
        } else if (source == aboutMenuItem) {
            Object[] options = {tr("Button.Close")};
            Desktop desktop = null;
            if (Desktop.isDesktopSupported()) {
                desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    options = new Object[]{tr("Button.Close"), tr("Button.Website")};
                }
            }
            StringBuffer message = new StringBuffer();
            message.append("<html><b><font color='red' size='5'>").append(tr("Application.title"));
            message.append("</font></b><br>").append(tr("Application.description"));
            message.append("<br>Copyright © 2005-2014 The Xtreme Media Player Project");
            message.append("<br><br><b>").append(tr("Dialog.About.Authors")).append(": </b>").append(tr("Application.authors"));
            message.append("<br><b>").append(tr("Dialog.About.Version")).append(": </b>").append(currentVersion);
            message.append("<br><b>").append(tr("Dialog.About.Codename")).append(": </b>").append(currentVersion.getCodename());
            message.append("<br><b>").append(tr("Dialog.About.ReleaseDate")).append(": </b>").append(currentVersion.getReleaseDate());
            message.append("<br><b>").append(tr("Dialog.About.Homepage")).append(": </b>").append(tr("Application.homepage"));
            message.append("<br><br><b>").append(tr("Dialog.About.JavaVersion")).append(": </b>").append(System.getProperty("java.version"));
            message.append("<br><b>").append(tr("Dialog.About.JavaVendor")).append(": </b>").append(System.getProperty("java.vendor"));
            message.append("<br><b>").append(tr("Dialog.About.JavaHome")).append(": </b>").append(System.getProperty("java.home"));
            message.append("<br><b>").append(tr("Dialog.About.OSName")).append(": </b>").append(System.getProperty("os.name"));
            message.append("<br><b>").append(tr("Dialog.About.OSArch")).append(": </b>").append(System.getProperty("os.arch"));
            message.append("<br><b>").append(tr("Dialog.About.UserName")).append(": </b>").append(System.getProperty("user.name"));
            message.append("<br><b>").append(tr("Dialog.About.UserHome")).append(": </b>").append(System.getProperty("user.home"));
            message.append("<br><b>").append(tr("Dialog.About.UserDir")).append(": </b>").append(System.getProperty("user.dir"));
            message.append("</html>");
            int n = JOptionPane.showOptionDialog(mainFrame, message, tr("Dialog.About"),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                    Utilities.APP_256_ICON, options, options[0]);
            if (n == 1 && desktop != null) {
                try {
                    URL url = new URL(tr("Application.homepage"));
                    desktop.browse(url.toURI());
                } catch (IOException | URISyntaxException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        } else if (source.equals(playModeRepeatNoneMenuItem)) {
            playlist.setPlayMode(Playlist.PlayMode.REPEAT_NONE);
        } else if (source.equals(playModeRepeatOneMenuItem)) {
            playlist.setPlayMode(Playlist.PlayMode.REPEAT_ONE);
        } else if (source.equals(playModeRepeatAllMenuItem)) {
            playlist.setPlayMode(Playlist.PlayMode.REPEAT_ALL);
        } else if (source.equals(playModeShuffleMenuItem)) {
            playlist.setPlayMode(Playlist.PlayMode.SHUFFLE);
        }
    }

    @Override
    public void playbackBuffering(PlaybackEvent pe) {
        setStatus(tr("MainFrame.StatusBar.Buffering"));
    }

    @Override
    public void playbackOpened(PlaybackEvent pe) {
        try {
            audioPlayer.setGain(Settings.getGain() / 100.0F);
            audioPlayer.setMuted(Settings.isMuted());
        } catch (PlayerException ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            if (currentPli != null) {
                if (!currentPli.isFile()) {
                    currentPli.getTagInfo();
                }
                setStatus(currentPli.getFormattedName());
            }
        }

        EventQueue.invokeLater(() -> {
            stopButton.setEnabled(true);
            stopMenuItem.setEnabled(true);
        });
    }

    @Override
    public void playbackEndOfMedia(PlaybackEvent pe) {
        if (playlist.isEmpty()) {
            acStop();
        } else {
            switch (playlist.getPlayMode()) {
                case REPEAT_NONE:
                    if (playlist.getCursorPosition() == playlist.size() - 1) {
                        acStop();
                    } else {
                        acNext();
                    }
                    break;
                case REPEAT_ONE:
                    acStop();
                    acPlayPause();
                    break;
                case REPEAT_ALL:
                    acNext();
                    break;
                case SHUFFLE:
                    acNext();
                    break;
            }
        }
    }

    @Override
    public void playbackPlaying(PlaybackEvent pe) {
        EventQueue.invokeLater(() -> {
            playPauseButton.setPauseIcon();
            playPauseMenuItem.setText(tr("MainFrame.Menu.Player.Pause"));
        });
    }

    @Override
    public void playbackProgress(PlaybackEvent pe) {
        if (currentPli != null && !seekSlider.getValueIsAdjusting() && !seekSlider.isPressed() && mainFrame.isVisible()) {
            acUpdateTime(seekSlider.getOldValue() + Math.round(pe.getPosition() / 1000F));

            // Shoutcast stream title.
            Map properties = pe.getProperties();
            String streamTitleKey = "mp3.shoutcast.metadata.StreamTitle";
            if (!currentPli.isFile() && properties.containsKey(streamTitleKey)) {
                String streamTitle = ((String) properties.get(streamTitleKey)).trim();
                TagInfo tagInfo = currentPli.getTagInfo();
                if (!streamTitle.isEmpty() && (tagInfo != null)) {
                    String sTitle = Utilities.isNullOrEmpty(tagInfo.getTitle()) ? "" : " (" + tagInfo.getTitle() + ")";
                    if (!currentPli.getFormattedName().equals(streamTitle + sTitle)) {
                        currentPli.setFormattedName(streamTitle + sTitle);
                        EventQueue.invokeLater(() -> {
                            playlistManager.refreshRow(playlist.indexOf(currentPli));
                            setStatus(currentPli.getFormattedName());
                        });
                    }
                }
            }
        }
    }

    @Override
    public void playbackPaused(PlaybackEvent pe) {
        EventQueue.invokeLater(() -> {
            playPauseButton.setPlayIcon();
            playPauseMenuItem.setText(tr("MainFrame.Menu.Player.Play"));
        });
    }

    @Override
    public void playbackStopped(PlaybackEvent pe) {
        currentPli = null;
        EventQueue.invokeLater(() -> {
            playPauseButton.setPlayIcon();
            stopButton.setEnabled(false);
            playPauseMenuItem.setText(tr("MainFrame.Menu.Player.Play"));
            stopMenuItem.setEnabled(false);
            acUpdateTime(0);
            statusLabel.setText("");
        });
    }

    @Override
    public void acOpen() {
        PlayerLauncher playerLauncher = new PlayerLauncher(false);
        playerLauncher.execute();
    }

    @Override
    public void acOpenAndPlay() {
        PlayerLauncher playerLauncher = new PlayerLauncher(true);
        playerLauncher.execute();
    }

    @Override
    public void acPrevious() {
        if (!playlist.isEmpty()) {
            playlist.previousCursor();
            acOpenAndPlay();
        }
    }

    @Override
    public void acNext() {
        if (!playlist.isEmpty()) {
            playlist.nextCursor();
            acOpenAndPlay();
        }
    }

    @Override
    public void acPlayPause() {
        try {
            if (playlist.isEmpty()) {
                if ((audioPlayer.getState() == AudioSystem.NOT_SPECIFIED) || (audioPlayer.getState() == AudioPlayer.STOP)) {
                    playlistManager.addFilesDialog(true);
                }
            } else {
                if ((audioPlayer.getState() != AudioPlayer.PLAY) && (audioPlayer.getState() != AudioPlayer.PAUSE) && (playlist.getCursorPosition() == -1)) {
                    playlist.begin();
                }
            }
            switch (audioPlayer.getState()) {
                case AudioPlayer.PLAY:
                    audioPlayer.pause();
                    break;
                case AudioPlayer.INIT:
                    audioPlayer.play();
                    break;
                case AudioPlayer.PAUSE:
                    audioPlayer.play();
                    break;
                default:
                    acOpenAndPlay();
                    break;
            }
        } catch (PlayerException ex) {
            logger.error(ex.getMessage());
//                String msg = "<html><b>An exeption was generated:</b><br><br>" + ex.getMessage() + "<html>";
//                JOptionPane.showMessageDialog(mainFrame, msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void acStop() {
        audioPlayer.stop();
    }

    @Override
    public void acUpdateTime(int value) {
        StringBuilder timeText = new StringBuilder();
        if (currentPli != null) {
            String formattedLength = currentPli.getFormattedLength();
            long duration = TimeUnit.MILLISECONDS.toSeconds(value);
            if (Utilities.isNullOrEmpty(formattedLength)) {
                timeText.append(currentPli.getFormattedLength(duration));
            } else {
                timeText.append(currentPli.getFormattedLength(duration));
                timeText.append(" / ");
                timeText.append(formattedLength);
            }
        } else {
            timeText.append(Utilities.ZERO_TIMER);
        }

        final String timeStr = timeText.toString();
        final int seekValue = (currentPli == null) ? 0 : value;
        if (EventQueue.isDispatchThread()) {
            seekSlider.setValue(seekValue);
            timeLabel.setText(timeStr);
        } else {
            EventQueue.invokeLater(() -> {
                seekSlider.setValue(seekValue);
                timeLabel.setText(timeStr);
            });
        }
    }

    @Override
    public void acSeek() {
        try {
            if (seekSlider.isEnabled()) {
                audioPlayer.seek(Math.round(audioPlayer.getByteLength() * (double) seekSlider.getValue() / seekSlider.getMaximum()));
            }
        } catch (PlayerException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void playlistItemAdded(PlaylistEvent e) {
        if (!playlist.isEmpty() && !previousButton.isEnabled()) {
            enableControlButtons(true);
        }
    }

    @Override
    public void playlistItemRemoved(PlaylistEvent e) {
        if (playlist.isEmpty()) {
            if (audioPlayer.getState() != AudioPlayer.PLAY
                    && audioPlayer.getState() != AudioPlayer.PAUSE) {
                audioPlayer.stop();
            }
            enableControlButtons(false);
        }
    }

    @Override
    public void playModeChanged(PlaylistEvent e) {
        Playlist.PlayMode playMode = playlist.getPlayMode();

        Settings.setPlayMode(playMode);

        StringBuilder toolTipMessage = new StringBuilder("<html><b>");
        toolTipMessage.append(tr("MainFrame.Menu.Player.PlayMode"));
        toolTipMessage.append("</b><br>");
        switch (playMode) {
            case REPEAT_NONE:
                playModeLabel.setIcon(Utilities.PLAYLIST_REPEAT_NONE_ICON);
                playModeRepeatNoneMenuItem.setSelected(true);
                toolTipMessage.append(tr("MainFrame.Menu.Player.PlayMode.RepeatNone"));
                break;
            case REPEAT_ONE:
                playModeLabel.setIcon(Utilities.PLAYLIST_REPEAT_ONE_ICON);
                playModeRepeatOneMenuItem.setSelected(true);
                toolTipMessage.append(tr("MainFrame.Menu.Player.PlayMode.RepeatOne"));
                break;
            case REPEAT_ALL:
                playModeLabel.setIcon(Utilities.PLAYLIST_REPEAT_ALL_ICON);
                playModeRepeatAllMenuItem.setSelected(true);
                toolTipMessage.append(tr("MainFrame.Menu.Player.PlayMode.RepeatAll"));
                break;
            case SHUFFLE:
                playModeLabel.setIcon(Utilities.PLAYLIST_SHUFFLE_ICON);
                playModeShuffleMenuItem.setSelected(true);
                toolTipMessage.append(tr("MainFrame.Menu.Player.PlayMode.Shuffle"));
                break;
            default:
                playModeLabel.setIcon(null);
        }
        toolTipMessage.append("</html>");
        playModeLabel.setToolTipText(toolTipMessage.toString());
    }

    @Override
    public void onIntellitype(int command) {
        switch (command) {
            case JIntellitype.APPCOMMAND_MEDIA_PLAY_PAUSE:
                logger.debug("APPCOMMAND_MEDIA_PLAY_PAUSE command received: " + Integer.toString(command));
                acPlayPause();
                break;
            case JIntellitype.APPCOMMAND_MEDIA_PREVIOUSTRACK:
                logger.debug("APPCOMMAND_MEDIA_PREVIOUSTRACK command received: " + Integer.toString(command));
                acPrevious();
                break;
            case JIntellitype.APPCOMMAND_MEDIA_NEXTTRACK:
                logger.debug("APPCOMMAND_MEDIA_NEXTTRACK command received: " + Integer.toString(command));
                acNext();
                break;
            case JIntellitype.APPCOMMAND_MEDIA_STOP:
                logger.debug("APPCOMMAND_MEDIA_STOP command received: " + Integer.toString(command));
                acStop();
                break;
            default:
                logger.debug("Undefined INTELLITYPE command received: " + Integer.toString(command));
                break;
        }
    }

    @Override
    public void guiEffectsStateChanged(boolean flag) {
        if (flag) {
//            AnimationConfigurationManager.getInstance().allowAnimations(AnimationFacet.ARM);
//            AnimationConfigurationManager.getInstance().allowAnimations(AnimationFacet.FOCUS);
//            AnimationConfigurationManager.getInstance().allowAnimations(AnimationFacet.FOCUS_LOOP_ANIMATION);
            AnimationConfigurationManager.getInstance().allowAnimations(AnimationFacet.GHOSTING_BUTTON_PRESS);
            AnimationConfigurationManager.getInstance().allowAnimations(AnimationFacet.GHOSTING_ICON_ROLLOVER);
            AnimationConfigurationManager.getInstance().allowAnimations(AnimationFacet.ICON_GLOW);
//            AnimationConfigurationManager.getInstance().allowAnimations(AnimationFacet.PRESS);
//            AnimationConfigurationManager.getInstance().allowAnimations(AnimationFacet.ROLLOVER);
//            AnimationConfigurationManager.getInstance().allowAnimations(AnimationFacet.SELECTION);
        } else {
//            AnimationConfigurationManager.getInstance().disallowAnimations(AnimationFacet.ARM);
//            AnimationConfigurationManager.getInstance().disallowAnimations(AnimationFacet.FOCUS);
//            AnimationConfigurationManager.getInstance().disallowAnimations(AnimationFacet.FOCUS_LOOP_ANIMATION);
            AnimationConfigurationManager.getInstance().disallowAnimations(AnimationFacet.GHOSTING_BUTTON_PRESS);
            AnimationConfigurationManager.getInstance().disallowAnimations(AnimationFacet.GHOSTING_ICON_ROLLOVER);
            AnimationConfigurationManager.getInstance().disallowAnimations(AnimationFacet.ICON_GLOW);
//            AnimationConfigurationManager.getInstance().disallowAnimations(AnimationFacet.PRESS);
//            AnimationConfigurationManager.getInstance().disallowAnimations(AnimationFacet.ROLLOVER);
//            AnimationConfigurationManager.getInstance().disallowAnimations(AnimationFacet.SELECTION);
        }
    }

    private class PlayerLauncher extends AbstractSwingWorker<Boolean, Void> {

        private boolean play = false;
        private boolean isFile = false;
        private int duration = -1;

        public PlayerLauncher(boolean play) {
            this.play = play;
        }

        @Override
        protected Boolean doInBackground() throws PlayerException, MalformedURLException {
            PlaylistItem pli = playlist.getCursor();
            if (pli != null) {
                currentPli = pli;
                isFile = pli.isFile();
                if (isFile) {
                    audioPlayer.open(new File(pli.getLocation()));
                    duration = Math.round(audioPlayer.getDuration() / 1000);
                } else {
                    audioPlayer.open(new URL(pli.getLocation()));
                }
                if (play) {
                    audioPlayer.play();
                }
                return Boolean.TRUE;
            }
            currentPli = null;
            return Boolean.FALSE;
        }

        @Override
        protected void done() {
            if (currentPli != null) {
                try {
                    if (get()) {
                        seekSlider.reset();
                        playlistManager.colorizeRow();
                        if (isFile) {
                            if (duration > 0) {
                                seekSlider.setMaximum(duration);
                                seekSlider.setEnabled(true);
                            } else {
                                seekSlider.setMaximum((int) (currentPli.getDuration() * 1000));
                                seekSlider.setEnabled(false);
                            }
                        } else {
                            seekSlider.setEnabled(false);
                        }
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    if (ex.getCause() instanceof PlayerException) {
                        acStop();
                        setStatus("An error occurred...");
                        logger.error(ex.getMessage(), ex);
                        if (ex.getCause().getCause() instanceof FileNotFoundException) {
                            String msg = "<html><b>" + currentPli.getFormattedLength() + "</b> could not be used<br>because the original file could not be found.<html>";
                            JOptionPane.showMessageDialog(mainFrame, msg, "Message", JOptionPane.ERROR_MESSAGE);
                        }
//                    String msg = "<html><b>An exeption was generated:</b><br><br>" + ex.getMessage() + "<html>";
//                    JOptionPane.showMessageDialog(mainFrame, msg, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    }
}
