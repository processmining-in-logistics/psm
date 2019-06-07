package org.processmining.scala.viewers.spectrum.view;

import javax.swing.*;

public class LegendDialog2 extends javax.swing.JDialog {

    private LegendPanel legendPanel1;

    public LegendDialog2(JComponent parent, final String legend, final TimeDiffController tdf, final String classifierName) {
        super((JFrame) SwingUtilities.getWindowAncestor(parent), true);
        legendPanel1 = new LegendPanel(tdf);
        initComponents();
        legendPanel1.setLegend(legend);
        jScrollPane1.setViewportView(legendPanel1);
        setTitle(String.format("%s (%s)", legendPanel1.getLegendTitle(), classifierName));
        PreProcessingDialog.centerWindow(this);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(550, 320));
        getContentPane().add(jScrollPane1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
