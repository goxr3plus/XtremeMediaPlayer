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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xtrememp.playlist.Playlist.PlayMode;
import xtrememp.ui.skin.DarkSapphireSkin;
import xtrememp.ui.table.PlaylistColumn;
import xtrememp.util.Utilities;
import xtrememp.visualization.FullSpectrogram;

/**
 *
 * @author Besmir Beqiri
 */
public final class Settings {

    private static final Logger logger = LoggerFactory.getLogger(Settings.class);
    private static final String CACHE_DIR = ".xtrememp";
    private static final String SETTINGS_FILE = "settings.conf";
    private static final String PROPERTY_CACHE_DIR = "xtrememp.cache.dir";
    private static final String PROPERTY_PLAYER_AUDIO_GAIN = "xtrememp.player.audio.gain";
    private static final String PROPERTY_PLAYER_AUDIO_PAN = "xtrememp.player.audio.pan";
    private static final String PROPERTY_PLAYER_AUDIO_MUTE = "xtrememp.player.audio.mute";
    private static final String PROPERTY_PLAYER_AUDIO_MIXERNAME = "xtrememp.player.audio.mixer.name";
    private static final String PROPERTY_EQUILAZER_PRESET_INDEX = "xtrememp.equilazer.preset.index";
    private static final String PROPERTY_LAST_DIR = "xtrememp.last.dir";
    private static final String PROPERTY_LAST_VIEW = "xtrememp.last.view";
    private static final String PROPERTY_PLAYLIST_POSITION = "xtrememp.playlist.position";
    private static final String PROPERTY_PLAYLIST_COLUMNS = "xtrememp.playlist.columns";
    private static final String PROPERTY_PLAYLIST_PLAYMODE = "xtrememp.playlist.playmode";
    private static final String PROPERTY_VISUALIZATION = "xtrememp.visualization";
    private static final String PROPERTY_LANGUAGE_INDEX = "xtrememp.language.index";
    private static final String PROPERTY_GUI_EFFECTS = "xtrememp.gui.effects";
    private static final String PROPERTY_SKIN = "xtrememp.skin";
    private static final String PROPERTY_UPDATES_AUTOMATIC = "xtrememp.update.automatic";
    private static final String PROPERTY_MAINFRAME_X = "xtrememp.mainframe.x";
    private static final String PROPERTY_MAINFRAME_Y = "xtrememp.mainframe.y";
    private static final String PROPERTY_MAINFRAME_WIDTH = "xtrememp.mainframe.width";
    private static final String PROPERTY_MAINFRAME_HEIGHT = "xtrememp.mainframe.height";
    private static final Properties properties = new Properties();

    public static void setPlaylistColumns(PlaylistColumn[] playlistColumns) {
        StringBuilder propertyColumns = new StringBuilder();

        for (PlaylistColumn playlistColumn : playlistColumns) {
            propertyColumns.append(playlistColumn.name()).append(":");
            propertyColumns.append(playlistColumn.getWidth()).append(";");
        }

        properties.setProperty(PROPERTY_PLAYLIST_COLUMNS, propertyColumns.toString());
    }

    public static PlaylistColumn[] getPlaylistColumns() {
        PlaylistColumn[] defaultPlaylistColumns = PlaylistColumn.values();
        StringBuilder defaultColumns = new StringBuilder();

        for (PlaylistColumn playlistColumn : defaultPlaylistColumns) {
            defaultColumns.append(playlistColumn.name()).append(":");
            defaultColumns.append(playlistColumn.getWidth()).append(";");
        }

        String propertyColumns = properties.getProperty(PROPERTY_PLAYLIST_COLUMNS,
                defaultColumns.toString());

        Pattern pattern = Pattern.compile("[:;]");
        String[] columns = pattern.split(propertyColumns, 0);
        int columnCount = columns.length / 2;
        if (columnCount <= 0) { //The config file might be corrupted → restore default.
            propertyColumns = defaultColumns.toString();
            columns = pattern.split(propertyColumns, 0);
            columnCount = columns.length / 2;
        }

        PlaylistColumn[] playlistColumns = new PlaylistColumn[columnCount];
        for (int i = 0; i < columnCount; i++) {
            PlaylistColumn column = PlaylistColumn.valueOf(columns[2*i]);
            column.setWidth(Integer.parseInt(columns[2*i + 1]));
            playlistColumns[i] = column;
        }

        return playlistColumns;
    }

    public static void setLanguageIndex(int languageIndex) {
        properties.setProperty(PROPERTY_LANGUAGE_INDEX, Integer.toString(languageIndex));
    }

    public static int getLanguageIndex() {
        return Integer.parseInt(properties.getProperty(PROPERTY_LANGUAGE_INDEX, "0"));
    }

    public static void setUIEffectsEnabled(boolean gfxUI) {
        properties.setProperty(PROPERTY_GUI_EFFECTS, Boolean.toString(gfxUI));
    }

    public static boolean isUIEffectsEnabled() {
        return Boolean.parseBoolean(properties.getProperty(PROPERTY_GUI_EFFECTS, Boolean.toString(true)));
    }

    public static void setLastView(String lastView) {
        properties.setProperty(PROPERTY_LAST_VIEW, lastView);
    }

    public static String getLastView() {
        return properties.getProperty(PROPERTY_LAST_VIEW, Utilities.PLAYLIST_MANAGER);
    }

    public static void setLastDir(String lastDir) {
        properties.setProperty(PROPERTY_LAST_DIR, lastDir);
    }

    public static String getLastDir() {
        return properties.getProperty(PROPERTY_LAST_DIR, System.getProperty("user.dir"));
    }

    public static String getVisualization() {
        return properties.getProperty(PROPERTY_VISUALIZATION, FullSpectrogram.NAME);
    }

