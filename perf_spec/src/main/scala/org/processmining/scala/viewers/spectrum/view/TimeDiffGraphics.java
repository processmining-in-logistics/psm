package org.processmining.scala.viewers.spectrum.view;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.processmining.scala.log.common.utils.common.EH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.processmining.scala.viewers.spectrum.model.AbstractDataSource;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

final class TimeDiffGraphics extends JPanel implements MouseWheelListener {

    private static final Logger logger = LoggerFactory.getLogger(TimeDiffGraphics.class.getName());
    public static int AllClasses = -1;
    final TimeDiffController tdf;
    final AbstractDataSource ds;
    ViewerState viewerState = ViewerState.EmptyState();
    PaintInputParameters paintInputParameters = PaintInputParameters.EmptyParameters();
    private final ViewerSettings viewerSettings = ViewerSettings.apply();
    private Dimension dimension = getSize();
    private Point startDrag, endDrag;
    private BufferedImage image;


    private Zooming zooming;

    public void setZooming(final Zooming zooming) {
        this.zooming = zooming;
        addMouseWheelListener(this);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (zooming != null) {
            if (e.isShiftDown()) {
                zooming.changeVerticalZoom(e.getWheelRotation());
            }
            if (e.isControlDown()) {
                zooming.changeHorizontalZoom(e.getWheelRotation());
            }
        }
    }

    interface PopupHandler {
        void handler(final int clazz, final MouseEvent e);
    }



    public TimeDiffGraphics(final TimeDiffController tdf)  {
        this.tdf = tdf;
        ds = tdf.ds();

        try {
            image = ImageIO.read(new File("./res/logo.png"));
        }catch(IOException ex){
            image = null;
        }



        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                try {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        startDrag = new Point(e.getX(), e.getY());
                        endDrag = startDrag;
                        //viewerState = viewerState.copyWithGrid(true);
                        //forceRepaint();
                    } else {
                        doPop(e, (clazz, e1) -> showTracesByClass(clazz, e1));
                    }
                } catch (Exception ex) {
                    EH.apply().error("Mouse pressed", ex);
                }
            }

            public void mouseReleased(final MouseEvent e) {
                try {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        if (!startDrag.equals(new Point(e.getX(), e.getY()))) {
                            doPop(e, (clazz, e1) -> firePopup(clazz, e1));
                        }
                    }
                } catch (Exception ex) {
                    logger.error(ex.toString());
                }
            }


            private void doPop(final MouseEvent e, final PopupHandler handler) {
                final JPopupMenu menu = new JPopupMenu();
                {
                    JMenuItem menuItem = new JMenuItem("All");
                    menuItem.addActionListener(ae -> handler.handler(AllClasses, e));
                    menu.add(menuItem);
                }
                final List<String> legend = LegendPanel.splitLegend(tdf.view().ds.legend());
                for (int clazz = 0; clazz < legend.size() - 1; clazz++) {
                    final int currentClazz = clazz;
                    final JMenuItem menuItem = new JMenuItem(legend.get(clazz + 1));
                    menuItem.addActionListener(ae -> handler.handler(currentClazz, e));
                    menu.add(menuItem);
                }
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        this.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                try {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        endDrag = new Point(e.getX(), e.getY());
                        forceRepaint();
                    }
                } catch (Exception ex) {
                    logger.error(ex.toString());
                }
            }

            public void mouseMoved(java.awt.event.MouseEvent evt) {
                try {
                    if (onMouseTimeChanged != null) {
                        final int x = evt.getPoint().x;
                        final long absTime = ds.startTimeMs() + getTwIndexAndTailByX(x).getLeft() * ds.twSizeMs() + getTwIndexAndTailByX(x).getRight();
                        onMouseTimeChanged.handler(absTime);
                    }
                } catch (Exception ex) {
                    logger.error(ex.toString());
                }
            }

        });
    }

    void firePopup(final int clazz, final MouseEvent e) {
        System.out.print(Integer.toString(clazz));
        selectTraces(makeRectangle(startDrag.x, startDrag.y, e.getX(), e.getY()), clazz);
        startDrag = null;
        endDrag = null;
        forceRepaint();
    }

    private void selectTraces(final Shape r, final int clazz) {
        final int left = getTwIndexAndTailByX(r.getBounds().x).getLeft() + 1;
        final int right = getTwIndexAndTailByX(r.getBounds().x + r.getBounds().width).getLeft() - 1;
        final int bottom = getNameIndexByY(r.getBounds().y) + 1;
        final int top = getNameIndexByY(r.getBounds().y + r.getBounds().height) - 1;
        final int width = right - left;
        final int height = top - bottom;
        if (width < 0 || height < 0) {
            Toolkit.getDefaultToolkit().beep();

        } else {
            final Rectangle selectedSegmentsArea = new Rectangle(left, bottom, width + 1, height + 1);
            logger.info(String.format("x=%d y=%d w=%d h=%d", selectedSegmentsArea.x, selectedSegmentsArea.y, selectedSegmentsArea.width, selectedSegmentsArea.height));
            tdf.setSelectedIds(selectedSegmentsArea, clazz);
        }
    }

    public void clearSelectionMode() {
        tdf.clearSelectionMode();
        forceRepaint();
    }

    public void showTracesByClass(final int clazz, final MouseEvent me) {
        tdf.showTracesByClazz(clazz);
        forceRepaint();

    }

    public interface TimeDiffEventListener {
        void handler(final long timeMs);
    }

    //
