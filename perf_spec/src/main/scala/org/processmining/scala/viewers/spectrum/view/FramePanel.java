package org.processmining.scala.viewers.spectrum.view;

import org.deckfour.xes.model.XLog;
import org.processmining.scala.log.common.enhancment.segments.common.PreprocessingSession;
import org.processmining.scala.log.common.enhancment.segments.parallel.SegmentProcessor;
import org.processmining.scala.log.common.utils.common.EH;
import org.processmining.scala.viewers.spectrum.model.AbstractDataSource;
import org.processmining.scala.viewers.spectrum.model.EmptyDatasource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.util.function.Consumer;

public class FramePanel extends JPanel implements OpenImpl {

    private static final Logger logger = LoggerFactory.getLogger(FramePanel.class.getName());
    private MainPanel mainPanel;
    private final Consumer<String> title;
    private final Timer singleShotTimer;

    public FramePanel(final String dir, final Consumer<String> title, final boolean isOpenEnabled) {
        this.title = title;
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
        setLayout(new java.awt.BorderLayout());
        performanceSpectrumFactory(new EmptyDatasource(), "", false);
        singleShotTimer = new Timer(0, e -> openPreProcessingDialog("", xlog));
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
                    dir.isEmpty() ? AppSettings.apply() : AppSettings.apply(String.format("%s/config.ini", dir)));
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
            final FileNameExtensionFilter filterPsm = new FileNameExtensionFilter("PSM Session Files", "psm");
            final FileNameExtensionFilter filterXes = new FileNameExtensionFilter("XES Event Log Files", "xes", "gz", "zip", "xml");
            dirDlg.setFileFilter(filterPsm);
            dirDlg.setFileFilter(filterXes);
            if (dirDlg.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                final String path = dirDlg.getSelectedFile().getPath();
                if (!path.isEmpty()) {
                    if (!dirDlg.getSelectedFile().getPath().endsWith(SegmentProcessor.SessionFileName())) {
                        openPreProcessingDialog(path, null);
                    } else {
                        openDatasetDialog(path.substring(0, path.length() - SegmentProcessor.SessionFileName().length()), true);
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

    private void openPreProcessingDialog(final String initialPath, final XLog xlog) {
        try {
            final PreProcessingDialog dialog = xlog == null ? new PreProcessingDialog((JFrame) SwingUtilities.getWindowAncestor(this), initialPath) : new PreProcessingDialog((JFrame) SwingUtilities.getWindowAncestor(this), xlog);
            dialog.setVisible(true);
            final String dir = dialog.getDir();
            if (!dir.isEmpty()) {
                openDatasetDialog(dir, xlog == null);
                //performanceSpectrumFactory(dir, xlog == null);
            }
        } catch (Exception ex) {
            EH.apply().errorAndMessageBox("Error", ex);
        }
    }

    public static boolean checkJvm() {

        if (!PreprocessingSession.isJavaVersionCorrect()) {
            final String msg = "You are using an incompartible version of Java: '" + PreprocessingSession.javaVersion() +
                    "'. Java 1.8.xxx 64bit is required.";
            logger.error(msg);
            logger.info(PreprocessingSession.javaVersion());
            logger.info(PreprocessingSession.javaPlatform());
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
        PreprocessingSession.reportToLog(logger, msg);
    }
}