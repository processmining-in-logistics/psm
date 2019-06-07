package org.processmining.scala.viewers.spectrum.view;

import javax.swing.*;

import org.processmining.scala.log.utils.common.errorhandling.EH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

final class LegendPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(LegendPanel.class.getName());
    private List<String> legend = new LinkedList<>();
    private final static int HeightPx = 25;
    private final static int WidthPx = 90;
    private final static int ShiftXPx = WidthPx;
    private final static int ShiftYPx = HeightPx;
    private final TimeDiffController tdf;

    LegendPanel(final TimeDiffController tdf) {
        this.tdf = tdf;
        this.setMinimumSize(new Dimension(500, 300));
    }

    public static List<String> splitLegend(String legend){
        return Arrays.asList(legend.split("%"));
    }
    void setLegend(final String legend) {
        this.legend = splitLegend(legend);
        setPreferredSize(new Dimension(500, ShiftYPx + HeightPx * this.legend.size()));
    }

    @Override
    public void paintComponent(Graphics g) {
        try {
            super.paintComponent(g);
            final Graphics2D g2 = (Graphics2D) g;
            for (int i = 1; i < legend.size(); i++) {
                g.setColor(tdf.getDefaultClazzColor(i - 1, TimeDiffController.NotTransparent()));
                g2.fill3DRect(ShiftXPx, ShiftYPx + i * HeightPx + 1, WidthPx, HeightPx - 2, true);
                g.setColor(Color.BLACK);
                g2.drawString(legend.get(i), ShiftXPx + WidthPx + 10, ShiftYPx + (i + 1) * HeightPx - 5);
            }
        } catch (Exception ex) {
            EH.apply().error("Paint", ex);
        }
    }

    public String getLegendTitle() {
        return legend.isEmpty() ? "" : legend.get(0);
    }
}
