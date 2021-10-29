import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SelectAreaHandler extends MouseAdapter {
    private final JPanel selectArea;
    private final Container container;
    private Point pPressed, pointSelected;
    private Dimension dimensionSelected;

    public SelectAreaHandler(Container container) {
        selectArea = new JPanel();
        selectArea.setBackground(TrayApp.trueTransparent);
        selectArea.setBorder(BorderFactory.createLineBorder(Color.red));
        container.add(selectArea, "hidemode 3");
        container.setLayout(new MigLayout());
        this.container = container;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        selectArea.setVisible(true);
        pPressed = e.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);
        Point pDragged = e.getPoint();
        int leftX = Math.min(pPressed.x, pDragged.x);
        int rightX = Math.max(pPressed.x, pDragged.x);
        int leftY = Math.min(pPressed.y, pDragged.y);
        int rightY = Math.max(pPressed.y, pDragged.y);
        selectArea.setBounds(leftX, leftY, rightX - leftX, rightY - leftY);
        pointSelected = selectArea.getLocation();
        dimensionSelected = selectArea.getSize();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        container.setBackground(TrayApp.trueTransparent);
        selectArea.setVisible(false);
        container.repaint();
    }

    public Point getPointSelected() {
        return pointSelected;
    }

    public Dimension getDimensionSelected() {
        return dimensionSelected;
    }
}
