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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import javax.sound.sampled.AudioSystem;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.commons.io.FilenameUtils;
import org.pushingpixels.substance.api.renderers.SubstanceDefaultTableCellRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xtrememp.player.audio.AudioPlayer;
import xtrememp.playlist.Playlist;
import xtrememp.playlist.PlaylistException;
import xtrememp.playlist.PlaylistIO;
import xtrememp.playlist.PlaylistItem;
import xtrememp.playlist.filter.Predicate;
import xtrememp.playlist.filter.TruePredicate;
import xtrememp.tag.TagInfo;
import xtrememp.ui.table.PlaylistColumn;
import xtrememp.ui.table.PlaylistTableColumn;
import xtrememp.ui.table.PlaylistTableColumnModel;
import xtrememp.ui.table.PlaylistTableModel;
import xtrememp.ui.text.SearchTextField;
import xtrememp.util.AbstractSwingWorker;
import xtrememp.util.Utilities;
import static xtrememp.util.Utilities.tr;
import xtrememp.util.file.AudioFileFilter;
import xtrememp.util.file.M3uPlaylistFileFilter;
import xtrememp.util.file.PlaylistFileFilter;
import xtrememp.util.file.XspfPlaylistFileFilter;

/**
 * Playlist manager class. Special thanks to rom1dep for the changes applied to
 * this class.
 *
 * @author Besmir Beqiri
 */
