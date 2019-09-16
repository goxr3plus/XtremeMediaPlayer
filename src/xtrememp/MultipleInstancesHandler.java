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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xtrememp.playlist.PlaylistIO;
import xtrememp.playlist.PlaylistItem;
import xtrememp.util.file.AudioFileFilter;
import xtrememp.util.file.PlaylistFileFilter;

/**
 *
 * @author Besmir Beqiri
 */
public class MultipleInstancesHandler {

    private final Logger logger = LoggerFactory.getLogger(MultipleInstancesHandler.class);
    /** The instance. */
    private static final MultipleInstancesHandler instance = new MultipleInstancesHandler();
    private final AudioFileFilter audioFileFilter = AudioFileFilter.INSTANCE;
    private final PlaylistFileFilter playlistFileFilter = PlaylistFileFilter.INSTANCE;
    /** Port number for multiple instances socket communication. */
    public static final int MULTIPLE_INSTANCES_SOCKET = 9999;
    private ServerSocket serverSocket;
    private static boolean closing = false;

    /**
     * Instantiates a new multiple instances handler.
     */
    private MultipleInstancesHandler() {
    }

    /**
     * Gets the single instance of MultipleInstancesHandler.
     * 
     * @return single instance of MultipleInstancesHandler
     */
    public static MultipleInstancesHandler getInstance() {
        return instance;
    }

    /**
     * This class is responsible of listening to server socket and accept
     * connections from "slave" instances.
     */
    private class SocketListener extends Thread {

        private ServerSocket serverSocket;
        private PlaylistItemQueue queue;

        /**
         * Instantiates a new socket listener.
         * 
         * @param serverSocket the server socket
         * @param queue the queue
         */
        SocketListener(ServerSocket serverSocket, PlaylistItemQueue queue) {
            super();
            this.serverSocket = serverSocket;
            this.queue = queue;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            Socket s = null;
            BufferedReader br = null;
            BufferedOutputStream bos = null;
            try {
                while (true) {
                    s = serverSocket.accept();
                    // Once a connection arrives, read args
                    br = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    String line;
                    while ((line = br.readLine()) != null) {
                        File file = new File(line);
                        logger.info("Received connection with content: {}", line);
                        if (audioFileFilter.accept(file)) {
                            String fileName = file.getName().substring(0, file.getName().lastIndexOf(".")).trim();
                            PlaylistItem newPli = new PlaylistItem(fileName, file.getAbsolutePath(), -1, true);
                            queue.addItem(newPli);
                        } else if (playlistFileFilter.accept(file)) {
                            List<PlaylistItem> pliList = PlaylistIO.load(file.getAbsolutePath());
                            for (PlaylistItem pli : pliList) {
                                queue.addItem(pli);
                            }
                        } else {
                            // process line
                        }
                    }
                    IOUtils.closeQuietly(br);
                    IOUtils.closeQuietly(s);
                    logger.info("Connection finished");
                }
            } catch (Exception ex) {
                if (!closing) {
                    logger.error(ex.getMessage(), ex);
                }
            } finally {
                IOUtils.closeQuietly(bos);
                IOUtils.closeQuietly(br);
                IOUtils.closeQuietly(s);
            }
        }
    }

    /**
     * This class is responsible of create a queue of songs to be added. When
     * opening multiple files, OS launch a "slave" instance for every file, so
     * this queue adds songs in the order connections are made, and when no more
     * connections are received, then add to playlist.
     */
    private class PlaylistItemQueue extends Thread {

        private List<PlaylistItem> queue;
        private volatile long lastItemAdded = 0;

        /**
         * Instantiates a new songs queue.
         */
        PlaylistItemQueue() {
            queue = new ArrayList<PlaylistItem>();
        }

        /**
         * Adds the item.
         * 
         * @param item the item
         */
        public void addItem(PlaylistItem item) {
            queue.add(item);
            lastItemAdded = System.currentTimeMillis();
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                while (true) {
                    if (!queue.isEmpty() && lastItemAdded < System.currentTimeMillis() - 1000) {
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                // Get an auxiliar list with songs
                                ArrayList<PlaylistItem> auxList = new ArrayList<PlaylistItem>(queue);
                                // Clear songs queue
                                queue.clear();
                                // Add songs
                                XtremeMP.getInstance().addToPlaylistAndPlay(auxList);
                            }
                        });
                    }
                    // Wait always, even if songsQueue was not empty, to avoid entering again until queue is cleared
                    Thread.sleep(1000);
                }
            } catch (InterruptedException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Called when application exits.
     */
    public void dispose() {
        if (serverSocket != null) {
            closing = true;
            try {
                serverSocket.close();
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Tries to open a server socket to listen to other instances.
     * 
     * @return true if server socket could be opened
     */
    public boolean isFirstInstance() {
        try {
            // Open server socket
            serverSocket = new ServerSocket(MULTIPLE_INSTANCES_SOCKET);
            logger.info("Listening on port {} for other instances", MULTIPLE_INSTANCES_SOCKET);

            // Initialize songs queue
            PlaylistItemQueue queue = new PlaylistItemQueue();

            // Initialize socket listener
            SocketListener listener = new SocketListener(serverSocket, queue);

            // Start threads
            queue.start();
            listener.start();

            // Server socket could be opened, so this instance is a "master"
            return true;
        } catch (Exception ex) {
            // Server socket could not be opened, so this instance is a "slave"
            logger.info("Another instance is running");
            return false;
        }
    }

    /**
     * Opens a client socket and sends arguments to "master".
     * 
     * @param args the args
     */
    public void sendArgumentsToFirstInstance(String... args) {
        Socket clientSocket = null;
        PrintWriter output = null;
        try {
            // Open client socket to communicate with "master"
            clientSocket = new Socket("localhost", MULTIPLE_INSTANCES_SOCKET);
            output = new PrintWriter(clientSocket.getOutputStream(), true);
            for (String arg : args) {
                File file = new File(arg);
                if (audioFileFilter.accept(file) || playlistFileFilter.accept(file)) {
                    // Send args: audio files or play lists
                    logger.info("Sending arg {}", arg);
                    output.write(arg);
                } else {
                    // It's a command
                    logger.info("Sending command {}", arg);
                    output.write(arg);
                }
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            IOUtils.closeQuietly(output);
            IOUtils.closeQuietly(clientSocket);
        }
    }
}
