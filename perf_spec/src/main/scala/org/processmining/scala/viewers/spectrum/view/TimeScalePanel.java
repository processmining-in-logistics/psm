package org.processmining.scala.viewers.spectrum.view;

import org.processmining.scala.log.common.utils.common.EH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;

final class TimeScalePanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(LegendPanel.class.getName());
    private TimeDiffGraphics view;

    @Override
    public void paintComponent(Graphics g) {
        try {
            super.paintComponent(g);
            final Graphics2D g2 = (Graphics2D) g;
            if (view != null) {
                final long rangeMs = (view.paintInputParameters.lastTwIndexExclusive() - view.paintInputParameters.startTwIndex()) * view.ds.twSizeMs();
                if (rangeMs > Duration.ofDays(365*5).toMillis()) {
                    TimeScalePanelHelper.drawByYears(g2, view);
                } else if (rangeMs > Duration.ofDays(120).toMillis()) {
                    TimeScalePanelHelper.drawByMonths(g2, view);
                } else if (rangeMs > Duration.ofDays(4).toMillis()) {
                    TimeScalePanelHelper.drawByDays(g2, view);
                }else if (rangeMs > Duration.ofHours(24).toMillis()) {
                    TimeScalePanelHelper.drawByHours(g2, view);
                } else if (rangeMs > Duration.ofHours(1).toMillis()) {
                    TimeScalePanelHelper.drawBy15min(g2, view);
                } else TimeScalePanelHelper.drawByMinute(g2, view);
            }

        } catch (Exception ex) {
            EH.apply().error("TimeScalePanel.paintComponent", ex);
        }
    }


    void adjustVisualizationParamsAndRepaint(final ViewerState viewerState, final PaintInputParameters paintInputParameters, final TimeDiffGraphics view) {
        this.view = view;
        forceRepaint();
    }

    private void forceRepaint() {
        invalidate();
        repaint();
    }


}
