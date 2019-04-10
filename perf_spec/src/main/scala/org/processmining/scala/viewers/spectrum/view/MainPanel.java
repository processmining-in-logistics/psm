package org.processmining.scala.viewers.spectrum.view;

import org.processmining.scala.log.common.utils.common.EH;
import org.processmining.scala.viewers.spectrum.api.PsmApi;
import org.processmining.scala.viewers.spectrum.api.PsmEvents;
import org.processmining.scala.viewers.spectrum.model.AbstractDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * @author nlvden
 */
public final class MainPanel extends javax.swing.JPanel implements Zooming, PsmApi {

    private static final Logger logger = LoggerFactory.getLogger(MainPanel.class.getName());
    final TimeDiffController controller;
    private final static DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH.mm.ss", Locale.US);
    private final static DateTimeFormatter weekOfDayFormatter = DateTimeFormatter.ofPattern("E", Locale.US);
    private final ZoneId zoneId;
    private final boolean isOpenEnabled;
    private final Set<PsmEvents> eventHandlers = new HashSet<>();

    MainPanel(final AbstractDataSource ds, final OpenImpl openImpl, final boolean isOpenEnabled, final AppSettings appSettings) {
        try {
            this.isOpenEnabled = isOpenEnabled;
            this.controller = new TimeDiffController(ds, appSettings);
            zoneId = controller.appSettings().zoneId();
            initComponents(appSettings.zoneId());
            jButtonOpen.addActionListener(e -> openImpl.onOpen());
            if (controller.ds().twCount() > 0) {
                subscribe();
            }
            enableControls(controller.ds().twCount() > 0);
            jButtonIds.setVisible(false);
        } catch (Exception ex) {
            EH.apply().error("MainPanel", ex);
            throw new RuntimeException(ex);
        }
    }


