package masterarbeit_thilo.hci.luh.de.visualbooksearch;

import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.util.ArrayList;

import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.image_processing.BookSearch;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.image_processing.CohenSutherlandClipping;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines.LineSegment;

// Stellt ein Teil bzw. eine Region des Bildes dar (Buchfach oder Buchausrichtung)
public abstract class Region {

    // Linien, die sich in dieser Region befinden
    protected ArrayList<LineSegment> horizontalLines = new ArrayList<>();
    protected ArrayList<LineSegment> verticalLines = new ArrayList<>();

    private Point boundMin, boundMax;
    private CohenSutherlandClipping cohenSutherlandClipping;
    private Scalar color;

    public Region(Point boundMin, Point boundMax) {
        this.boundMin = boundMin;
        this.boundMax = boundMax;
        cohenSutherlandClipping = new CohenSutherlandClipping(boundMin, boundMax);
        if (Debug.DEBUGGING) color = Debug.getRandomColor();
    }

    // Hinzufügen von Linien: mit dem Cohen-Sutherland-Clipping Algorithmus wird geprüft,
    // ob sich die Linie ganz oder teilweise innerhalb der Region befindet
    public boolean add(LineSegment line, boolean vertical) {
        if (cohenSutherlandClipping.clip(line)) {
            LineSegment clippedLine = cohenSutherlandClipping.getClippedLine();
            if (clippedLine != null && clippedLine.getLength() > BookSearch.LENGTH_THRESHOLD) {
                addLineToArray(clippedLine, vertical);
                if (Debug.DEBUGGING) drawLine(clippedLine, color);
            }
            return false; // Linie ist nur teilweise oder gar nicht in der Region
        } else {
            addLineToArray(line, vertical);
            if (Debug.DEBUGGING) drawLine(line,color);
            return true; // Linie ist komplett in der Region
        }
    }

    private void addLineToArray(LineSegment line, boolean vertical) {
        if (vertical) verticalLines.add(line);
        else horizontalLines.add(line);
    }

    public ArrayList<LineSegment> getVerticalLines() {
        return verticalLines;
    }

    public ArrayList<LineSegment> getHorizontalLines() {
        return horizontalLines;
    }

    public Point getBoundMin() {
        return boundMin;
    }

    public Point getBoundMax() {
        return boundMax;
    }

    public Scalar getColor() {
        return color;
    }

    protected abstract void drawLine(LineSegment line, Scalar color);

}
