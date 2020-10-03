package masterarbeit_thilo.hci.luh.de.visualbooksearch.debug;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Random;

import masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database.BookEntity;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines.LineSegment;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines.LineSegmentCluster;

public class Debug {

    private static final String TAG = "Debug";

    // Zum Ein- und Ausschalten vom DEBUG-Modus
    public static final boolean DEBUGGING = true;

    // alle Similarity-Werte abspeichern in .csv-Datei für rf_feature_importances.py
    public static final boolean STORE_FEATURE_VALUES = false;

    // um alle Bücher aus Regal "BOOKCASE" zu suchen
    public static final boolean SEARCH_ALL_BOOKS = false;
    public static final String BOOKCASE = "A"; // A für links, B für rechts

    // Name der Testdatei auf der eine Suche ausgeführt werden soll (muss sich im Ordner FileReaderWriter.DIR_NAME befinden)
    //public static final String TEST_FILE = "A";
    public static final String TEST_FILE = null; // keine Testdatei verwenden

    public static final Scalar RED = new Scalar(255, 0, 0);
    public static final Scalar GREEN = new Scalar(0, 255, 0);
    public static final Scalar BLUE = new Scalar(0, 0, 255);
    public static final Scalar YELLOW = new Scalar(255, 255, 0);
    public static final Scalar ORANGE = new Scalar(255, 165, 0);

    // OpenCV Matrizen zum Debuggen
    public static Mat ORIGINAL;
    public static Mat ALL_LINES;
    public static Mat FILTER_1_LINES_VERTICAL;
    public static Mat FILTER_1_LINES_HORIZONTAL;
    public static Mat FILTER_1_LINES_DISCARDED;
    public static Mat FILTER_2_COMPARTMENT_LINES;
    public static Mat FILTER_2_COMPARTMENTS;
    public static Mat FILTER_3_BOOK_REGION_LINES;
    public static Mat FILTER_3_BOOK_REGIONS;
    public static Mat FILTER_4_CLUSTER_ALL_LINES;
    public static Mat FILTER_4_CLUSTER_MERGED_LINES;
    public static Mat FILTER_5_CLUSTER_LENGTH;
    public static Mat FILTER_5_CLUSTER_DISCARDED;
    public static Mat FILTER_6_CLUSTER_MERGED;
    public static Mat FILTER_7_HORIZONTAL_BOUND;
    public static Mat FILTER_8_DISCARD_COLOR;
    public static Mat RESULT_SPINES;
    public static Mat TEST;

    // Linienstärke, falls nicht anders übergeben
    private static int THICKNESS = 1;

    private static Random random = new Random();

    // Bild der Kamera (src) als Ausgangsbild setzen, Debug-Bilder in grau, damit die Linien besser zu sehen sind
    public static void setMat(Mat src) {
        ORIGINAL = src;
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.cvtColor(gray, gray, Imgproc.COLOR_GRAY2RGB);

        ALL_LINES = gray.clone();
        FILTER_1_LINES_VERTICAL = gray.clone();
        FILTER_1_LINES_HORIZONTAL = gray.clone();
        FILTER_1_LINES_DISCARDED = gray.clone();
        FILTER_2_COMPARTMENT_LINES = gray.clone();
        FILTER_2_COMPARTMENTS = gray.clone();
        FILTER_3_BOOK_REGION_LINES = gray.clone();
        FILTER_3_BOOK_REGIONS = gray.clone();
        FILTER_4_CLUSTER_ALL_LINES = gray.clone();
        FILTER_4_CLUSTER_MERGED_LINES = gray.clone();
        FILTER_5_CLUSTER_LENGTH = gray.clone();
        FILTER_5_CLUSTER_DISCARDED = gray.clone();
        FILTER_6_CLUSTER_MERGED = gray.clone();
        FILTER_7_HORIZONTAL_BOUND = gray.clone();
        FILTER_8_DISCARD_COLOR = gray.clone();
        RESULT_SPINES = gray.clone();
        TEST = gray.clone();
    }

    public static void drawLine(Mat mat, LineSegment line, Scalar color) {
        drawLine(mat, line.getBot(), line.getTop(), color);
    }

    public static void drawLine(Mat mat, LineSegment line, Scalar color, int thickness) {
        drawLine(mat, line.getBot(), line.getTop(), color, thickness);
    }

    public static void drawLine(Mat mat, Point bot, Point top, Scalar color) {
        drawLine(mat, bot, top, color, THICKNESS);
    }

    public static void drawLine(Mat mat, Point bot, Point top, Scalar color, int thickness) {
        Imgproc.line(mat, bot, top, color, thickness);
    }

    // Für die Regionen mit liegenden Büchern
    public static void drawSwappedLine(Mat mat, Point bot, Point top, Scalar color, int thickness) {
        Imgproc.line(mat, new Point(bot.y, bot.x), new Point(top.y, top.x), color, thickness);
    }

    // Für die Regionen mit liegenden Büchern
    public static void drawSwappedLine(Mat mat, LineSegment line, Scalar color, int thickness) {
        drawSwappedLine(mat, line.getBot(), line.getTop(), color, thickness);
    }