public class PlaylistManager extends JPanel implements ActionListener,
        DropTargetListener, ListSelectionListener {

    private final Logger logger = LoggerFactory.getLogger(PlaylistManager.class);
    private final AudioFileFilter audioFileFilter = AudioFileFilter.INSTANCE;
    private final PlaylistFileFilter playlistFileFilter = PlaylistFileFilter.INSTANCE;
    private final ControlListener controlListener;
    private final Playlist playlist;
    private JButton openPlaylistButton;
    private JButton savePlaylistButton;
    private JButton addToPlaylistButton;
    private JButton remFromPlaylistButton;
    private JButton clearPlaylistButton;
    private JButton moveUpButton;
    private JButton moveDownButton;
    private JButton mediaInfoButton;
    private JTable playlistTable;
    private PlaylistTableModel playlistTableModel;
    private PlaylistTableColumnModel playlistTableColumnModel;
    private SearchTextField searchTextField;
    private Predicate<PlaylistItem> searchFilter;
    private String searchString;
    private int doubleSelectedRow = -1;
    private volatile boolean firstLoad = false;

    public PlaylistManager(ControlListener controlListener) {
        super(new BorderLayout());
        this.controlListener = controlListener;
        playlist = new Playlist();
        initModel();
        initComponents();
        initFiltering();
    }

    private void initModel() {
        playlistTableColumnModel = new PlaylistTableColumnModel();
        PlaylistColumn[] playlistColumns = Settings.getPlaylistColumns();
        for (int i = 0; i < playlistColumns.length; i++) {
            playlistTableColumnModel.addColumn(new PlaylistTableColumn(playlistColumns[i], i));
        }
        playlistTableModel = new PlaylistTableModel(playlist, playlistTableColumnModel);
    }

    private void initComponents() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        openPlaylistButton = new JButton(Utilities.DOCUMENT_OPEN_ICON);
        openPlaylistButton.setToolTipText(tr("MainFrame.PlaylistManager.OpenPlaylist"));
        openPlaylistButton.addActionListener(this);
        toolBar.add(openPlaylistButton);
        savePlaylistButton = new JButton(Utilities.DOCUMENT_SAVE_ICON);
        savePlaylistButton.setToolTipText(tr("MainFrame.PlaylistManager.SavePlaylist"));
        savePlaylistButton.addActionListener(this);
        toolBar.add(savePlaylistButton);
        toolBar.addSeparator();
        addToPlaylistButton = new JButton(Utilities.LIST_ADD_ICON);
        addToPlaylistButton.setToolTipText(tr("MainFrame.PlaylistManager.AddToPlaylist"));
        addToPlaylistButton.addActionListener(this);
        toolBar.add(addToPlaylistButton);
        remFromPlaylistButton = new JButton(Utilities.LIST_REMOVE_ICON);
        remFromPlaylistButton.setToolTipText(tr("MainFrame.PlaylistManager.RemoveFromPlaylist"));
        remFromPlaylistButton.addActionListener(this);
        remFromPlaylistButton.setEnabled(false);
        toolBar.add(remFromPlaylistButton);
        clearPlaylistButton = new JButton(Utilities.EDIT_CLEAR_ICON);
        clearPlaylistButton.setToolTipText(tr("MainFrame.PlaylistManager.ClearPlaylist"));
        clearPlaylistButton.addActionListener(this);
        clearPlaylistButton.setEnabled(false);
        toolBar.add(clearPlaylistButton);
        toolBar.addSeparator();
        moveUpButton = new JButton(Utilities.GO_UP_ICON);
        moveUpButton.setToolTipText(tr("MainFrame.PlaylistManager.MoveUp"));
        moveUpButton.addActionListener(this);
        moveUpButton.setEnabled(false);
        toolBar.add(moveUpButton);
        moveDownButton = new JButton(Utilities.GO_DOWN_ICON);
        moveDownButton.setToolTipText(tr("MainFrame.PlaylistManager.MoveDown"));
        moveDownButton.addActionListener(this);
        moveDownButton.setEnabled(false);
        toolBar.add(moveDownButton);
        toolBar.addSeparator();
        mediaInfoButton = new JButton(Utilities.MEDIA_INFO_ICON);
        mediaInfoButton.setToolTipText(tr("MainFrame.PlaylistManager.MediaInfo"));
        mediaInfoButton.addActionListener(this);
        mediaInfoButton.setEnabled(false);
        toolBar.add(mediaInfoButton);
        toolBar.add(Box.createHorizontalGlue());
        searchTextField = new SearchTextField(15);
        searchTextField.setMaximumSize(new Dimension(120, searchTextField.getPreferredSize().height));
        searchTextField.getTextField().getDocument().addDocumentListener(new SearchFilterListener());
        toolBar.add(searchTextField);
        toolBar.add(Box.createHorizontalStrut(6));
        this.add(toolBar, BorderLayout.NORTH);

        playlistTable = new JTable(playlistTableModel, playlistTableColumnModel);
        playlistTable.setDefaultRenderer(String.class, new PlaylistCellRenderer());
        playlistTable.setActionMap(null);

        playlistTable.getTableHeader().addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent ev) {
                if (SwingUtilities.isRightMouseButton(ev) || (MouseInfo.getNumberOfButtons() == 1 && ev.isControlDown())) {
                    playlistTableColumnModel.getPopupMenu().show(playlistTable.getTableHeader(), ev.getX(), ev.getY());
                    return;
                }

                int clickedColumn = playlistTableColumnModel.getColumnIndexAtX(ev.getX());
                PlaylistTableColumn playlistColumn = playlistTableColumnModel.getColumn(clickedColumn);
                playlistTableColumnModel.resetAll(playlistColumn.getModelIndex());
                playlistColumn.setSortOrderUp(!playlistColumn.isSortOrderUp());
                playlistTableModel.sort(playlistColumn.getComparator());

                colorizeRow();
            }
        });
        playlistTable.setFillsViewportHeight(true);
        playlistTable.setShowGrid(false);
        playlistTable.setRowSelectionAllowed(true);
        playlistTable.setColumnSelectionAllowed(false);
        playlistTable.setDragEnabled(false);
        playlistTable.setFont(playlistTable.getFont().deriveFont(Font.BOLD));
        playlistTable.setIntercellSpacing(new Dimension(0, 0));
        playlistTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        playlistTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent ev) {
                int selectedRow = playlistTable.rowAtPoint(ev.getPoint());
                if (SwingUtilities.isLeftMouseButton(ev) && ev.getClickCount() == 2) {
                    if (selectedRow != -1) {
                        playlist.setCursorPosition(selectedRow);
                        controlListener.acOpenAndPlay();
                    }
                }
            }
        });
        playlistTable.getSelectionModel().addListSelectionListener(this);
        playlistTable.getColumnModel().getSelectionModel().addListSelectionListener(this);
        playlistTable.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                // View Media Info
                if (e.getKeyCode() == KeyEvent.VK_I && e.getModifiers() == KeyEvent.CTRL_MASK) {
                    viewMediaInfo();
                } // Select all
                else if (e.getKeyCode() == KeyEvent.VK_A && e.getModifiers() == KeyEvent.CTRL_MASK) {
                    playlistTable.selectAll();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    // Move selected track(s) up
                    if (e.getModifiers() == KeyEvent.ALT_MASK) {
                        moveUp();
                    } // Select previous track
                    else {
                        if (playlistTable.getSelectedRow() > 0) {
                            int previousRowIndex = playlistTable.getSelectedRow() - 1;
                            playlistTable.clearSelection();
                            playlistTable.addRowSelectionInterval(previousRowIndex, previousRowIndex);
                            makeRowVisible(previousRowIndex);
                        }
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    // Move selected track(s) down
                    if (e.getModifiers() == KeyEvent.ALT_MASK) {
                        moveDown();
                    } // Select next track
                    else {
                        if (playlistTable.getSelectedRow() < playlistTable.getRowCount() - 1) {
                            int nextRowIndex = playlistTable.getSelectedRow() + 1;
                            playlistTable.clearSelection();
                            playlistTable.addRowSelectionInterval(nextRowIndex, nextRowIndex);
                            makeRowVisible(nextRowIndex);
                        }
                    }
                }// Play selected track
                else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    int selectedRow = playlistTable.getSelectedRow();
                    if (selectedRow != -1) {
                        playlist.setCursorPosition(selectedRow);
                        controlListener.acOpenAndPlay();
                    }
                } // Add new tracks
                else if (e.getKeyCode() == KeyEvent.VK_INSERT) {
                    addFilesDialog(false);
                } // Delete selected tracks
                else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    remove();
                }
            }
        });
        XtremeMP.getInstance().getMainFrame().setDropTarget(new DropTarget(playlistTable, this));
        JScrollPane ptScrollPane = new JScrollPane(playlistTable);
        ptScrollPane.setActionMap(null);
        this.add(ptScrollPane, BorderLayout.CENTER);
    }

    private void initFiltering() {
        searchFilter = (PlaylistItem pli) -> {
            StringBuilder sb = new StringBuilder();
            TagInfo tagInfo = pli.getTagInfo();
            sb.append(tagInfo.getTrack()).append(tagInfo.getTitle()).append(
                    tagInfo.getArtist()).append(tagInfo.getAlbum()).append(
                            tagInfo.getGenre());
            return sb.toString().toLowerCase().contains(
                    PlaylistManager.this.searchString.toLowerCase());
        };
    }

    protected void addFiles(List<File> newFiles, boolean playFirst) {
        AddFilesWorker addFilesWorker = new AddFilesWorker(newFiles, playFirst);
        addFilesWorker.execute();
    }

    public void add(PlaylistItem newPli) {
        playlistTableModel.add(newPli);
    }

    public void add(List<PlaylistItem> newItems) {
        playlistTableModel.add(newItems);
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public void randomizePlaylist() {
        if (!playlist.isEmpty()) {
            playlistTableModel.randomize();
            colorizeRow();
        }
    }

    public void loadPlaylist(String location) {
        PlaylistLoaderWorker playlistLoader = new PlaylistLoaderWorker(location);
        playlistLoader.execute();
    }

    public PlaylistColumn[] getPlaylistColumns() {
        return playlistTableColumnModel.getPlaylistColumns();
    }

    public void setFirstLoad(boolean flag) {
        this.firstLoad = flag;
    }

    public void refreshRow(int index) {
        playlistTableModel.fireTableRowsUpdated(index, index);
    }

    public void openPlaylistDialog() {
        JFileChooser fileChooser = new JFileChooser(Settings.getLastDir());
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(playlistFileFilter);
        fileChooser.setMultiSelectionEnabled(false);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            Settings.setLastDir(file.getPath());
            clearPlaylist();
            loadPlaylist(file.getPath());
        }
    }

    public boolean savePlaylistDialog() {
        JFileChooser fileChooser = new JFileChooser(Settings.getLastDir());
        M3uPlaylistFileFilter m3uFileFilter = new M3uPlaylistFileFilter();
        XspfPlaylistFileFilter xspfFileFilter = new XspfPlaylistFileFilter();
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(m3uFileFilter);
        fileChooser.addChoosableFileFilter(xspfFileFilter);
        fileChooser.setMultiSelectionEnabled(false);
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            FileFilter fileFilter = fileChooser.getFileFilter();
            String fileName = file.getName().toLowerCase();
            if (fileFilter == m3uFileFilter) {
                if (!fileName.endsWith(".m3u")) {
                    fileName = fileName.concat(".m3u");
                }
                try {
                    return PlaylistIO.saveM3U(playlist, file.getParent() + File.separator + fileName);
                } catch (PlaylistException ex) {
                    logger.error("Can't save playlist in M3U format", ex);
                }
            }
            if (fileFilter == xspfFileFilter) {
                if (!fileName.endsWith(".xspf")) {
                    fileName = fileName.concat(".xspf");
                }
                try {
                    return PlaylistIO.saveXSPF(playlist, file.getParent() + File.separator + fileName);
                } catch (PlaylistException ex) {
                    logger.error("Can't save playlist in XSPF format", ex);
                }
            }
            Settings.setLastDir(file.getParent());
        }
        return false;
    }

    public void addFilesDialog(boolean playFirst) {
        JFileChooser fileChooser = new JFileChooser(Settings.getLastDir());
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(audioFileFilter);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            Settings.setLastDir(selectedFiles[0].getParent());
            addFiles(Arrays.asList(selectedFiles), playFirst);
        }
    }

    public void moveUp() {
        if (playlistTable.getSelectedRowCount() > 0) {
            int[] selectedRows = playlistTable.getSelectedRows();
            int minSelectedIndex = selectedRows[0];
            if (minSelectedIndex > 0) {
                playlistTable.clearSelection();
                for (int i = 0, len = selectedRows.length; i < len; i++) {
                    int selectedRow = selectedRows[i];
                    int prevRow = selectedRow - 1;
                    playlistTableModel.moveItem(selectedRow, prevRow);
                    playlistTable.addRowSelectionInterval(prevRow, prevRow);
                }
                makeRowVisible(minSelectedIndex - 1);
            }
            colorizeRow();
        }
    }

    public void moveDown() {
        if (playlistTable.getSelectedRowCount() > 0) {
            int[] selectedRows = playlistTable.getSelectedRows();
            int maxLength = selectedRows.length - 1;
            int maxSelectedIndex = selectedRows[maxLength];
            if (maxSelectedIndex < playlist.size() - 1) {
                playlistTable.clearSelection();
                for (int i = maxLength; i >= 0; i--) {
                    int selectedRow = selectedRows[i];
                    int nextRow = selectedRow + 1;
                    playlistTableModel.moveItem(selectedRow, nextRow);
                    playlistTable.addRowSelectionInterval(nextRow, nextRow);
                }
                makeRowVisible(maxSelectedIndex + 1);
            }
            colorizeRow();
        }
    }

    public void remove() {
        int selectedRowCount = playlistTable.getSelectedRowCount();
        if (selectedRowCount > 0) {
            if (selectedRowCount == playlist.size() && !playlist.isFiltered()) {
                clearPlaylist();
                return;
            }
            List<PlaylistItem> items = new ArrayList<>();
            int[] selectedRows = playlistTable.getSelectedRows();
            for (int i = 0, len = selectedRows.length; i < len; i++) {
                items.add(playlist.getItemAt(selectedRows[i]));
            }
            playlistTableModel.removeAll(items);
            clearSelection();
            colorizeRow();
        }
    }

    protected void clearSelection() {
        playlistTable.clearSelection();
        remFromPlaylistButton.setEnabled(false);
        mediaInfoButton.setEnabled(false);
        moveUpButton.setEnabled(false);
        moveDownButton.setEnabled(false);
    }

    public void clearPlaylist() {
        if (!playlist.isEmpty()) {
            playlistTableModel.clear();
            doubleSelectedRow = -1;
            remFromPlaylistButton.setEnabled(false);
            mediaInfoButton.setEnabled(false);
            moveUpButton.setEnabled(false);
            moveDownButton.setEnabled(false);
            clearPlaylistButton.setEnabled(false);
            Settings.setPlaylistPosition(-1);
            playlistTable.requestFocusInWindow();
        }
    }

    public void colorizeRow() {
        if (!playlist.isEmpty()) {
            int cursorPos = playlist.getCursorPosition();
            doubleSelectedRow = cursorPos;
            playlistTable.repaint();
            makeRowVisible(cursorPos);
        }
    }

    public void makeRowVisible(int rowIndex) {
        if (!(playlistTable.getParent() instanceof JViewport)) {
            return;
        }
        JViewport viewport = (JViewport) playlistTable.getParent();
        Rectangle contentRect = (Rectangle) playlistTable.getCellRect(rowIndex,
                playlistTable.getSelectedColumn(), true).clone();
        Point pt = viewport.getViewPosition();
        contentRect.setLocation(contentRect.x - pt.x, contentRect.y - pt.y);
        viewport.scrollRectToVisible(contentRect);
    }

    private void viewMediaInfo() {
        int selectedRow = playlistTable.getSelectedRow();
        if (selectedRow != -1) {
            PlaylistItem pli = playlist.getItemAt(selectedRow);
            MediaInfoWorker mediaInfoWorker = new MediaInfoWorker(pli);
            mediaInfoWorker.execute();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source.equals(openPlaylistButton)) {
            openPlaylistDialog();
        } else if (source.equals(savePlaylistButton)) {
            savePlaylistDialog();
        } else if (source.equals(addToPlaylistButton)) {
            addFilesDialog(false);
        } else if (source.equals(remFromPlaylistButton)) {
            remove();
        } else if (source.equals(clearPlaylistButton)) {
            clearPlaylist();
        } else if (source.equals(moveUpButton)) {
            moveUp();
        } else if (source.equals(moveDownButton)) {
            moveDown();
        } else if (source.equals(mediaInfoButton)) {
            viewMediaInfo();
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource() == playlistTable.getSelectionModel()) {
            if (playlistTable.getSelectedRowCount() > 0) {
                remFromPlaylistButton.setEnabled(true);
                mediaInfoButton.setEnabled(true);
            }
            ListSelectionModel lsm = playlistTable.getSelectionModel();
            if (lsm.getMinSelectionIndex() == 0) {
                moveUpButton.setEnabled(false);
            } else {
                moveUpButton.setEnabled(true);
            }
            if (lsm.getMaxSelectionIndex() == (playlistTable.getRowCount() - 1)) {
                moveDownButton.setEnabled(false);
            } else {
                moveDownButton.setEnabled(true);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void drop(DropTargetDropEvent ev) {
        DropTargetContext targetContext = ev.getDropTargetContext();
        Transferable t = ev.getTransferable();
        try {
            // Windows
            if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                ev.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                addFiles((List<File>) t.getTransferData(DataFlavor.javaFileListFlavor), false);
                targetContext.dropComplete(true);
                // Linux
            } else if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                ev.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                String urls = (String) t.getTransferData(DataFlavor.stringFlavor);
                List<File> fileList = new ArrayList<>();
                StringTokenizer st = new StringTokenizer(urls);
                while (st.hasMoreTokens()) {
                    URI uri = new URI(st.nextToken());
                    fileList.add(new File(uri));
                }
                addFiles(fileList, false);
                targetContext.dropComplete(true);
            }
        } catch (UnsupportedFlavorException | InvalidDnDOperationException | IOException | URISyntaxException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent ev) {
    }

    @Override
    public void dragOver(DropTargetDragEvent ev) {
    }

    @Override
    public void dragEnter(DropTargetDragEvent ev) {
    }

    @Override
    public void dragExit(DropTargetEvent ev) {
    }

    protected class PlaylistCellRenderer extends SubstanceDefaultTableCellRenderer {

        private final Border emptyBorder = BorderFactory.createEmptyBorder();
        private final Color selectedColor = Color.red;

        public PlaylistCellRenderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
//            if (!SubstanceLookAndFeel.isCurrentLookAndFeel()) {
//                return super.getTableCellRendererComponent(table, value,
//                        isSelected, hasFocus, row, column);
//            }

            super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);

            this.setBorder(emptyBorder);

            if (playlistTableColumnModel.getColumn(column).getPlaylistColumn() == PlaylistColumn.DURATION) {
                this.setHorizontalAlignment(SwingConstants.RIGHT);
            } else {
                this.setHorizontalAlignment(SwingConstants.LEFT);
            }

            if (row == doubleSelectedRow) {
                this.setForeground(selectedColor);
            }

            return this;
        }
    }

    protected class SearchFilterListener implements DocumentListener {

        public void changeFilter(DocumentEvent event) {
            Document document = event.getDocument();
            try {
                clearSelection();
                searchString = document.getText(0, document.getLength());
                if (searchString != null && !searchString.isEmpty()) {
                    playlistTableModel.filter(searchFilter);
                    moveUpButton.setEnabled(false);
                    moveDownButton.setEnabled(false);
                } else {
                    playlistTableModel.filter(TruePredicate.<PlaylistItem>getInstance());
                }
                colorizeRow();
            } catch (BadLocationException ex) {
            }
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            changeFilter(e);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            changeFilter(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            changeFilter(e);
        }
    }

    protected class PlaylistLoaderWorker extends AbstractSwingWorker<Void, PlaylistItem> {

        private final String location;

        public PlaylistLoaderWorker(String location) {
            this.location = location;
        }

        @Override
        protected Void doInBackground() throws Exception {
            List<PlaylistItem> pliList = PlaylistIO.load(location);
            int count = 0;
            int size = pliList.size();
            for (PlaylistItem pli : pliList) {
                if (pli.isFile()) {
                    pli.getTagInfo();
                }
                publish(pli);
                count++;
                setProgress(100 * count / size);
            }
            return null;
        }

        @Override
        protected void process(List<PlaylistItem> moreItems) {
            playlistTableModel.add(moreItems);
        }

        @Override
        protected void done() {
            setProgress(100);
            if (!playlist.isEmpty()) {
                clearPlaylistButton.setEnabled(true);
                AudioPlayer audioPlayer = XtremeMP.getInstance().getAudioPlayer();
                if (audioPlayer.getState() == AudioSystem.NOT_SPECIFIED || audioPlayer.getState() == AudioPlayer.STOP) {
                    int index = Settings.getPlaylistPosition();
                    if (!firstLoad && index >= 0 && index <= (playlist.size() - 1)) {
                        playlist.setCursorPosition(index);
                    } else {
                        playlist.begin();
                    }
                    if (firstLoad) {
                        firstLoad = false;
                        controlListener.acOpenAndPlay();
                    } else {
                        controlListener.acOpen();
                    }
                }
            }
        }
    }

    protected class AddFilesWorker extends AbstractSwingWorker<Void, PlaylistItem> {

        private final List<File> fileList;
        private final boolean playFirst;
        private final int firstIndex;

        public AddFilesWorker(List<File> fileList, boolean playFirst) {
            this.fileList = fileList;
            this.playFirst = playFirst;
            this.firstIndex = playlist.size();
        }

        @Override
        protected Void doInBackground() {
            List<File> tempFileList = new ArrayList<>();
            for (File file : fileList) {
                if (file.isDirectory()) {
                    scanDir(file, tempFileList);
                } else if (audioFileFilter.accept(file)) {
                    tempFileList.add(file);
                }
            }
            int count = 0;
            int size = tempFileList.size();
            for (File file : tempFileList) {
                String baseName = FilenameUtils.getBaseName(file.getName());
                PlaylistItem pli = new PlaylistItem(baseName, file.getAbsolutePath(), -1, true);
                pli.getTagInfo();
                publish(pli);
                count++;
                setProgress(100 * count / size);
            }
            return null;
        }

        @Override
        protected void process(List<PlaylistItem> moreItems) {
            playlistTableModel.add(moreItems);
        }

        @Override
        protected void done() {
            setProgress(100);
            if (!playlist.isEmpty()) {
                clearPlaylistButton.setEnabled(true);
                if (playFirst) {
                    firstLoad = false;
                    playlist.setCursor(playlist.getItemAt(firstIndex));
                    controlListener.acOpenAndPlay();
                }
            }
        }

        protected void scanDir(File dir, List<File> fileList) {
            for (File file : dir.listFiles((FilenameFilter) audioFileFilter)) {
                if (file.isFile()) {
                    fileList.add(file);
                } else {
                    scanDir(file, fileList);
                }
            }
        }
    }

    protected class MediaInfoWorker extends AbstractSwingWorker<Void, PlaylistItem> {

        private final PlaylistItem pli;

        public MediaInfoWorker(PlaylistItem pli) {
            this.pli = pli;
        }

        @Override
        protected Void doInBackground() throws Exception {
            if (pli != null) {
                pli.getTagInfo();
            }
            return null;
        }

        @Override
        protected void process(List<PlaylistItem> moreItems) {
            playlistTableModel.add(moreItems);
        }

        @Override
        protected void done() {
            setProgress(100);
            if (pli != null) {
                MediaInfoDialog mediaInfoDialog = new MediaInfoDialog(pli);
                mediaInfoDialog.setVisible(true);
            }
        }
    }
}
