package org.processmining.scala.viewers.spectrum.view;

import org.processmining.scala.log.utils.common.errorhandling.EH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class OptionsDialog extends javax.swing.JDialog {
    private static final Logger logger = LoggerFactory.getLogger(OptionsDialog.class.getName());
    private final MainPanel parent;
    Options options;


    public OptionsDialog(final MainPanel parent, final boolean modal, final Options options) {
        super((JFrame) SwingUtilities.getWindowAncestor(parent), modal);
        this.options = options;
        this.parent = parent;
        initComponents();
        jTextFieldWhiteList.setText(patternsToString(options.whiteList()));
        jTextFieldBlackList.setText(patternsToString(options.blackList()));
        jTextFieldIds.setText(options.ids());
        jSpinnerMin.setValue(options.minCount());
        jSpinnerMax.setValue(options.maxCount());
        checkBoxReverseColorsOrder.setSelected(options.reverseColors());
        PreProcessingDialog.centerWindow(this);
    }

    private static String patternsToString(final Pattern[] patterns) {
        final String[] l = new String[patterns.length];
        for (int i = 0; i < patterns.length; i++) {
            l[i] = patterns[i].toString();
        }
        return String.join(";", l);
    }


    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelBottom = new javax.swing.JPanel();
        jPanel14 = new javax.swing.JPanel();
        jButtonOk = new javax.swing.JButton();
        jButtonCancel = new javax.swing.JButton();
        jPanel15 = new javax.swing.JPanel();
        jButtonCopy = new javax.swing.JButton();
        jPanel16 = new javax.swing.JPanel();
        jPanel17 = new javax.swing.JPanel();
        jButtonApply = new javax.swing.JButton();
        jPanel18 = new javax.swing.JPanel();
        jPanelContent = new javax.swing.JPanel();
        jPanelWhiteList = new javax.swing.JPanel();
        jTextFieldWhiteList = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jTextFieldBlackList = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldIds = new javax.swing.JTextField();
        jPanel5 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jButtonLoadIds = new javax.swing.JButton();
        jButtonClearIds = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jSpinnerMax = new javax.swing.JSpinner();
        jSpinnerMin = new javax.swing.JSpinner();
        jPanel11 = new javax.swing.JPanel();
        jPanel12 = new javax.swing.JPanel();
        checkBoxReverseColorsOrder = new javax.swing.JCheckBox();
        jPanel13 = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Settings");
        setMaximumSize(new java.awt.Dimension(2147483647, 400));
        setMinimumSize(new java.awt.Dimension(650, 400));

        jPanelBottom.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanelBottom.setPreferredSize(new java.awt.Dimension(0, 43));
        jPanelBottom.setLayout(new java.awt.BorderLayout());

        jPanel14.setPreferredSize(new java.awt.Dimension(200, 30));
        jPanel14.setLayout(new java.awt.BorderLayout());

        jButtonOk.setText("OK");
        jButtonOk.setPreferredSize(new java.awt.Dimension(100, 25));
        jButtonOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOkActionPerformed(evt);
            }
        });
        jPanel14.add(jButtonOk, java.awt.BorderLayout.EAST);

        jButtonCancel.setText("Cancel");
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });
        jPanel14.add(jButtonCancel, java.awt.BorderLayout.WEST);

        jPanelBottom.add(jPanel14, java.awt.BorderLayout.EAST);

        jPanel15.setLayout(new java.awt.BorderLayout());

        jButtonCopy.setText("Copy");
        jButtonCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCopyActionPerformed(evt);
            }
        });
        jPanel15.add(jButtonCopy, java.awt.BorderLayout.LINE_START);

        jPanel16.setPreferredSize(new java.awt.Dimension(25, 0));
        jPanel16.setLayout(new java.awt.BorderLayout());
        jPanel15.add(jPanel16, java.awt.BorderLayout.LINE_END);

        jPanel17.setLayout(new java.awt.BorderLayout());

        jButtonApply.setText("Apply");
        jButtonApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonApplyActionPerformed(evt);
            }
        });
        jPanel17.add(jButtonApply, java.awt.BorderLayout.LINE_END);

        jPanel18.setLayout(new java.awt.BorderLayout());
        jPanel17.add(jPanel18, java.awt.BorderLayout.CENTER);

        jPanel15.add(jPanel17, java.awt.BorderLayout.CENTER);

        jPanelBottom.add(jPanel15, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanelBottom, java.awt.BorderLayout.SOUTH);

        jPanelContent.setLayout(new java.awt.BorderLayout());

        jPanelWhiteList.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanelWhiteList.setPreferredSize(new java.awt.Dimension(0, 43));
        jPanelWhiteList.setLayout(new java.awt.BorderLayout());
        jPanelWhiteList.add(jTextFieldWhiteList, java.awt.BorderLayout.CENTER);

        jLabel1.setText("Filter in:");
        jLabel1.setPreferredSize(new java.awt.Dimension(84, 16));
        jPanelWhiteList.add(jLabel1, java.awt.BorderLayout.WEST);

        jPanelContent.add(jPanelWhiteList, java.awt.BorderLayout.NORTH);

        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel2.setPreferredSize(new java.awt.Dimension(0, 43));
        jPanel2.setLayout(new java.awt.BorderLayout());

        jLabel2.setText("Filter out:");
        jLabel2.setPreferredSize(new java.awt.Dimension(84, 16));
        jPanel2.add(jLabel2, java.awt.BorderLayout.WEST);

        jTextFieldBlackList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldBlackListActionPerformed(evt);
            }
        });
        jPanel2.add(jTextFieldBlackList, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanel2, java.awt.BorderLayout.PAGE_START);

        jPanel3.setLayout(new java.awt.BorderLayout());

        jPanel4.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 5, 5, 5));
        jPanel4.setPreferredSize(new java.awt.Dimension(0, 53));
        jPanel4.setLayout(new java.awt.BorderLayout());

        jLabel3.setText("Case IDs:");
        jLabel3.setPreferredSize(new java.awt.Dimension(84, 16));
        jPanel4.add(jLabel3, java.awt.BorderLayout.WEST);

        jTextFieldIds.setToolTipText("Use spaces as separators");
        jPanel4.add(jTextFieldIds, java.awt.BorderLayout.CENTER);

        jPanel3.add(jPanel4, java.awt.BorderLayout.PAGE_START);

        jPanel5.setLayout(new java.awt.BorderLayout());

        jPanel6.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel6.setPreferredSize(new java.awt.Dimension(0, 43));
        jPanel6.setLayout(new java.awt.BorderLayout());

        jButtonLoadIds.setText("Load IDs...");
        jButtonLoadIds.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLoadIdsActionPerformed(evt);
            }
        });
        jPanel6.add(jButtonLoadIds, java.awt.BorderLayout.LINE_START);

        jButtonClearIds.setText("Clear IDs");
        jButtonClearIds.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonClearIdsActionPerformed(evt);
            }
        });
        jPanel6.add(jButtonClearIds, java.awt.BorderLayout.LINE_END);

        jPanel5.add(jPanel6, java.awt.BorderLayout.PAGE_START);

        jPanel7.setLayout(new java.awt.BorderLayout());

        jPanel8.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 5, 5, 5));
        jPanel8.setPreferredSize(new java.awt.Dimension(0, 53));
        jPanel8.setLayout(new java.awt.BorderLayout());

        jLabel4.setText("Throughput:");
        jLabel4.setPreferredSize(new java.awt.Dimension(84, 16));
        jPanel8.add(jLabel4, java.awt.BorderLayout.WEST);

        jPanel9.setLayout(new java.awt.BorderLayout());

        jPanel10.setLayout(new java.awt.BorderLayout());

        jSpinnerMax.setToolTipText("Maximal throughput");
        jSpinnerMax.setPreferredSize(new java.awt.Dimension(100, 22));
        jPanel10.add(jSpinnerMax, java.awt.BorderLayout.LINE_END);

        jSpinnerMin.setToolTipText("Minimal throughput");
        jSpinnerMin.setPreferredSize(new java.awt.Dimension(100, 22));
        jPanel10.add(jSpinnerMin, java.awt.BorderLayout.LINE_START);

        jPanel9.add(jPanel10, java.awt.BorderLayout.WEST);

        jPanel8.add(jPanel9, java.awt.BorderLayout.CENTER);

        jPanel7.add(jPanel8, java.awt.BorderLayout.PAGE_START);

        jPanel11.setLayout(new java.awt.BorderLayout());

        jPanel12.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 5, 5, 5));
        jPanel12.setPreferredSize(new java.awt.Dimension(0, 53));
        jPanel12.setLayout(new java.awt.BorderLayout());

        checkBoxReverseColorsOrder.setText("Reverse colors order");
        checkBoxReverseColorsOrder.setToolTipText("Reverse order of colors");
        jPanel12.add(checkBoxReverseColorsOrder, java.awt.BorderLayout.WEST);

        jPanel11.add(jPanel12, java.awt.BorderLayout.PAGE_START);

        jPanel13.setLayout(new java.awt.BorderLayout());
        jPanel11.add(jPanel13, java.awt.BorderLayout.CENTER);

        jPanel7.add(jPanel11, java.awt.BorderLayout.CENTER);

        jPanel5.add(jPanel7, java.awt.BorderLayout.CENTER);

        jPanel3.add(jPanel5, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanel3, java.awt.BorderLayout.CENTER);

        jPanelContent.add(jPanel1, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanelContent, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextFieldBlackListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldBlackListActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldBlackListActionPerformed

    private void jButtonCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCopyActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButtonCopyActionPerformed

    private void jButtonClearIdsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearIdsActionPerformed
        try {
            parent.controller.clearSelectionMode();
        } catch (Exception ex) {
            EH.apply().error(ex);
        }
    }//GEN-LAST:event_jButtonClearIdsActionPerformed

    private void jButtonLoadIdsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadIdsActionPerformed
        final JFileChooser dirDlg = new JFileChooser();
        final JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(parent);
        dirDlg.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (dirDlg.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            final String path = dirDlg.getSelectedFile().getPath();
            try {
                byte[] encoded = Files.readAllBytes(Paths.get(path));
                final String file = new String(encoded, StandardCharsets.UTF_8);
                parent.controller.addId(file);
            } catch (IOException ex) {
                logger.warn(ex.toString());
                JOptionPane.showMessageDialog(frame, "Cannot pre-processed file " + path + ": " + ex, "Wrong ID file", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                EH.apply().error(ex);
            }
        }
    }//GEN-LAST:event_jButtonLoadIdsActionPerformed

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCancelActionPerformed
        dispose();
    }//GEN-LAST:event_jButtonCancelActionPerformed

    private void jButtonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOkActionPerformed
        if (onApply()) {
            dispose();
        }
    }//GEN-LAST:event_jButtonOkActionPerformed

    private void jButtonApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonApplyActionPerformed
        onApply();
    }//GEN-LAST:event_jButtonApplyActionPerformed


    private boolean onApply() {
        try {
            Pattern[] tmpWhiteListFilter = new Pattern[0];
            Pattern[] tmpBlackListFilter = new Pattern[0];
            if (!jTextFieldWhiteList.getText().isEmpty()) {
                final String[] tmp = jTextFieldWhiteList.getText().split(";");
                tmpWhiteListFilter = new Pattern[tmp.length];
                for (int i = 0; i < tmp.length; i++) {
                    tmpWhiteListFilter[i] = Pattern.compile(tmp[i]);
                }
            }
            if (!jTextFieldBlackList.getText().isEmpty()) {
                final String[] tmp = jTextFieldBlackList.getText().split(";");
                tmpBlackListFilter = new Pattern[tmp.length];
                for (int i = 0; i < tmp.length; i++) {
                    tmpBlackListFilter[i] = Pattern.compile(tmp[i]);
                }
            }

            final String ids = jTextFieldIds.getText();

            options = new Options(tmpWhiteListFilter,
                    tmpBlackListFilter,
                    ids,
                    (Integer) jSpinnerMin.getValue(),
                    (Integer) jSpinnerMax.getValue(),
                    checkBoxReverseColorsOrder.isSelected(),
                    20
            );
            parent.adjustVisualizationParams(options);

        } catch (PatternSyntaxException ex) {
            JOptionPane.showMessageDialog(this, "Wrong regex pattern: " + ex.toString(), "Regex filter error", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (Exception ex) {
            EH.apply().error("Error in onApply", ex);
        }
        return true;
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox checkBoxReverseColorsOrder;
    private javax.swing.JButton jButtonApply;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonClearIds;
    private javax.swing.JButton jButtonCopy;
    private javax.swing.JButton jButtonLoadIds;
    private javax.swing.JButton jButtonOk;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JPanel jPanelBottom;
    private javax.swing.JPanel jPanelContent;
    private javax.swing.JPanel jPanelWhiteList;
    private javax.swing.JSpinner jSpinnerMax;
    private javax.swing.JSpinner jSpinnerMin;
    private javax.swing.JTextField jTextFieldBlackList;
    private javax.swing.JTextField jTextFieldIds;
    private javax.swing.JTextField jTextFieldWhiteList;
    // End of variables declaration//GEN-END:variables
}