    public static void drawLines(Mat mat, boolean horizontal, LineSegmentCluster c, int thickness) {
        for (LineSegment line : c.getLineSegments()) {
            if (horizontal) Imgproc.line(mat, new Point(line.getBot().y, line.getBot().x), new Point(line.getTop().y, line.getTop().x), YELLOW, thickness);
            else Imgproc.line(mat, line.getBot(), line.getTop(), YELLOW, thickness);
        }
    }

    public static void drawLines(Mat mat, ArrayList<LineSegment> lineSegments, Scalar color) {
        for (LineSegment line : lineSegments) {
            Imgproc.line(mat, line.getBot(), line.getTop(), color, THICKNESS);
        }
    }

    public static void drawCluster(Mat mat, boolean horizontal, int thickness, LineSegmentCluster... clusters) {
        int count = 0;
        for (LineSegmentCluster c : clusters) {
            count++;
            if (horizontal) {
                Imgproc.line(mat, new Point(c.getBot().y, c.getBot().x), new Point(c.getTop().y, c.getTop().x), YELLOW, thickness);
                //drawText(mat, new Point((c.getBot().y + c.getTop().y) / 2, (c.getBot().x + c.getTop().x) / 2), 0.8, String.valueOf(count), YELLOW);
            }
            else {
                Imgproc.line(mat, c.getBot(), c.getTop(), YELLOW, thickness);
                //drawText(mat, new Point((c.getBot().x + c.getTop().x) / 2, (c.getBot().y + c.getTop().y) / 2), 0.8, String.valueOf(count), YELLOW);
            }
        }
    }

    public static void drawCircle(Mat mat, Point center, Scalar color) {
        Imgproc.circle(mat, center, 5, color, -1);
    }

    public static void drawRect(Mat mat, Rect bound, Scalar color, int thickness) {
        Imgproc.rectangle(mat, bound, color, thickness);
    }

    public static void drawText(Mat mat, Point position, double scale, String text, Scalar color) {
        Imgproc.putText(mat, text, position, Imgproc.FONT_HERSHEY_DUPLEX, scale, color, 2);
    }

    public static Scalar getRandomColor() {
        return new Scalar(random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }

    public static void storeMat(Mat mat, String name) {
        FileReaderWriter.writeToFile(name, mat);
    }

    // Alle Debug-Ausgaben speichern
    public static void storeResults() {
        FileReaderWriter.writeToFile("_ORIGINAL", ORIGINAL);
        FileReaderWriter.writeToFile("ALL_LINES", ALL_LINES);
        FileReaderWriter.writeToFile("FILTER_1_LINES_VERTICAL", FILTER_1_LINES_VERTICAL);
        FileReaderWriter.writeToFile("FILTER_1_LINES_HORIZONTAL", FILTER_1_LINES_HORIZONTAL);
        FileReaderWriter.writeToFile("FILTER_1_LINES_DISCARDED", FILTER_1_LINES_DISCARDED);
        FileReaderWriter.writeToFile("FILTER_2_COMPARTMENT_LINES", FILTER_2_COMPARTMENT_LINES);
        FileReaderWriter.writeToFile("FILTER_2_COMPARTMENTS", FILTER_2_COMPARTMENTS);
        FileReaderWriter.writeToFile("FILTER_3_BOOK_REGION_LINES", FILTER_3_BOOK_REGION_LINES);
        FileReaderWriter.writeToFile("FILTER_3_BOOK_REGIONS", FILTER_3_BOOK_REGIONS);
        FileReaderWriter.writeToFile("FILTER_4_CLUSTER_ALL_LINES", FILTER_4_CLUSTER_ALL_LINES);
        FileReaderWriter.writeToFile("FILTER_4_CLUSTER_MERGED_LINES", FILTER_4_CLUSTER_MERGED_LINES);
        FileReaderWriter.writeToFile("FILTER_5_CLUSTER_LENGTH", FILTER_5_CLUSTER_LENGTH);
        FileReaderWriter.writeToFile("FILTER_5_CLUSTER_DISCARDED", FILTER_5_CLUSTER_DISCARDED);
        FileReaderWriter.writeToFile("FILTER_6_CLUSTER_MERGED", FILTER_6_CLUSTER_MERGED);
        FileReaderWriter.writeToFile("FILTER_7_HORIZONTAL_BOUND", FILTER_7_HORIZONTAL_BOUND);
        FileReaderWriter.writeToFile("FILTER_8_DISCARD_COLOR", FILTER_8_DISCARD_COLOR);
        FileReaderWriter.writeToFile("RESULT_SPINES", RESULT_SPINES);
        FileReaderWriter.writeToFile("TEST", TEST);
    }

    public static void drawLinesFeatureValues(Point[] boundPoints, BookEntity bookEntity) {
        Mat mat = RESULT_SPINES.clone();
        Debug.drawLine(mat, boundPoints[0], boundPoints[1], Debug.GREEN, 3);
        Debug.drawLine(mat, boundPoints[1], boundPoints[2], Debug.GREEN, 3);
        Debug.drawLine(mat, boundPoints[2], boundPoints[3], Debug.GREEN, 3);
        Debug.drawLine(mat, boundPoints[3], boundPoints[0], Debug.GREEN, 3);
        FileReaderWriter.writeToFile(bookEntity.title, mat);
    }
}