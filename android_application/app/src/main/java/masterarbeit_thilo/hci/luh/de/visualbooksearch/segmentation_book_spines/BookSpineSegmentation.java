package masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_compartment.AlignmentRegion;

import static masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug.DEBUGGING;

public class BookSpineSegmentation {

    private static final String TAG = "segmentation_book_spine BookSpineSegmentation";

    private static final double ORTHOGONAL_ANGLE = Math.toRadians(90);

    // um wie viel darf die horizontale Begrenzungslinie vom orthogonalen Winkel abweichen
    private static final double ORTHOGONAL_THRESHOLD = Math.toRadians(10);

    // wie breit muss die horizontale Begrenzungslinie mindestens sein abhängig von der Breite des Buchrückens
    private static final double HORIZONTAL_SPINE_WIDTH_FACTOR = 0.2;

    // um wie viel darf die horizontale Begrenzungslinie die Länge des Buchrückens maximal verlängern
    private static final double VERTICAL_EXTENDING_FACTOR = 0.1;

    // um wie viel darf die horizontale Begrenzungslinie die Länge des Buchrückens maximal verringern
    private static final double VERTICAL_SHRINKING_FACTOR = 0.25;

    // Breite und Höhe, die ein fertig segmentierter Buchrücken mindestens haben muss
    private static final double BOOK_SPINE_MIN_WIDTH = 10;
    private static final double BOOK_SPINE_MIN_HEIGHT = 20;

    private Mat src;

    public BookSpineSegmentation(Mat src) {
        this.src = src;
    }

