package masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_compartment;

import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import java.util.ArrayList;

import masterarbeit_thilo.hci.luh.de.visualbooksearch.Region;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines.LineSegment;

// Stellt eine Region mit einer bestimmten Buchausrichtung (vertikal/horizontal) dar
public class AlignmentRegion extends Region {

    private static final String TAG = "AlignmentRegion";

    private static int COUNTER = 0;

    private int id;
    private boolean vertical;

    public AlignmentRegion(Point boundMin, Point boundMax, boolean vertical) {
        super(boundMin, boundMax);
        this.vertical = vertical;
        if (Debug.DEBUGGING) {
            id = ++COUNTER;
            Debug.drawRect(Debug.FILTER_3_BOOK_REGIONS, new Rect(boundMin, boundMax), Debug.GREEN, 10);
            Debug.drawRect(Debug.FILTER_5_CLUSTER_DISCARDED, new Rect(boundMin, boundMax), Debug.GREEN, 1);
            Debug.drawText(Debug.FILTER_5_CLUSTER_DISCARDED, new Point(boundMin.x + 10, boundMin.y + 60), 2, "" + id, Debug.GREEN);
        }
    }

    @Override
    protected void drawLine(LineSegment line, Scalar color) {
        //Debug.drawLine(Debug.FILTER_3_BOOK_REGIONS, line, color, 3);
    }

    public boolean isVertical() {
        return vertical;
    }

    public boolean isHorizontal() {
        return !vertical;
    }

    public int getId() {
        return id;
    }

}