//    private void fireSelectionEvent() {
//        if (onSelection != null) {
//            onSelection.handler();
//        }
//    }
    private TimeDiffEventListener onMouseTimeChanged;

    void subscribeToSelectionEvent(TimeDiffEventListener onMouseTimeChanged) {
        this.onMouseTimeChanged = onMouseTimeChanged;

    }


    private Rectangle2D.Float makeRectangle(int x1, int y1, int x2, int y2) {
        return new Rectangle2D.Float(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        try {
            super.paintComponent(g);
            if (!this.viewerState.equals(ViewerState.EmptyState()) && paintInputParameters.names().length > 0) {
                tdf.paint(g2);
//                if (viewerState.showGrid()) {
//                    drawHorizontalGrid(g2);
//                    drawVerticalGrid(g2);
//                }


                //g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


//        if(selectedSegmentsArea != null) {
//            g2.setPaint(Color.BLACK);
//            g2.draw(selectedSegmentsArea);
//            g2.setPaint(Color.MAGENTA);
//            g2.fill(selectedSegmentsArea);
//        }

                if (startDrag != null && endDrag != null) {
//            g2.setStroke(new BasicStroke(2));
//            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50f));
                    g2.setPaint(Color.RED);
                    Shape r = makeRectangle(startDrag.x, startDrag.y, endDrag.x, endDrag.y);
                    g2.draw(r);
//            g2.setPaint(Color.LIGHT_GRAY);
//            g2.fill(r);
                }
            }else{
//                if(image != null) {
//                    g.drawImage(image, 0, 0, null);
//                }
            }
        } catch (Exception ex) {
            EH.apply().error("paintComponent", ex);
        }
    }


    private void drawLine(final Graphics2D g2, final double x1, final double y1, final double x2, final double y2) {
        g2.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
    }


    Pair<Integer, Integer> getYIntervalInclusiveByIndex(final int y) {
        final double yRatio = getYRatio();
        final int left = (int) (y * viewerSettings.segmentHeightPx() * yRatio);
        final int right = Math.max(y, (int) ((y + 1) * viewerSettings.segmentHeightPx() * yRatio) - 1);
        return new ImmutablePair<>(left, right);
    }

    double getTwWidthPx() {
        return ((double) (getWidth()) / paintInputParameters.twsFitIntoScreen());
    }

    Pair<Integer, Integer> getXIntervalInclusiveByTwIndex(final long absTwIndex) {
        final long relTwIndex = absTwIndex - paintInputParameters.startTwIndex();
        final long n = paintInputParameters.twsFitIntoScreen();
        final int left = (int) (getTwWidthPx() * relTwIndex);
        final int right = Math.max(left, (int) ((double) (getWidth()) / n * (relTwIndex + 1)) - 1);
        return new ImmutablePair<>(left, right);
    }

    int getXByAbsTime(final long timestamp) {
        final double relTimestamp = timestamp - ds.startTimeMs();
        final double twIndexDouble = relTimestamp / ds.twSizeMs();
        final long twIndex = (long) twIndexDouble;
        final double tailTwIndex = twIndexDouble - twIndex;
        final int shiftX = (int) (getTwWidthPx() * tailTwIndex);
        return getXIntervalInclusiveByTwIndex(twIndex).getLeft() + shiftX;

    }


    private int getNameIndexByY(final int y) {
        final double yRatio = getYRatio();
        return (int) (y / yRatio / viewerSettings.segmentHeightPx());
    }

    private Pair<Integer, Long> getTwIndexAndTailByX(final int x) {
        final long n = paintInputParameters.twsFitIntoScreen();
        final double twWidthPx = ((double) (getWidth())) / n;
        final int relTwIndex = (int) (x / twWidthPx);
        final long tailMs = (long) (((x / twWidthPx) - relTwIndex) * ds.twSizeMs());
        final int twIndex = paintInputParameters.startTwIndex() + relTwIndex;
        return new ImmutablePair<>(twIndex, tailMs);
    }

    String getNameByIndex(final int y) {
        if (y < paintInputParameters.names().length)
            return paintInputParameters.names()[y];
        else return "";
    }

    private double getYRatio() {
        return viewerState.yZoom() / MainPanel.HundredPercent * MainPanel.SliderYZoomUnitZoomingPercent;
    }

    private final Stroke thick = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
    private final Stroke thin = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0);

    void drawCustomGrid(final Graphics2D g2, final long date, final boolean isThin) {
        g2.setColor(TimeDiffController.getDefaultGridColor());
        g2.setStroke(isThin ? thin : thick);
        final int x = getXByAbsTime(date);
        drawLine(g2, x, 0, x, getHeight());
    }

    void drawHorizontalGrid(final Graphics2D g2) {
        g2.setColor(TimeDiffController.getDefaultGridColor());
        for (int i = 0; i < paintInputParameters.names().length; i++) {
            final Pair<Integer, Integer> yy = getYIntervalInclusiveByIndex(i);
            drawLine(g2, 0, yy.getRight(), getWidth(), yy.getRight());
            drawLine(g2, 0, yy.getLeft(), getWidth(), yy.getLeft()); //???
        }
    }

    void drawVerticalGrid(final Graphics2D g2) {
        g2.setColor(TimeDiffController.getDefaultGridColor());
        for (long twIndex = paintInputParameters.startTwIndex(); twIndex < paintInputParameters.lastTwIndexExclusive(); twIndex++) {
            final Pair<Integer, Integer> pair = getXIntervalInclusiveByTwIndex(twIndex);
            drawLine(g2, pair.getLeft(), 0, pair.getLeft(), getHeight());
            drawLine(g2, pair.getRight(), 0, pair.getRight(), getHeight());
        }
    }

    void adjustPanelSize() {
        final int panelHeightPx = (int) (viewerSettings.segmentHeightPx() * paintInputParameters.names().length * getYRatio());
        final int panelWidthPx = dimension.width - viewerSettings.outerBorderPx();
        final Rectangle oldBounds = getBounds();
        final Rectangle bounds = new Rectangle(oldBounds.x, oldBounds.y, panelWidthPx, panelHeightPx);
        if (!bounds.equals(oldBounds)) {
            setBounds(bounds);
            setPreferredSize(new Dimension(bounds.width, bounds.height));
        }
    }

    public void onComponentResized(final Dimension dimension) {
        this.dimension = dimension;
    }

    int adjustVisualizationParamsAndRepaint(final ViewerState viewerState, final PaintInputParameters paintInputParameters) {
        this.viewerState = viewerState;
        this.paintInputParameters = paintInputParameters;
        adjustPanelSize();
        forceRepaint();
        return (int) (viewerSettings.segmentHeightPx() * getYRatio());
    }

    public void forceRepaint() {
        invalidate();
        repaint();
    }


}
