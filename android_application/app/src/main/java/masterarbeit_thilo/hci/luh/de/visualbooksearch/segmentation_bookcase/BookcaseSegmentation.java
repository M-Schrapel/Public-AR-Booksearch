package masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_bookcase;

import android.util.Log;

import org.opencv.core.Point;
import org.opencv.core.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines.LineSegment;

public class BookcaseSegmentation {

    private static final String TAG = "BookcaseSegmentation";

    // Mindestbreite einer Linie in Abhängigkeit der Bildbreite, um als Begrenzungslinie eines Regalfaches in Frage zu kommen
    private static final float BOOKCASE_WIDTH_FACTOR = 0.5f;

    // Mindesthöhe zwischen zwei Linien in Abhängigkeit der Bildhöhe, sodass der Zwischenraum als Regalfach gilt
    private static final float BOOKCASE_HEIGHT_FACTOR = 0.1f;

    // Breite und Höhe des Bildes
    private int width, height;

    public BookcaseSegmentation(Size size) {
        width = (int) size.width;
        height = (int) size.height;
    }

    // Bestimmt die Regionen der Regalfächer (4.4.2)
    public ArrayList<CompartmentRegion> getCompartments(ArrayList<LineSegment> horizontalLines) {
        ArrayList<CompartmentRegion> bookcaseCompartments = new ArrayList<>();
        ArrayList<LineSegment> lines = new ArrayList<>();

        // Finden von langen Linien, welche für eine Begrenzungslinie eines Regalfaches in Frage zu kommen
        horizontalLines.sort(Comparator.comparingDouble(LineSegment::getLength));
        Collections.reverse(horizontalLines);
        for (LineSegment line : horizontalLines) {
            Log.d(TAG, "line.getLength(): " + line.getLength());
            // ist die Linie kürzer, so kann abgebrochen werden, da aufgrund der Sortierung nur noch kürzere Linien folgen
            if (line.getLength() < width * BOOKCASE_WIDTH_FACTOR) break;
            lines.add(line);
            if (Debug.DEBUGGING) Debug.drawLine(Debug.FILTER_2_COMPARTMENT_LINES, line, Debug.YELLOW, 25);
        }

        // Abstände zwischen zwei benachbarten Linien berechnen und ggf. eine neue CompartmentRegion erstellen
        lines.sort(Comparator.comparingDouble(LineSegment::getVerticalPosition));
        int yStart = 0;
        double angleOffset = 0;
        for (LineSegment line : lines) {
            int yEnd = Math.max(line.getIntersectionLeft(), line.getIntersectionRight());
            if (yEnd - yStart > height * BOOKCASE_HEIGHT_FACTOR) {
                angleOffset = line.getAngle() > Math.toRadians(90) ? line.getAngle() - Math.toRadians(180) : -line.getAngle();
                Log.d(TAG, "line.getAngle(): " + Math.toDegrees(line.getAngle()));
                if (yEnd >= height) yEnd = height - 1;
                bookcaseCompartments.add(
                        new CompartmentRegion(new Point(0, yStart), new Point(width, yEnd), angleOffset));
            }
            yStart = yEnd;
        }
        if (height - yStart > height * BOOKCASE_HEIGHT_FACTOR) {
            bookcaseCompartments.add(
                    new CompartmentRegion(new Point(0, yStart), new Point(width - 1, height - 1), angleOffset));
        }
        return bookcaseCompartments;
    }
}
