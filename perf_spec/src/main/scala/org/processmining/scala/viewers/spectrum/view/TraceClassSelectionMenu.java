package org.processmining.scala.viewers.spectrum.view;

import javax.swing.*;

public class TraceClassSelectionMenu  extends JPopupMenu {
    JMenuItem anItem;
    public TraceClassSelectionMenu(){
        anItem = new JMenuItem("All");
        add(anItem);
    }
}
