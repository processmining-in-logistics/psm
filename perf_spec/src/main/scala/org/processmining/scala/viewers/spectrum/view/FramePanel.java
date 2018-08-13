package org.processmining.scala.viewers.spectrum.view;

import org.deckfour.xes.model.XLog;
import org.processmining.scala.log.common.enhancment.segments.parallel.SegmentProcessor;
import org.processmining.scala.log.common.utils.common.EH;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class FramePanel extends JPanel implements OpenImpl{

    private MainPanel mainPanel;
    private final Consumer<String> title;
    private final Timer singleShotTimer;

    public FramePanel(final String dir, final Consumer<String> title) {
        this.title = title;
        singleShotTimer = null;
        setLayout(new java.awt.BorderLayout());
        performanceSpectrumFactory(dir, true);
    }

    public FramePanel(final XLog xlog) {
        this.title = null;
        setLayout(new java.awt.BorderLayout());
        performanceSpectrumFactory("", false);
        singleShotTimer = new Timer(0, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openPreProcessingDialog("", xlog);
            }
        });

        singleShotTimer.setRepeats(false);
        singleShotTimer.start();
    }


    private void performanceSpectrumFactory(final String dir, final boolean isOpenEnabled) {
        final MainPanel newMainPanel = new MainPanel(dir, this, isOpenEnabled);
        if (mainPanel != null) {
            remove(mainPanel);
        }
        add(newMainPanel, java.awt.BorderLayout.CENTER);
        newMainPanel.adjustVisualizationParams();
        newMainPanel.adjustZoomRange();
        newMainPanel.setZooming();
        mainPanel = newMainPanel;
        if(title != null){
            title.accept(dir);
        }
        repaint();
        revalidate();
    }



    @Override
    public void onOpen() {
        try {
            final JFileChooser dirDlg = new JFileChooser();
            dirDlg.setFileSelectionMode(JFileChooser.FILES_ONLY);
            final FileNameExtensionFilter filterXes = new FileNameExtensionFilter("XES Event Log Files", "xes", "gz", "zip", "xml");
            final FileNameExtensionFilter filterPsm = new FileNameExtensionFilter("PSM Session Files", "psm");
            dirDlg.setFileFilter(filterXes);
            dirDlg.setFileFilter(filterPsm);
            if (dirDlg.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                final String path = dirDlg.getSelectedFile().getPath();
                if (!path.isEmpty()) {
                    if (!dirDlg.getSelectedFile().getPath().endsWith(SegmentProcessor.SessionFileName())) {
                        openPreProcessingDialog(path, null);
                    } else {
                        performanceSpectrumFactory(path.substring(0, path.length() - SegmentProcessor.SessionFileName().length()), true);
                    }
                }
            }
        } catch (Exception ex) {
            EH.apply().errorAndMessageBox("Cannot open a new dataset", ex);
        }

    }

    private void openPreProcessingDialog(final String initialPath, final XLog xlog){
        final PreProcessingDialog dialog = xlog == null ? new PreProcessingDialog((JFrame) SwingUtilities.getWindowAncestor(this), initialPath) : new PreProcessingDialog((JFrame) SwingUtilities.getWindowAncestor(this), xlog);
        dialog.setVisible(true);
        final String dir = dialog.getDir();
        if (!dir.isEmpty()) {
            performanceSpectrumFactory(dir, xlog == null);
        }
    }

}
