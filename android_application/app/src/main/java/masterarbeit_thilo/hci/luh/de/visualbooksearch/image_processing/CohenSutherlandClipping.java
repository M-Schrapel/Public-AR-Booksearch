package masterarbeit_thilo.hci.luh.de.visualbooksearch.image_processing;

import org.opencv.core.Point;

import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines.LineSegment;

// Cohen-Sutherland algorithm based on https://en.wikipedia.org/wiki/Cohen-Sutherland_algorithm
public class CohenSutherlandClipping {

    private static final int INSIDE = 0;    // 0000
    private static final int LEFT = 1;  // 0001
    private static final int RIGHT = 2; // 0010
    private static final int BOTTOM = 4; // 0100
    private static final int TOP = 8; // 1000

    private int xMin, yMin, xMax, yMax;
    private LineSegment clippedLine;

    public CohenSutherlandClipping(Point boundMin, Point boundMax) {
        xMin = (int) boundMin.x;
        yMin = (int) boundMin.y;
        xMax = (int) boundMax.x;
        yMax = (int) boundMax.y;
    }

    // Cohen–Sutherland clipping algorithm clips a line from
    // P0 = (x0, y0) to P1 = (x1, y1) against a rectangle with
    // diagonal from (xMin, yMin) to (XMax, YMax).
    // returns true if line was clipped or lies outside of the rectangle
    public boolean clip(LineSegment line) {
        clippedLine = null;
        int x0 = (int) line.getBot().x;
        int y0 = (int) line.getBot().y;
        int x1 = (int) line.getTop().x;
        int y1 = (int) line.getTop().y;

        // compute outcodes for P0, P1, and whatever point lies outside the clip rectangle
        int outcode0 = computeOutCode(x0, y0);
        int outcode1 = computeOutCode(x1, y1);
        if ((outcode0 | outcode1) == 0) return false;
        boolean accept = false;

        while (true) {
            if ((outcode0 | outcode1) == 0) {
                // bitwise OR is 0: both points inside window; trivially accept and exit loop
                accept = true;
                break;
            } else if ((outcode0 & outcode1) != 0) {
                // bitwise AND is not 0: both points share an outside zone (LEFT, RIGHT, TOP,
                // or BOTTOM), so both must be outside window; exit loop (accept is false)
                break;
            } else {
                // failed both tests, so calculate the line segment to clip
                // from an outside point to an intersection with clip edge
                int x = 0, y = 0;

                // At least one endpoint is outside the clip rectangle; pick it.
                int outcodeOut = (outcode0 != 0) ? outcode0 : outcode1;

                // Now find the intersection point;
                // use formulas:
                //   slope = (y1 - y0) / (x1 - x0)
                //   x = x0 + (1 / slope) * (ym - y0), where ym is ymin or ymax
                //   y = y0 + slope * (xm - x0), where xm is xmin or xmax
                // No need to worry about divide-by-zero because, in each case, the
                // outcode bit being tested guarantees the denominator is non-zero
                if ((outcodeOut & TOP) != 0) {           // point is above the clip window
                    x = x0 + (x1 - x0) * (yMax - y0) / (y1 - y0);
                    y = yMax;
                } else if ((outcodeOut & BOTTOM) != 0) { // point is below the clip window
                    x = x0 + (x1 - x0) * (yMin - y0) / (y1 - y0);
                    y = yMin;
                } else if ((outcodeOut & RIGHT) != 0) {  // point is to the right of clip window
                    y = y0 + (y1 - y0) * (xMax - x0) / (x1 - x0);
                    x = xMax;
                } else if ((outcodeOut & LEFT) != 0) {   // point is to the left of clip window
                    y = y0 + (y1 - y0) * (xMin - x0) / (x1 - x0);
                    x = xMin;
                }

                // Now we move outside point to intersection point to clip
                // and get ready for next pass.
                if (outcodeOut == outcode0) {
                    x0 = x;
                    y0 = y;
                    outcode0 = computeOutCode(x0, y0);
                } else {
                    x1 = x;
                    y1 = y;
                    outcode1 = computeOutCode(x1, y1);
                }
            }
        }
        if (accept) {
            clippedLine = new LineSegment(new Point(x0, y0), new Point(x1, y1));
        }
        return true;
    }

    // Compute the bit code for a point (x, y) using the clip rectangle
    // bounded diagonally by (xmin, ymin), and (xmax, ymax)

    // ASSUME THAT xmax, xmin, ymax and ymin are global constants.
    private int computeOutCode(double x, double y) {
        int code;
        code = INSIDE;          // initialised as being inside of [[clip window]]

        if (x < xMin)           // to the left of clip window
            code |= LEFT;
        else if (x > xMax)      // to the right of clip window
            code |= RIGHT;
        if (y < yMin)           // below the clip window
            code |= BOTTOM;
        else if (y > yMax)      // above the clip window
            code |= TOP;

        return code;
    }

    public LineSegment getClippedLine() {
        return clippedLine;
    }

}
