package org.processmining.scala.viewers.spectrum.view;

import org.deckfour.xes.model.XLog;
import org.processmining.scala.log.common.enhancment.segments.common.PreprocessingSession;
import org.processmining.scala.log.common.enhancment.segments.parallel.SegmentProcessor;
import org.processmining.scala.log.utils.common.errorhandling.EH;
import org.processmining.scala.log.utils.common.errorhandling.JvmParams;
import org.processmining.scala.viewers.spectrum.api.PsmApi;
import org.processmining.scala.viewers.spectrum.api.PsmEvents;
import org.processmining.scala.viewers.spectrum.model.AbstractDataSource;
import org.processmining.scala.viewers.spectrum.model.EmptyDatasource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.util.function.Consumer;

public class FramePanel extends JPanel implements OpenImpl, PsmApi {

    private static final Logger logger = LoggerFactory.getLogger(FramePanel.class.getName());
    private MainPanel mainPanel;
    private final Consumer<String> title;
    private final Timer singleShotTimer;
    private final boolean useDuration;
    public static final String CSV_DIR = "csvdir";

    public FramePanel(final String dir, final Consumer<String> title, final boolean isOpenEnabled, final boolean useDuration) {
        this.title = title;
        this.useDuration = useDuration;
        setLayout(new java.awt.BorderLayout());
        performanceSpectrumFactory(new EmptyDatasource(), dir, true);
        singleShotTimer = new Timer(0, e -> openDatasetDialog(dir, isOpenEnabled));
        singleShotTimer.setRepeats(false);
        if (!isOpenEnabled) {
            singleShotTimer.start();
        }
    }

    public FramePanel(final XLog xlog) {
        this.title = null;
        this.useDuration = true;
        setLayout(new java.awt.BorderLayout());
        performanceSpectrumFactory(new EmptyDatasource(), "", false);
        singleShotTimer = new Timer(0, e -> openPreProcessingDialog("", xlog, xlog == null));
        singleShotTimer.setRepeats(false);
        singleShotTimer.start();
    }

    public FramePanel(final String csvDir) {
        this.title = null;
        this.useDuration = true;
        setLayout(new java.awt.BorderLayout());
        performanceSpectrumFactory(new EmptyDatasource(), csvDir, false);
        singleShotTimer = new Timer(0, e -> openPreProcessingDialog(csvDir, null, false));
        singleShotTimer.setRepeats(false);
        singleShotTimer.start();
    }


    @Override
    public void paintComponent(Graphics g) {
        try {
            super.paintComponent(g);
        } catch (Exception ex) {
            EH.apply().error("FramePanel.paintComponent", ex);
        }
    }

    private void performanceSpectrumFactory(final AbstractDataSource ds, final String dir, final boolean isOpenEnabled) {
        try {
            final MainPanel newMainPanel = new MainPanel(ds, this, isOpenEnabled,
                    dir.isEmpty() || dir.endsWith(CSV_DIR) ? AppSettings.apply() : AppSettings.apply(String.format("%s/config.ini", dir)));
            if (mainPanel != null) {
                remove(mainPanel);
            }
            add(newMainPanel, java.awt.BorderLayout.CENTER);
            newMainPanel.adjustVisualizationParams();
            newMainPanel.adjustZoomRange();
            newMainPanel.setZooming();
            mainPanel = newMainPanel;
            if (title != null) {
                title.accept(dir);
            }
            repaint();
            revalidate();
        } catch (Exception ex) {
            EH.apply().error("performanceSpectrumFactory", ex);
            throw new RuntimeException(ex);
        }
    }


    @Override
    public void onOpen() {
        try {
            final JFileChooser dirDlg = new JFileChooser(PreProcessingPanel.getPsmHomeDir());
            dirDlg.setFileSelectionMode(JFileChooser.FILES_ONLY);
            final FileNameExtensionFilter filterXes = new FileNameExtensionFilter("XES Event Log Files", "xes", "gz", "zip", "xml");
            final FileNameExtensionFilter filterCsv = new FileNameExtensionFilter("Folder with CSV Event Log Files", CSV_DIR);
            final FileNameExtensionFilter filterPsm = new FileNameExtensionFilter("PSM Session Files", "psm");
            dirDlg.setFileFilter(filterXes);
            dirDlg.setFileFilter(filterCsv);
            dirDlg.setFileFilter(filterPsm);

            if (dirDlg.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                final String path = dirDlg.getSelectedFile().getPath();
                if (!path.isEmpty()) {
                    if (dirDlg.getSelectedFile().getPath().endsWith(SegmentProcessor.SessionFileName())) {
                        openDatasetDialog(path.substring(0, path.length() - SegmentProcessor.SessionFileName().length()), true);
                    } else if (dirDlg.getSelectedFile().getPath().toLowerCase().endsWith(".csv")){
                        openPreProcessingDialog(path, null, true);
                    }else{
                        openPreProcessingDialog(path, null, true);
                    }
                }
            }
        } catch (Exception ex) {
            EH.apply().errorAndMessageBox("Cannot open a new dataset", ex);
        }

    }

    private void openDatasetDialog(final String dir, final boolean isOpenEnabled) {
        try {
            final DatasetOpenDialog dialog = new DatasetOpenDialog((JFrame) SwingUtilities.getWindowAncestor(this), dir);
            dialog.setVisible(true);
            final AbstractDataSource ds = dialog.getDs();
            performanceSpectrumFactory(ds, dir, isOpenEnabled);
        } catch (Exception ex) {
            EH.apply().errorAndMessageBox("Error", ex);
        }

    }

    private void openPreProcessingDialog(final String initialPath, final XLog xlog, final boolean isOpenEnabled) {
        try {
            final PreProcessingDialog dialog = xlog == null ?
                    new PreProcessingDialog((JFrame) SwingUtilities.getWindowAncestor(this), initialPath, useDuration)
                    : new PreProcessingDialog((JFrame) SwingUtilities.getWindowAncestor(this), xlog);
            dialog.setVisible(true);
            final String dir = dialog.getDir();
            if (!dir.isEmpty()) {
                openDatasetDialog(dir, isOpenEnabled);
                //performanceSpectrumFactory(dir, xlog == null);
            }
        } catch (Exception ex) {
            EH.apply().errorAndMessageBox("Error", ex);
        }
    }

    public static boolean checkJvm() {

        if (!JvmParams.isJavaVersionCorrect()) {
            final String msg = "You are using an incompartible version of Java: '" + JvmParams.javaVersion() +
                    "'. Java 1.8.xxx 64bit is required.";
            logger.error(msg);
            logger.info(JvmParams.javaVersion());
            logger.info(JvmParams.javaPlatform());
            JOptionPane.showMessageDialog(null, msg, "Wrong JRE/JDK version", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
//            if (!PreprocessingSession.isJavaPlatformCorrect()) {
//                final String msg = "You are using an incompartible platform of Java: '" + PreprocessingSession.javaPlatform() +
//                        "'. Java 1.8.xxx 64bit is required. The application will not work stable!";
//                logger.error(msg);
//
    }

    //for ProM
    public static void reportToLog(final String msg) {
        JvmParams.reportToLog(logger, msg);
    }

    @Override
    public void sortAndFilter(String[] sortedSegments) {
        mainPanel.sortAndFilter(sortedSegments);
    }

    @Override
    public void addEventHandler(PsmEvents handler) {
        mainPanel.addEventHandler(handler);
   }

    @Override
    public void removeEventHandler(PsmEvents handler) {
        mainPanel.removeEventHandler(handler);
    }
}
