package org.processmining.scala.viewers.spectrum.view;

import org.apache.log4j.PropertyConfigurator;
import org.processmining.scala.log.common.enhancment.segments.common.PreprocessingSession;
import org.processmining.scala.log.common.utils.common.EH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class Form extends javax.swing.JFrame {
    private static final Logger logger = LoggerFactory.getLogger(Form.class.getName());
    private static final String Title = "Performance Spectrum Miner " + PreprocessingSession.Version();
    private static Form frame;

    public Form() {
    }

    public static void main(String args[]) {
        try {
            //System.setProperty("sun.java2d.opengl", "True");
            PropertyConfigurator.configure("./res/log4j.properties");
            PreprocessingSession.reportToLog(logger, "Performance Spectrum Miner started");
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ex) {
                logger.warn("Nimbus is not available", ex);
            }
            if (!PreprocessingSession.isJavaVersionCorrect()) {
                final String msg = "You are using an incompartible version of Java: '" + PreprocessingSession.javaVersion() +
                        "'. Java 1.8.xxx 64bit is required. The application will not work stable!";
                logger.error(msg);
                JOptionPane.showMessageDialog(null, msg, "Application start error", JOptionPane.ERROR_MESSAGE);
            }

            if (!PreprocessingSession.isJavaPlatformCorrect()) {
                final String msg = "You are using an incompartible platform of Java: '" + PreprocessingSession.javaPlatform() +
                        "'. Java 1.8.xxx 64bit is required. The application will not work stable!";
                logger.error(msg);
                JOptionPane.showMessageDialog(null, msg, "Application start error", JOptionPane.ERROR_MESSAGE);
            }
            frame = new Form();
            frame.setContentPane(new FramePanel("", (dir) -> frame.onSetTitle(dir) ));
            frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
            frame.onSetTitle("");
            frame.pack();
            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
            frame.setVisible(true);
        } catch (Exception ex) {
            EH.apply().errorAndMessageBox("Exception in main", ex);
        }
    }

    private void onSetTitle(final String dir){
        frame.setTitle(dir.isEmpty() ? Title : String.format("[%s] - %s", dir, Title));
    }

    public static String getStackTrace(final Throwable ex) {
        StringWriter stack = new StringWriter();
        ex.printStackTrace(new PrintWriter(stack));
        return stack.toString();
    }

}
