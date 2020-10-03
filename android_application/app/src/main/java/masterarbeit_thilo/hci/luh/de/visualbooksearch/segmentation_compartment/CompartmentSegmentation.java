package masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_compartment;

import android.util.Log;

import org.opencv.core.Point;
import org.opencv.core.Size;

import java.util.ArrayList;
import java.util.Comparator;

import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines.LineSegment;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_bookcase.CompartmentRegion;

import static masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug.DEBUGGING;

public class CompartmentSegmentation {

    private static final String TAG = "CompartmentSegmentation";
    private static final int HORIZONTAL_DIST_MEAN_FACTOR = 4;
    private static final int HORIZONTAL_MIN_DIST = 20;

    private int width, height;

    public CompartmentSegmentation(Size size) {
        width = (int) size.width;
        height = (int) size.height;
    }

    public ArrayList<AlignmentRegion> getAlignments(CompartmentRegion compartment) {
        Log.d(TAG, "Compartment Id: " + compartment.getId());
        ArrayList<AlignmentRegion> alignmentRegions = new ArrayList<>();
        ArrayList<LineSegment> lines = new ArrayList<>();

        ArrayList<LineSegment> verticalLines = compartment.getVerticalLines();
        if (verticalLines.isEmpty()) return new ArrayList<>();
        verticalLines.sort(Comparator.comparingDouble(LineSegment::getLength));
        double vLengthMedian = verticalLines.get(verticalLines.size() / 2).getLength();
        double vLengthMean = 0;
        for (LineSegment line : verticalLines) {
            //if (DEBUGGING) Debug.drawLine(Debug.TEST, line, Debug.YELLOW, 8);
            //Log.d(TAG, "line.getLength(): " + line.getLength());
            vLengthMean += line.getLength();
        }
        vLengthMean /= verticalLines.size();

        if (DEBUGGING) {
            Point boundMin = compartment.getBoundMin();
            Debug.drawLine(Debug.FILTER_3_BOOK_REGION_LINES,
                    new Point(boundMin.x + 10, boundMin.y + 10),
                    new Point(boundMin.x + 10, boundMin.y + 10 + vLengthMean),
                    Debug.RED, 3);
            Debug.drawLine(Debug.FILTER_3_BOOK_REGION_LINES,
                    new Point(boundMin.x + 30, boundMin.y + 10),
                    new Point(boundMin.x + 30, boundMin.y + 10 + vLengthMedian),
                    Debug.RED, 3);
        }
        Log.d(TAG, "vLengthMean: " + vLengthMean);
        Log.d(TAG, "vLengthMedian: " + vLengthMedian);

        for (LineSegment line : verticalLines) {
            if (line.getLength() >= vLengthMean) {
                lines.add(line);
                if (DEBUGGING) Debug.drawLine(Debug.FILTER_3_BOOK_REGION_LINES, line, Debug.YELLOW, 2);
            }
        }
        lines.sort(Comparator.comparingDouble(LineSegment::getHorizontalPosition));
        int xLeft = 0; // linke Begrenzungslinie
        double hDistMean = 0; // durchschnittlicher horizontaler Abstand
        int count = 0;
        for (LineSegment line : lines) {
            int xRight = line.getHorizontalPosition(); // rechte Begrenzungslinie
            int hDist = (xRight - xLeft); // horizontaler Abstand zwischen linker und rechter Begrenzungslinie
            if (hDist > HORIZONTAL_MIN_DIST) {
                //if (DEBUGGING) Debug.drawLine(Debug.TEST, line, Debug.YELLOW, 8);
                count++;
                hDistMean += hDist;
                xLeft = xRight;
            }
        }
        hDistMean /= count;
        Log.d(TAG, "hDistMean: " + hDistMean);

        double yMin = compartment.getBoundMin().y;
        double yMax = compartment.getBoundMax().y;
        double xStart = 0; // Anfangspunkt einer AlignmentRegion
        LineSegment lineLeft = null; // linke Begrenzugslinie der Region
        xLeft = 0; // Position der linken Begrenzugslinie
        for (LineSegment lineRight : lines) {
            int xRight = lineRight.getHorizontalPosition(); // // Position der rechten Begrenzugslinie
            int hDist = xRight - xLeft; // horizontaler Abstand zwischen zwei Linien
            if (hDist > hDistMean * HORIZONTAL_DIST_MEAN_FACTOR) {
                if (lineLeft != null) {
                    alignmentRegions.add(new AlignmentRegion(new Point(xStart, yMin), new Point(lineLeft.getRight().x, yMax), true));
                    alignmentRegions.add(new AlignmentRegion(new Point(lineLeft.getRight().x, yMin), new Point(lineRight.getLeft().x, yMax), false));
                } else {
                    alignmentRegions.add(new AlignmentRegion(new Point(xStart, yMin), new Point(lineRight.getLeft().x, yMax), false));
                }
                xStart = lineRight.getLeft().x;
            }
            xLeft = xRight; // Position der rechten Begrenzungslinie wird zur Position der linken
            lineLeft = lineRight; // rechte Begrenzungslinie wird zur linken
        }
        int hDist = width - xLeft;
        if (hDist > hDistMean * HORIZONTAL_DIST_MEAN_FACTOR) {
            if (lineLeft != null) {
                alignmentRegions.add(new AlignmentRegion(new Point(xStart, yMin), new Point(lineLeft.getRight().x, yMax), true));
                alignmentRegions.add(new AlignmentRegion(new Point(lineLeft.getRight().x, yMin), new Point(width, yMax), false));
            } else {
                alignmentRegions.add(new AlignmentRegion(new Point(xStart, yMin), new Point(width, yMax), false));
            }

        }
        else alignmentRegions.add(new AlignmentRegion(new Point(xStart, yMin), new Point(width, yMax), true));
        return alignmentRegions;
    }

}