    private void subscribe() {

        jPanelWhiteBoard.setBackground(Color.WHITE);
        controller.view().subscribeToSelectionEvent((x) -> {
                    try {
                        jLabelPos.setText(
                                String.format(
                                        "%s %s", (LocalDateTime.ofInstant(Instant.ofEpochMilli(x), zoneId)).format(timestampFormatter),
                                        ""/*zoneId.toString()*/)
                        );
                        jLabelDayOfWeek.setText(LocalDateTime.ofInstant(Instant.ofEpochMilli(x), zoneId).format(weekOfDayFormatter));
                    } catch (Exception ex) {
                        EH.apply().error(ex);
                    }
                }
        );
        jContentScrollPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                try {
                    jPanelWhiteBoard.onComponentResized(e.getComponent().getSize());
                } catch (Exception ex) {
                    EH.apply().error(ex);
                }
            }
        });

        jSliderXZoom.addChangeListener(e -> adjustVisualizationParams());
        jSliderYZoom.addChangeListener(e -> adjustVisualizationParams());
        jSliderPos.addChangeListener(e -> adjustVisualizationParams());
        checkBoxShowTraces.addChangeListener(e -> adjustVisualizationParams());
        checkBoxShowBins.addChangeListener(e -> adjustVisualizationParams());
        checkBoxHideSelection.addChangeListener(e -> adjustVisualizationParams());
        checkBoxegmentNames.addChangeListener(e -> adjustVisualizationParams());
        checkBoxShowGrid.addChangeListener(e -> adjustVisualizationParams());
        jButtonClearSelection.addActionListener(e -> controller.clearSelectionMode());
    }


    static final double HundredPercent = 100.0;
    static final double SliderPosUnitPercent = 0.01;
    static final double SliderXZoomUnitZoomingPercent = 1.0;
    static final double SliderYZoomUnitZoomingPercent = 1.0;
    private Options options = Options.apply();

    public void adjustVisualizationParams() {
        adjustVisualizationParams(options);
    }


    public void raiseSortingOrFiltering(final String[] sortedSegments){
        eventHandlers.forEach(x -> x.onSortingOrFiltering(sortedSegments));
    }

    public void adjustVisualizationParams(final Options options) {
        try {
            this.options = options;
            final ViewerState state = new ViewerState(
                    jSliderPos.getValue(), // [0; 10000)  1 is for SliderPosUnitPercent% of the dataset duration
                    jSliderXZoom.getValue(), //  [100; 10000] 1 is for SliderXZoomUnitZoomingPercent% zooming
                    jSliderYZoom.getValue(), // [10; 1000] 1 is for SliderYZoomUnitZoomingPercent% zooming
                    options.whiteList(),
                    options.blackList(), //empty line is for no filter
                    options.minCount(), // [1; MAX_INT]
                    options.maxCount(), //
                    checkBoxShowTraces.isSelected(),
                    checkBoxShowGrid.isSelected(),
                    false,
                    options.reverseColors(),
                    checkBoxShowBins.isSelected(),
                    checkBoxHideSelection.isSelected(),
                    checkBoxegmentNames.isSelected()
            );

            if (!options.ids().isEmpty()) {
                controller.addId(options.ids());
            }

            logger.debug(state.toString());
            final int startTwIndex = (int) (state.pos() / HundredPercent * SliderPosUnitPercent * controller.ds().twCount());
            final int twsFitIntoScreen = (int) (controller.ds().twCount() / (state.xZoom() / HundredPercent * SliderXZoomUnitZoomingPercent));
            final int lastTwIndexExclusive = startTwIndex + Math.min(twsFitIntoScreen, controller.ds().twCount() - startTwIndex);
            final String[] filteredNames = localSort(controller.ds().segmentNames(state.whiteList(), state.blackList(), state.minCount(), state.maxCount()));
            final PaintInputParameters pip = new PaintInputParameters(startTwIndex, lastTwIndexExclusive, twsFitIntoScreen, filteredNames);



            final int h = jPanelWhiteBoard.adjustVisualizationParamsAndRepaint(state, pip);
            jContentScrollPane.getVerticalScrollBar().setUnitIncrement(h);
            jTimeScalePanel2.adjustVisualizationParamsAndRepaint(state, pip, jPanelWhiteBoard);

        } catch (Exception ex) {
            EH.apply().error("adjustVisualizationParams", ex);
        }
    }

    private static final int DefaultScreenWidthPx = 1900;
    private static final int DefaultBarWidthPx = 8;

    public void adjustZoomRange() {
        final int maxSegmentsToShow = DefaultScreenWidthPx / DefaultBarWidthPx;
        final int xZoomPercent = (int) (controller.view().ds.twCount() * HundredPercent / maxSegmentsToShow);
        jSliderXZoom.setValue(xZoomPercent);
    }

    public void setZooming() {
        controller.setZooming(this);
    }

    @Override
    public void changeVerticalZoom(final int delta) {
        try {
            jSliderYZoom.setValue(jSliderYZoom.getValue() + delta * 1);
            adjustVisualizationParams();
        } catch (Exception ex) {
            EH.apply().error(ex);
        }

    }

    @Override
    public void changeHorizontalZoom(final int delta) {
        try {
            jSliderXZoom.setValue(jSliderXZoom.getValue() + delta * 1);
            adjustVisualizationParams();
        } catch (Exception ex) {
            EH.apply().error(ex);
        }
    }

    private String[] localSort(final String[] names) {
        return names;
    }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents(final ZoneId zoneId) {

        jPanelPlayer = new javax.swing.JPanel();
        jPanelPlayerNorth = new javax.swing.JPanel();
        jSliderPos = new javax.swing.JSlider();
        jPanelEventsPreview = new javax.swing.JPanel();
        jPanelEventPreviewChart = new javax.swing.JPanel();
        jTimeScalePanel2 = new org.processmining.scala.viewers.spectrum.view.TimeScalePanel(zoneId);
        jPanelPlayerCenter = new javax.swing.JPanel();
        jPanelDateZoom = new javax.swing.JPanel();
        jPanelDate = new javax.swing.JPanel();
        jLabelPos = new javax.swing.JLabel();
        jLabelDayOfWeek = new javax.swing.JLabel();
        jPanelZoom = new javax.swing.JPanel();
        jSliderYZoom = new javax.swing.JSlider();
        jSliderXZoom = new javax.swing.JSlider();
        jPanelControls = new javax.swing.JPanel();
        jButtonOpen = new javax.swing.JButton();
        jPanelOpenR = new javax.swing.JPanel();
        jButtonSettings = new javax.swing.JButton();
        jPanelL01 = new javax.swing.JPanel();
        jPanelL02 = new javax.swing.JPanel();
        jPanelR02 = new javax.swing.JPanel();
        checkBoxShowTraces = new javax.swing.JCheckBox();
        jPanelR03 = new javax.swing.JPanel();
        checkBoxShowBins = new javax.swing.JCheckBox();
        jPanelR04 = new javax.swing.JPanel();
        checkBoxShowGrid = new javax.swing.JCheckBox();
        jPanelR05 = new javax.swing.JPanel();
        jPanelR06 = new javax.swing.JPanel();
        checkBoxHideSelection = new javax.swing.JCheckBox();
        jPanelR07 = new javax.swing.JPanel();
        jPanelL08 = new javax.swing.JPanel();
        jPanelR08 = new javax.swing.JPanel();
        jButtonIds = new javax.swing.JButton();
        jPanelR09 = new javax.swing.JPanel();
        jPanelR10 = new javax.swing.JPanel();
        jPanelL10 = new javax.swing.JPanel();
        jButtonClearSelection = new javax.swing.JButton();
        jPanelL11 = new javax.swing.JPanel();
        jPanelR12 = new javax.swing.JPanel();
        jPanelL12 = new javax.swing.JPanel();
        jButtonLegend = new javax.swing.JButton();
        jPanelL13 = new javax.swing.JPanel();
        checkBoxegmentNames = new javax.swing.JCheckBox();
        jPanelContent = new javax.swing.JPanel();
        jContentScrollPane = new javax.swing.JScrollPane();
        jPanelWhiteBoard = controller.view();

        setMinimumSize(new java.awt.Dimension(800, 300));
        setLayout(new java.awt.BorderLayout());

        jPanelPlayer.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 5, 5));
        jPanelPlayer.setPreferredSize(new java.awt.Dimension(0, 143));
        jPanelPlayer.setLayout(new java.awt.BorderLayout());

        jPanelPlayerNorth.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 5, 1));
        jPanelPlayerNorth.setPreferredSize(new java.awt.Dimension(0, 65));
        jPanelPlayerNorth.setLayout(new java.awt.BorderLayout());

        jSliderPos.setMaximum(9999);
        jSliderPos.setMinimum(-9999);
        jSliderPos.setToolTipText("Horizontal position");
        jSliderPos.setValue(0);
        jPanelPlayerNorth.add(jSliderPos, java.awt.BorderLayout.SOUTH);

        jPanelEventsPreview.setLayout(new java.awt.BorderLayout());

        jPanelEventPreviewChart.setLayout(new java.awt.BorderLayout());

        jTimeScalePanel2.setLayout(new java.awt.BorderLayout());
        jPanelEventPreviewChart.add(jTimeScalePanel2, java.awt.BorderLayout.CENTER);

        jPanelEventsPreview.add(jPanelEventPreviewChart, java.awt.BorderLayout.CENTER);

        jPanelPlayerNorth.add(jPanelEventsPreview, java.awt.BorderLayout.CENTER);

        jPanelPlayer.add(jPanelPlayerNorth, java.awt.BorderLayout.NORTH);

        jPanelPlayerCenter.setLayout(new java.awt.BorderLayout());

        jPanelDateZoom.setPreferredSize(new java.awt.Dimension(0, 50));
        jPanelDateZoom.setLayout(new java.awt.BorderLayout());

        jPanelDate.setPreferredSize(new java.awt.Dimension(220, 0));
        jPanelDate.setLayout(new java.awt.BorderLayout());

        jLabelPos.setFont(new java.awt.Font("Tahoma", 0, 20)); // NOI18N
        jLabelPos.setText("00-00-00 01-01-1970");
        jPanelDate.add(jLabelPos, java.awt.BorderLayout.NORTH);

        jLabelDayOfWeek.setFont(new java.awt.Font("Tahoma", 0, 20)); // NOI18N
        jLabelDayOfWeek.setText("MO");
        jPanelDate.add(jLabelDayOfWeek, java.awt.BorderLayout.SOUTH);

        jPanelDateZoom.add(jPanelDate, java.awt.BorderLayout.WEST);

        jPanelZoom.setLayout(new java.awt.BorderLayout());

        jSliderYZoom.setMaximum(500);
        jSliderYZoom.setMinimum(10);
        jSliderYZoom.setToolTipText("Vertical zoom");
        jPanelZoom.add(jSliderYZoom, java.awt.BorderLayout.NORTH);

        jSliderXZoom.setMaximum(10000);
        jSliderXZoom.setMinimum(1);
        jSliderXZoom.setToolTipText("Horizontal zoom");
        jSliderXZoom.setValue(100);
        jPanelZoom.add(jSliderXZoom, java.awt.BorderLayout.SOUTH);

        jPanelDateZoom.add(jPanelZoom, java.awt.BorderLayout.CENTER);

        jPanelPlayerCenter.add(jPanelDateZoom, java.awt.BorderLayout.NORTH);

        jPanelControls.setLayout(new java.awt.BorderLayout());

        jButtonOpen.setText("Open...");
        jButtonOpen.setToolTipText("Open new dataset");
        jPanelControls.add(jButtonOpen, java.awt.BorderLayout.WEST);

        jPanelOpenR.setLayout(new java.awt.BorderLayout());

        jButtonSettings.setText("Options...");
        jButtonSettings.setToolTipText("Filtering and advanced options");
        jButtonSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSettingsActionPerformed(evt);
            }
        });

        jPanelOpenR.add(jButtonSettings, java.awt.BorderLayout.EAST);

        jPanelL01.setLayout(new java.awt.BorderLayout());

        jPanelL02.setPreferredSize(new java.awt.Dimension(20, 0));
        jPanelL02.setLayout(new java.awt.BorderLayout());
        jPanelL01.add(jPanelL02, java.awt.BorderLayout.WEST);

        jPanelR02.setLayout(new java.awt.BorderLayout());

        checkBoxShowTraces.setSelected(true);
        checkBoxShowTraces.setText("Lines");
        checkBoxShowTraces.setToolTipText("Show detailed spectrum");
        checkBoxShowTraces.setMargin(new java.awt.Insets(2, 5, 2, 5));
        jPanelR02.add(checkBoxShowTraces, java.awt.BorderLayout.WEST);

        jPanelR03.setLayout(new java.awt.BorderLayout());

        checkBoxShowBins.setText("Bars");
        checkBoxShowBins.setToolTipText("Show aggregated spectrum");
        checkBoxShowBins.setMargin(new java.awt.Insets(2, 5, 2, 25));
        jPanelR03.add(checkBoxShowBins, java.awt.BorderLayout.WEST);

        jPanelR04.setLayout(new java.awt.BorderLayout());

        checkBoxShowGrid.setText("Grid");
        checkBoxShowGrid.setToolTipText("Show vertical grid");
        checkBoxShowGrid.setMargin(new java.awt.Insets(2, 5, 2, 5));
        jPanelR04.add(checkBoxShowGrid, java.awt.BorderLayout.WEST);

        jPanelR05.setLayout(new java.awt.BorderLayout());

        jPanelR06.setLayout(new java.awt.BorderLayout());

        checkBoxHideSelection.setText("Hide selection");
        checkBoxHideSelection.setToolTipText("Hide selected spectrum");
        checkBoxHideSelection.setMargin(new java.awt.Insets(2, 25, 2, 5));
        jPanelR06.add(checkBoxHideSelection, java.awt.BorderLayout.WEST);

        jPanelR07.setLayout(new java.awt.BorderLayout());

        jPanelL08.setPreferredSize(new java.awt.Dimension(10, 0));
        jPanelL08.setLayout(new java.awt.BorderLayout());
        jPanelR07.add(jPanelL08, java.awt.BorderLayout.WEST);

        jPanelR08.setLayout(new java.awt.BorderLayout());

        jButtonIds.setText("IDs...");
        jButtonIds.setToolTipText("Define case IDs for highlighting");
        jPanelR08.add(jButtonIds, java.awt.BorderLayout.WEST);

        jPanelR09.setLayout(new java.awt.BorderLayout());

        jPanelR10.setPreferredSize(new java.awt.Dimension(25, 0));
        jPanelR10.setLayout(new java.awt.BorderLayout());
        jPanelR09.add(jPanelR10, java.awt.BorderLayout.EAST);

        jPanelL10.setLayout(new java.awt.BorderLayout());

        jButtonClearSelection.setText("Clear");
        jButtonClearSelection.setToolTipText("Clear selection");
        jPanelL10.add(jButtonClearSelection, java.awt.BorderLayout.EAST);

        jPanelL11.setLayout(new java.awt.BorderLayout());

        jPanelR12.setPreferredSize(new java.awt.Dimension(10, 0));
        jPanelR12.setLayout(new java.awt.BorderLayout());
        jPanelL11.add(jPanelR12, java.awt.BorderLayout.EAST);

        jPanelL12.setLayout(new java.awt.BorderLayout());

        jButtonLegend.setText("Legend...");
        jButtonLegend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {

                //Code for debugging starts
                final PsmEvents handler = sortedSegments -> {
                    for(String s: sortedSegments){
                        logger.info(s);
                    }
                };
                addEventHandler(handler);
                sortAndFilter(new String[]{"Create Fine:Payment", "Create Fine:Send Fine", "Send Fine:Payment", "Payment:Add penalty"});
                //removeEventHandler(handler);
                //Code for debugging ends

                final LegendDialog2 legendDialog =
                        new LegendDialog2((JComponent) evt.getSource(), controller.view().ds.legend(), controller);
                legendDialog.setVisible(true);

            }
        });
        jPanelL12.add(jButtonLegend, java.awt.BorderLayout.LINE_END);

        jPanelL13.setLayout(new java.awt.BorderLayout());
        jPanelL12.add(jPanelL13, java.awt.BorderLayout.CENTER);

        jPanelL11.add(jPanelL12, java.awt.BorderLayout.CENTER);

        jPanelL10.add(jPanelL11, java.awt.BorderLayout.CENTER);

        jPanelR09.add(jPanelL10, java.awt.BorderLayout.CENTER);

        jPanelR08.add(jPanelR09, java.awt.BorderLayout.CENTER);

        jPanelR07.add(jPanelR08, java.awt.BorderLayout.CENTER);

        jPanelR06.add(jPanelR07, java.awt.BorderLayout.CENTER);

        jPanelR05.add(jPanelR06, java.awt.BorderLayout.CENTER);

        checkBoxegmentNames.setSelected(true);
        checkBoxegmentNames.setText("Names");
        checkBoxegmentNames.setToolTipText("Show names of segments");
        checkBoxegmentNames.setMargin(new java.awt.Insets(2, 5, 2, 5));
        jPanelR05.add(checkBoxegmentNames, java.awt.BorderLayout.WEST);

        jPanelR04.add(jPanelR05, java.awt.BorderLayout.CENTER);

        jPanelR03.add(jPanelR04, java.awt.BorderLayout.CENTER);

        jPanelR02.add(jPanelR03, java.awt.BorderLayout.CENTER);

        jPanelL01.add(jPanelR02, java.awt.BorderLayout.CENTER);

        jPanelOpenR.add(jPanelL01, java.awt.BorderLayout.CENTER);

        jPanelControls.add(jPanelOpenR, java.awt.BorderLayout.CENTER);

        jPanelPlayerCenter.add(jPanelControls, java.awt.BorderLayout.CENTER);

        jPanelPlayer.add(jPanelPlayerCenter, java.awt.BorderLayout.CENTER);

        add(jPanelPlayer, java.awt.BorderLayout.SOUTH);

        jPanelContent.setLayout(new java.awt.BorderLayout());

        jContentScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        jPanelWhiteBoard.setLayout(new java.awt.BorderLayout());
        jContentScrollPane.setViewportView(jPanelWhiteBoard);

        jPanelContent.add(jContentScrollPane, java.awt.BorderLayout.CENTER);

        add(jPanelContent, java.awt.BorderLayout.CENTER);
    }

    private void jButtonSettingsActionPerformed(ActionEvent evt) {
        (new OptionsDialog(this, true, options)).setVisible(true);
    }


    private void jButtonClearSelectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearSelectionActionPerformed
        try {
            controller.clearSelectionMode();
        } catch (Exception ex) {
            EH.apply().error(ex);
        }
    }//GEN-LAST:event_jButtonClearSelectionActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox checkBoxHideSelection;
    private javax.swing.JCheckBox checkBoxShowBins;
    private javax.swing.JCheckBox checkBoxShowGrid;
    private javax.swing.JCheckBox checkBoxShowTraces;
    private javax.swing.JCheckBox checkBoxegmentNames;
    private javax.swing.JButton jButtonClearSelection;
    private javax.swing.JButton jButtonIds;
    private javax.swing.JButton jButtonLegend;
    private javax.swing.JButton jButtonOpen;
    private javax.swing.JButton jButtonSettings;
    private javax.swing.JScrollPane jContentScrollPane;
    private javax.swing.JLabel jLabelDayOfWeek;
    private javax.swing.JLabel jLabelPos;
    private javax.swing.JPanel jPanelContent;
    private javax.swing.JPanel jPanelControls;
    private javax.swing.JPanel jPanelDate;
    private javax.swing.JPanel jPanelDateZoom;
    private javax.swing.JPanel jPanelEventPreviewChart;
    private javax.swing.JPanel jPanelEventsPreview;
    private javax.swing.JPanel jPanelL01;
    private javax.swing.JPanel jPanelL02;
    private javax.swing.JPanel jPanelL08;
    private javax.swing.JPanel jPanelL10;
    private javax.swing.JPanel jPanelL11;
    private javax.swing.JPanel jPanelL12;
    private javax.swing.JPanel jPanelL13;
    private javax.swing.JPanel jPanelOpenR;
    private javax.swing.JPanel jPanelPlayer;
    private javax.swing.JPanel jPanelPlayerCenter;
    private javax.swing.JPanel jPanelPlayerNorth;
    private javax.swing.JPanel jPanelR02;
    private javax.swing.JPanel jPanelR03;
    private javax.swing.JPanel jPanelR04;
    private javax.swing.JPanel jPanelR05;
    private javax.swing.JPanel jPanelR06;
    private javax.swing.JPanel jPanelR07;
    private javax.swing.JPanel jPanelR08;
    private javax.swing.JPanel jPanelR09;
    private javax.swing.JPanel jPanelR10;
    private javax.swing.JPanel jPanelR12;
    private org.processmining.scala.viewers.spectrum.view.TimeDiffGraphics jPanelWhiteBoard;
    private javax.swing.JPanel jPanelZoom;
    private javax.swing.JSlider jSliderPos;
    private javax.swing.JSlider jSliderXZoom;
    private javax.swing.JSlider jSliderYZoom;
    private org.processmining.scala.viewers.spectrum.view.TimeScalePanel jTimeScalePanel2;
    // End of variables declaration//GEN-END:variables


    private void enableControls(final boolean e) {
        jButtonOpen.setEnabled(isOpenEnabled);
        jButtonClearSelection.setEnabled(e);
        jButtonIds.setEnabled(e);
        jButtonLegend.setEnabled(e);
        jButtonSettings.setEnabled(e);
        jSliderPos.setEnabled(e);
        jSliderXZoom.setEnabled(e);
        jSliderYZoom.setEnabled(e);
        checkBoxegmentNames.setEnabled(e);
        checkBoxHideSelection.setEnabled(e);
        checkBoxShowBins.setEnabled(e);
        checkBoxShowGrid.setEnabled(e);
        checkBoxShowTraces.setEnabled(e);
        jLabelPos.setEnabled(e);
        jLabelDayOfWeek.setEnabled(e);

    }

    @Override
    public void sortAndFilter(final String[] sortedSegments) {
        final String[] blackList = {};
        adjustVisualizationParams(options.setListOfSegments(sortedSegments, blackList));
    }

    @Override
    public void addEventHandler(PsmEvents handler) {
        eventHandlers.add(handler);
    }

    @Override
    public void removeEventHandler(PsmEvents handler) {
        eventHandlers.remove(handler);
    }
}