    public static void setVisualization(String visualization) {
        properties.setProperty(PROPERTY_VISUALIZATION, visualization);
    }

    public static int getPlaylistPosition() {
        return Integer.parseInt(properties.getProperty(PROPERTY_PLAYLIST_POSITION, "0"));
    }

    public static void setPlaylistPosition(int playlistPosition) {
        properties.setProperty(PROPERTY_PLAYLIST_POSITION, Integer.toString(playlistPosition));
    }

    public static PlayMode getPlayMode() {
        return PlayMode.valueOf(properties.getProperty(PROPERTY_PLAYLIST_PLAYMODE, PlayMode.REPEAT_ALL.name()));
    }

    public static void setPlayMode(PlayMode playMode) {
        properties.setProperty(PROPERTY_PLAYLIST_PLAYMODE, playMode.name());
    }

    public static boolean isMuted() {
        return Boolean.parseBoolean(properties.getProperty(PROPERTY_PLAYER_AUDIO_MUTE, Boolean.toString(false)));
    }

    public static void setMuted(boolean mute) {
        properties.setProperty(PROPERTY_PLAYER_AUDIO_MUTE, Boolean.toString(mute));
    }

    public static int getGain() {
        return Integer.parseInt(properties.getProperty(PROPERTY_PLAYER_AUDIO_GAIN, String.valueOf(Utilities.MAX_GAIN)));
    }

    public static void setGain(int gain) {
        properties.setProperty(PROPERTY_PLAYER_AUDIO_GAIN, Integer.toString(gain));
    }

    public static int getPan() {
        return Integer.parseInt(properties.getProperty(PROPERTY_PLAYER_AUDIO_PAN, "0"));
    }

    public static void setPan(int pan) {
        properties.setProperty(PROPERTY_PLAYER_AUDIO_PAN, Integer.toString(pan));
    }

    public static String getMixerName() {
        return properties.getProperty(PROPERTY_PLAYER_AUDIO_MIXERNAME, "");
    }

    public static void setMixerName(String mixerName) {
        properties.setProperty(PROPERTY_PLAYER_AUDIO_MIXERNAME, mixerName);
    }

    public static int getEqualizerPresetIndex() {
        return Integer.parseInt(properties.getProperty(PROPERTY_EQUILAZER_PRESET_INDEX, "0"));
    }

    public static void setEqualizerPresetIndex(int eqIndex) {
        properties.setProperty(PROPERTY_EQUILAZER_PRESET_INDEX, Integer.toString(eqIndex));
    }

    public static String getSkin() {
        return properties.getProperty(PROPERTY_SKIN, DarkSapphireSkin.class.getName());
    }

    public static void setSkin(String className) {
        properties.setProperty(PROPERTY_SKIN, className);
    }

    public static File getCacheDir() {
        File cacheDir = new File(properties.getProperty(PROPERTY_CACHE_DIR, System.getProperty("user.home")), CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
        return cacheDir;
    }

    public static void setCacheDir(File parent) {
        properties.setProperty(PROPERTY_CACHE_DIR, parent.getPath());
        configureLogback();
    }

    public static void configureLogback() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            JoranConfigurator jc = new JoranConfigurator();
            jc.setContext(context);
            context.reset();
            context.putProperty("CACHE_DIR", getCacheDir().getPath());
            // override default configuration
            jc.doConfigure(Settings.class.getResourceAsStream("/xtrememp/resources/logback.xml"));
        } catch (JoranException ex) {
            logger.error(ex.getMessage());
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    }

    public static boolean isAutomaticUpdatesEnabled() {
        return Boolean.parseBoolean(properties.getProperty(PROPERTY_UPDATES_AUTOMATIC, Boolean.toString(true)));
    }

    public static void setAutomaticUpdatesEnabled(boolean b) {
        properties.setProperty(PROPERTY_UPDATES_AUTOMATIC, Boolean.toString(b));
    }

    /**
     * Gets the bounds of the application main frame in the form of a
     * <code>Rectangle</code> object.
     * 
     * @return a rectangle indicating this component's bounds
     */
    public static Rectangle getMainFrameBounds() {
        String x = properties.getProperty(PROPERTY_MAINFRAME_X, "0");
        String y = properties.getProperty(PROPERTY_MAINFRAME_Y, "0");
        String width = properties.getProperty(PROPERTY_MAINFRAME_WIDTH, "1016");
        String height = properties.getProperty(PROPERTY_MAINFRAME_HEIGHT, "552");
        return new Rectangle(Integer.parseInt(x), Integer.parseInt(y), Integer.parseInt(width), Integer.parseInt(height));
    }

    /**
     * Sets the application main frame new size and location.
     * 
     * @param r the bounding rectangle for this component
     */
    public static void setMainFrameBounds(Rectangle r) {
        properties.setProperty(PROPERTY_MAINFRAME_X, Integer.toString(r.x));
        properties.setProperty(PROPERTY_MAINFRAME_Y, Integer.toString(r.y));
        properties.setProperty(PROPERTY_MAINFRAME_WIDTH, Integer.toString(r.width));
        properties.setProperty(PROPERTY_MAINFRAME_HEIGHT, Integer.toString(r.height));
    }

    /**
     * Reads all the properties from the settings file.
     */
    public static void loadSettings() {
        File file = new File(getCacheDir(), SETTINGS_FILE);
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                properties.load(fis);
                fis.close();
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Writes all the properties in the settings file.
     */
    public static void storeSettings() {
        try {
            File file = new File(getCacheDir(), SETTINGS_FILE);
            FileOutputStream fos = new FileOutputStream(file);
            properties.store(fos, "Xtreme Media Player Settings");
            fos.close();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