    // segmentiert aus einer Buchregion alle Buchrücken
    public ArrayList<BookSpine> segmentBookSpines(AlignmentRegion region) {
        boolean horizontal = region.isHorizontal();
        ArrayList<LineSegment> verticalLines = region.getVerticalLines();
        ArrayList<LineSegment> horizontalLines = region.getHorizontalLines();
        ArrayList<BookSpine> bookSpines = new ArrayList<>();
        LineSegmentMerging lineSegmentMerging;
        if (horizontal) { // falls die Bücher liegen, dann werden sie vorher gedreht
            for (LineSegment line : verticalLines) {
                line.swapXY();
            }
            for (LineSegment line : horizontalLines) {
                line.swapXY();
            }
            ArrayList<LineSegment> tmpLines = horizontalLines;
            horizontalLines = verticalLines;
            verticalLines = tmpLines;
            lineSegmentMerging = new LineSegmentMerging(region.getBoundMin().x, region.getBoundMax().x);
        } else {
            lineSegmentMerging = new LineSegmentMerging(region.getBoundMin().y, region.getBoundMax().y);
        }
        ArrayList<LineSegmentCluster> clusters = lineSegmentMerging.merge(verticalLines);
        ArrayList<LineSegmentCluster> spineRegions = lineSegmentMerging.calculateVerticalSpineRegions(clusters, region);

        /*
            Nachdem die vertikalen Abgrenzungen der Buchrücken bekannt sind, müssen nun die horizontalen
            (unten + oben) gefunden werden. Es wird zuerst geprüft, ob sich die horizontale Linie zwischen
            der linken und rechten (firstBound, secondBound) Begrenzung befindet.
        */
        if (DEBUGGING && !spineRegions.isEmpty()) {
            if (horizontal) Debug.drawSwappedLine(Debug.FILTER_7_HORIZONTAL_BOUND, spineRegions.get(0).getBot(), spineRegions.get(0).getTop(), Debug.YELLOW, 3);
            else Debug.drawLine(Debug.FILTER_7_HORIZONTAL_BOUND, spineRegions.get(0).getBot(), spineRegions.get(0).getTop(), Debug.YELLOW, 3);
        }
        for (int i = 1; i < spineRegions.size(); i++) {
            //Log.d(TAG, "spineRegions.get(" + i + ").getLength(): " + spineRegions.get(i).getLength());
            LineSegmentCluster firstBound = spineRegions.get(i-1); // linke vertikale Begrenzungslinie
            LineSegmentCluster secondBound = spineRegions.get(i); // rechte vertikale Begrenzungslinie
            double spineWidth = firstBound.getWidth(secondBound);
            double spineHeightMean = ((firstBound.getBot().y - firstBound.getTop().y) + (secondBound.getBot().y - secondBound.getTop().y)) / 2;
            if (DEBUGGING) {
                if (horizontal) Debug.drawSwappedLine(Debug.FILTER_7_HORIZONTAL_BOUND, secondBound.getBot(), secondBound.getTop(), Debug.YELLOW, 3);
                else Debug.drawLine(Debug.FILTER_7_HORIZONTAL_BOUND, secondBound.getBot(), secondBound.getTop(), Debug.YELLOW, 3);
            }
            double minGapTop = Double.MAX_VALUE, minGapBot = Double.MAX_VALUE;
            LineSegment topBound = null, botBound = null;
            ArrayList<LineSegment> usedLines = new ArrayList<>();
            for (LineSegment line : horizontalLines) {

                // Testen ob Linie ganz oder teilweise zwischen den beiden vertikalen Begrenzungslinien liegt
                boolean leftBeforeFirst = line.getLeft().x < firstBound.calculateX(line.getLeft().y);
                boolean leftBeforeSecond = line.getLeft().x < secondBound.calculateX(line.getLeft().y);
                boolean rightBeforeFirst = line.getRight().x < firstBound.calculateX(line.getRight().y);
                boolean rightBeforeSecond = line.getRight().x < secondBound.calculateX(line.getRight().y);
                LineSegment horizontalBound = null;
                if (leftBeforeFirst && !rightBeforeFirst) {
                    if (rightBeforeSecond) {
                        horizontalBound = new LineSegment(firstBound.getIntersectionPoint(line), line.getRight(), line.getAngle());
                    } else {
                        horizontalBound = new LineSegment(firstBound.getIntersectionPoint(line), secondBound.getIntersectionPoint(line), line.getAngle());
                    }
                } else if (!leftBeforeFirst && leftBeforeSecond) {
                    if (rightBeforeSecond) {
                        horizontalBound = new LineSegment(line.getLeft(), line.getRight(), line.getAngle());
                        usedLines.add(line);
                    } else {
                        horizontalBound = new LineSegment(line.getLeft(), secondBound.getIntersectionPoint(line), line.getAngle());
                    }
                }

                // Falls die Linie als horizontale Begrenzung infrage kommt
                if (horizontalBound != null) {
                    boolean orthogonalToFirst = Math.abs(Math.abs(firstBound.getAngle() - horizontalBound.getAngle())  - ORTHOGONAL_ANGLE) < ORTHOGONAL_THRESHOLD;
                    boolean orthogonalToSecond = Math.abs(Math.abs(secondBound.getAngle() - horizontalBound.getAngle()) - ORTHOGONAL_ANGLE) < ORTHOGONAL_THRESHOLD;

                    // Wenn die Linie zu klein im Verhälnis zur Spinebreite oder nicht orthogonal genug zu den beiden Rändern ist
                    // ,dann mit nächster weitermachen
                    if (horizontalBound.getLength() < spineWidth * HORIZONTAL_SPINE_WIDTH_FACTOR
                            || (!orthogonalToFirst && !orthogonalToSecond)) continue;

                    if (DEBUGGING) {
                        //if (horizontal) Debug.drawSwappedLine(Debug.FILTER_7_HORIZONTAL_BOUND, horizontalBound, Debug.GREEN, 3);
                        //else Debug.drawLine(Debug.FILTER_7_HORIZONTAL_BOUND, horizontalBound, Debug.GREEN, 3);
                    }

                    // es wird nach der besten horizontalen Begrenzungslinie gesucht (4.4.4.5)
                    double gapTop = Math.abs(horizontalBound.getLeft().x - firstBound.getTop().x)
                            + Math.abs(horizontalBound.getRight().x - secondBound.getTop().x)
                            + Math.abs(horizontalBound.getLeft().y - firstBound.getTop().y)
                            + Math.abs(horizontalBound.getRight().y - secondBound.getTop().y);
                    double distTopExtending = (firstBound.getTop().y < secondBound.getTop().y) ?
                            firstBound.getTop().y - horizontalBound.getLeft().y : secondBound.getTop().y - horizontalBound.getRight().y;
                    double distTopShrinking = (firstBound.getTop().y > secondBound.getTop().y) ?
                            horizontalBound.getLeft().y - firstBound.getTop().y : horizontalBound.getRight().y - secondBound.getTop().y;
                    if (distTopExtending >= spineHeightMean * VERTICAL_EXTENDING_FACTOR
                            || distTopShrinking >= spineHeightMean * VERTICAL_SHRINKING_FACTOR) continue;
                    if (DEBUGGING) Debug.drawLine(Debug.FILTER_7_HORIZONTAL_BOUND, horizontalBound, Debug.RED, 3);
                    if (gapTop < minGapTop) {
                        minGapTop = gapTop;
                        topBound = horizontalBound;
                        if (DEBUGGING) {
                            //if (horizontal) Debug.drawSwappedLine(Debug.FILTER_7_HORIZONTAL_BOUND, horizontalBound, Debug.GREEN, 3);
                            //else Debug.drawLine(Debug.FILTER_7_HORIZONTAL_BOUND, horizontalBound, Debug.GREEN, 3);
                        }
                    }
                    /*
                    double gapBot = Math.abs(horizontalBound.getLeft().x - firstBound.getBot().x)
                            + Math.abs(horizontalBound.getRight().x - secondBound.getBot().x)
                            + Math.abs(horizontalBound.getLeft().y - firstBound.getBot().y)
                            + Math.abs(horizontalBound.getRight().y - secondBound.getBot().y);
                    double distBotExtending = (firstBound.getBot().y > secondBound.getBot().y) ?
                            horizontalBound.getLeft().y - firstBound.getBot().y : horizontalBound.getRight().y - secondBound.getBot().y;
                    double distBotShrinking = (firstBound.getBot().y < secondBound.getBot().y) ?
                            firstBound.getBot().y - horizontalBound.getLeft().y : secondBound.getBot().y - horizontalBound.getRight().y;
                    if (gapBot < minGapBot
                            && distBotExtending < spineHeightMean * VERTICAL_EXTENDING_FACTOR
                            && distBotShrinking < spineHeightMean * VERTICAL_SHRINKING_FACTOR) {
                        minGapBot = gapBot;
                        botBound = horizontalBound;
                        if (DEBUGGING) {
                            if (horizontal) Debug.drawSwappedLine(Debug.FILTER_7_HORIZONTAL_BOUND, horizontalBound, Debug.GREEN);
                            Debug.drawLine(Debug.FILTER_7_HORIZONTAL_BOUND, horizontalBound, Debug.GREEN);
                        }
                    }
                    */
                }
            }
            horizontalLines.removeAll(usedLines);

            // Berechnung der Schnittpunkte zwischen der linken, rechten und oberen Begrenzungslinie
            // cornerPoints[0] = oben links
            // cornerPoints[1] = oben rechts
            // cornerPoints[2] = unten rechts
            // cornerPoints[3] = unten links
            Point[] cornerPoints = new Point[4];
            if (topBound != null) {
                cornerPoints[0] = firstBound.getIntersectionPoint(topBound);
                cornerPoints[1] = secondBound.getIntersectionPoint(topBound);
            } else if (firstBound.getMinY() < secondBound.getMinY()) { // hier größere (<) oder kleinere (>) vertical line nutzen?
                cornerPoints[0] = firstBound.getTop();
                cornerPoints[1] = new Point(secondBound.calculateX(firstBound.getMinY()), firstBound.getMinY());
            } else {
                cornerPoints[0] = new Point(firstBound.calculateX(secondBound.getMinY()), secondBound.getMinY());
                cornerPoints[1] = secondBound.getTop();
            }
            if (DEBUGGING) Debug.drawLine(Debug.FILTER_7_HORIZONTAL_BOUND, cornerPoints[0], cornerPoints[1], Debug.GREEN, 3);

            /*
            if (botBound != null) {
                cornerPoints[2] = secondBound.getIntersectionPoint(botBound);
                cornerPoints[3] = firstBound.getIntersectionPoint(botBound);
            } else if (firstBound.getMaxY() > secondBound.getMaxY()) { // hier größere (>) oder kleinere (<) vertical line nutzen?
                cornerPoints[2] = new Point(secondBound.calculateX(firstBound.getMaxY()), firstBound.getMaxY());
                cornerPoints[3] = firstBound.getBot();
            } else {
                cornerPoints[2] = secondBound.getBot();
                cornerPoints[3] = new Point(firstBound.calculateX(secondBound.getMaxY()), secondBound.getMaxY());
            }
            */

            // Berechnung der Schnittpunkte zwischen der linken, rechten und unteren Begrenzungslinie
            if (firstBound.getMaxY() > secondBound.getMaxY()) { // hier tiefere (>) oder höhere (<) vertical line nutzen?
                cornerPoints[2] = new Point(secondBound.calculateX(firstBound.getMaxY()), firstBound.getMaxY());
                cornerPoints[3] = firstBound.getBot();
            } else {
                cornerPoints[2] = secondBound.getBot();
                cornerPoints[3] = new Point(firstBound.calculateX(secondBound.getMaxY()), secondBound.getMaxY());
            }
            if (DEBUGGING) Debug.drawLine(Debug.FILTER_7_HORIZONTAL_BOUND, cornerPoints[2], cornerPoints[3], Debug.GREEN, 3);

            double length01 = Math.sqrt(Math.pow(cornerPoints[0].x - cornerPoints[1].x, 2) + Math.pow(cornerPoints[0].y - cornerPoints[1].y, 2));
            double length23 = Math.sqrt(Math.pow(cornerPoints[2].x - cornerPoints[3].x, 2) + Math.pow(cornerPoints[2].y - cornerPoints[3].y, 2));
            int width = (int) (Math.max(length01, length23) + 0.5);
            double length12 = Math.sqrt(Math.pow(cornerPoints[1].x - cornerPoints[2].x, 2) + Math.pow(cornerPoints[1].y - cornerPoints[2].y, 2));
            double length30 = Math.sqrt(Math.pow(cornerPoints[3].x - cornerPoints[0].x, 2) + Math.pow(cornerPoints[3].y - cornerPoints[0].y, 2));
            int height = (int) (Math.max(length12, length30) + 0.5);
            if (width < BOOK_SPINE_MIN_WIDTH || height < BOOK_SPINE_MIN_HEIGHT) continue;

            Point[] perspectivePoints = new Point[4];
            perspectivePoints[0] = new Point(0, 0);
            perspectivePoints[1] = new Point(width - 1, 0);
            perspectivePoints[2] = new Point(width - 1, height - 1);
            perspectivePoints[3] = new Point(0, height - 1);

            // falls die Bücher liegend engeordnet waren, dann werden sie jetzt wieder zurückgedreht
            if (horizontal) {
                Point[] cornerPointsTemp = new Point[4];
                for (int j = 0; j < cornerPoints.length; j++) {
                    cornerPointsTemp[j] = cornerPoints[j];
                }
                cornerPoints[0] = new Point(cornerPointsTemp[1].y, cornerPointsTemp[1].x);
                cornerPoints[1] = new Point(cornerPointsTemp[0].y, cornerPointsTemp[0].x);
                cornerPoints[2] = new Point(cornerPointsTemp[3].y, cornerPointsTemp[3].x);
                cornerPoints[3] = new Point(cornerPointsTemp[2].y, cornerPointsTemp[2].x);
            }

            // perspektivische Projektion, sodass aus den vier Schnittpunkten (cornerPoints) ein Bild entsteht (bookSpineImg)
            Mat perspectiveMat = Imgproc.getPerspectiveTransform(new MatOfPoint2f(cornerPoints), new MatOfPoint2f(perspectivePoints));
            Mat bookSpineImg = new Mat();
            Imgproc.warpPerspective(src, bookSpineImg, perspectiveMat, new Size(width, height));
            bookSpines.add(new BookSpine(bookSpineImg, cornerPoints));
        }
        return bookSpines;
    }

}
