package masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_bookcase;

import org.opencv.core.Point;
import org.opencv.core.Scalar;

import masterarbeit_thilo.hci.luh.de.visualbooksearch.Region;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines.LineSegment;

// Stellt ein Fach des Bücherregals dar
public class CompartmentRegion extends Region {

    private static int counter = 0;

    private int id;

    public CompartmentRegion(Point boundMin, Point boundMax, double angleOffset) {
        super(boundMin, boundMax);
        if (Debug.DEBUGGING) {
            id = ++counter;
            Debug.drawLine(Debug.FILTER_2_COMPARTMENTS, boundMin, new Point(boundMax.x, boundMin.y), Debug.GREEN, 25);
            Debug.drawLine(Debug.FILTER_2_COMPARTMENTS, new Point(boundMin.x, boundMax.y), boundMax, Debug.GREEN, 25);
            Debug.drawText(Debug.FILTER_2_COMPARTMENTS, new Point((boundMin.x + boundMax.x) / 2, (boundMin.y + boundMax.y) / 2), 15, "" + id, Debug.GREEN);
        }
    }

    @Override
    protected void drawLine(LineSegment line, Scalar color) {
        //if (Debug.DEBUGGING) Debug.drawLine(Debug.FILTER_2_COMPARTMENTS, line, color, 1);
    }

    public int getId() {
        return id;
    }

}