package masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines;

import android.util.Log;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_compartment.AlignmentRegion;

import static masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug.DEBUGGING;

public class LineSegmentMerging {

    private static final String TAG = "LineSegmentMerging";
    private static final double CLUSTER_LENGTH_MEDIAN_FACTOR = 5;

    private double yTopBorder, yBotBorder;


    public LineSegmentMerging(double yTopBorder, double yBotBorder) {
        this.yTopBorder = yTopBorder;
        this.yBotBorder = yBotBorder;
    }

    /*
     Angefangen mit der längsten Linie wird nacheinander geprüft, ob zwei Linien miteinander
     verbunden werden können, sodass sie zu einem LineSegmentCluster werden
      */
    public ArrayList<LineSegmentCluster> merge(ArrayList<LineSegment> verticalLines) {
        ArrayList<LineSegmentCluster> clusters = new ArrayList<>();
        verticalLines.sort(Comparator.comparingDouble(LineSegment::getLength));
        Collections.reverse(verticalLines);
        for (LineSegment line : verticalLines) {
            //Debug.drawLine(Debug.TEST, line, Debug.getRandomColor(), 2);
            boolean added = false;
            for (LineSegmentCluster cluster : clusters) {
                added = cluster.match(line);
                if (added) {
                    cluster.add(line);
                    break;
                }
            }
            if (!added) {
                clusters.add(new LineSegmentCluster(line, yTopBorder, yBotBorder));
            }
        }
        return clusters;
    }

    // Berechnet die vertikalen Buchbegrenzungslinien (4.4.4)
    public ArrayList<LineSegmentCluster> calculateVerticalSpineRegions(ArrayList<LineSegmentCluster> clusters, AlignmentRegion region) {
        boolean horizontal = region.isHorizontal();
        if (clusters.isEmpty()) return new ArrayList<>();
        Log.d(TAG, "id: " + region.getId());

        // 4.4.4.2
        ArrayList<LineSegmentCluster> spineRegion = discardShortLines(clusters, horizontal);

        // 4.4.4.3
        if (!horizontal) spineRegion = discardTopLines(spineRegion, region.getBoundMax());

        // 4.4.4.4
        mergeCluster(spineRegion);

        spineRegion.sort(Comparator.comparingDouble(LineSegmentCluster::getPosition));
        if (DEBUGGING) Debug.drawCluster(Debug.FILTER_6_CLUSTER_MERGED, horizontal, 3, spineRegion.toArray(new LineSegmentCluster[0]));

        if (!horizontal) addVerticalBorderSegments(region, spineRegion);

        return spineRegion;
    }

    // 4.4.4.2
    private ArrayList<LineSegmentCluster> discardShortLines(ArrayList<LineSegmentCluster> spineRegion, boolean horizontal) {
        ArrayList<LineSegmentCluster> result = new ArrayList<>();
        spineRegion.sort(Comparator.comparingDouble(LineSegmentCluster::getLength));
        double clusterLengthMedian = spineRegion.get(spineRegion.size() / 2).getLength();
        Log.d(TAG, "clusterLengthMedian: " + clusterLengthMedian);
        Log.d(TAG, "clusterLengthThreshold: " + clusterLengthMedian * CLUSTER_LENGTH_MEDIAN_FACTOR);
        for (LineSegmentCluster c : spineRegion) {
            c.calculatePoints();
            if (DEBUGGING) {
                Debug.drawLines(Debug.FILTER_4_CLUSTER_ALL_LINES, horizontal, c, 2);
                Debug.drawCluster(Debug.FILTER_4_CLUSTER_MERGED_LINES, horizontal, 2, c);
            }
            if (c.getLength() > clusterLengthMedian * CLUSTER_LENGTH_MEDIAN_FACTOR) {
                result.add(c);
                if (DEBUGGING) Debug.drawCluster(Debug.FILTER_5_CLUSTER_LENGTH, horizontal, 2, c);
            } else {
                if (DEBUGGING) Debug.drawLines(Debug.FILTER_5_CLUSTER_DISCARDED, horizontal, c, 2);
            }
        }
        return result;
    }

    // 4.4.4.3
    private ArrayList<LineSegmentCluster> discardTopLines(ArrayList<LineSegmentCluster> spineRegion, Point boundMax) {
        ArrayList<LineSegmentCluster> result = new ArrayList<>();
        if (spineRegion.isEmpty()) return spineRegion;
        spineRegion.sort(Comparator.comparingDouble(LineSegmentCluster::getLength));
        double clusterLengthMedian = spineRegion.get(spineRegion.size() / 2).getLength();
        Log.d(TAG, "clusterLengthMedian: " + clusterLengthMedian);
        for (LineSegmentCluster c : spineRegion) {
            Log.d(TAG, "Math.abs(c.getMaxY() - boundMax.y): " + Math.abs(c.getMaxY() - boundMax.y));
            if (Math.abs(c.getMaxY() - boundMax.y) < clusterLengthMedian) {
                result.add(c);
                if (DEBUGGING) Debug.drawLine(Debug.FILTER_5_CLUSTER_DISCARDED, c.getBot(), c.getTop(), Debug.YELLOW, 2);
            } else {
                //if (DEBUGGING) Debug.drawLine(Debug.TEST, c.getBot(), c.getTop(), Debug.YELLOW, 2);
            }
        }
        return result;
    }

    // 4.4.4.4
    private void mergeCluster(ArrayList<LineSegmentCluster> spineRegions) {
        boolean clusterMerged;
        do {
            clusterMerged = false;
            LineSegmentCluster c1 = null;
            LineSegmentCluster c2 = null;
            for (int i = 0; i < spineRegions.size(); i++) {
                for (int j = i+1; j < spineRegions.size(); j++) {
                    c1 = spineRegions.get(i);
                    c2 = spineRegions.get(j);
                    clusterMerged = c1.mergeWith(c2);
                    if (clusterMerged) break;
                }
                if (clusterMerged) break;
            }
            if (clusterMerged) spineRegions.remove(c2);
        } while (clusterMerged);
    }

    // Ende 4.4.4.4
    private void addVerticalBorderSegments(AlignmentRegion region, ArrayList<LineSegmentCluster> spineRegions) {
        if (spineRegions.isEmpty()) return;
        LineSegmentCluster mostLeft = spineRegions.get(0);
        LineSegment cornerLeft = new LineSegment(
                new Point(region.getBoundMin().x, mostLeft.getMaxY()),
                new Point(region.getBoundMin().x, mostLeft.getMinY()));
        LineSegmentCluster borderLeft = new LineSegmentCluster(cornerLeft, yTopBorder, yBotBorder);
        borderLeft.calculatePoints();
        spineRegions.add(0, borderLeft);

        LineSegmentCluster mostRight = spineRegions.get(spineRegions.size() - 1);
        LineSegment cornerRight = new LineSegment(
                new Point(region.getBoundMax().x, mostRight.getMaxY()),
                new Point(region.getBoundMax().x, mostRight.getMinY()));
        LineSegmentCluster borderRight = new LineSegmentCluster(cornerRight, yTopBorder, yBotBorder);
        borderRight.calculatePoints();
        spineRegions.add(borderRight);
    }

}