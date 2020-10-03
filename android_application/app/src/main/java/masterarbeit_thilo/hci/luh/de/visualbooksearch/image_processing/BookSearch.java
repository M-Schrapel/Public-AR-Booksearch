package masterarbeit_thilo.hci.luh.de.visualbooksearch.image_processing;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.LineSegmentDetector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import masterarbeit_thilo.hci.luh.de.visualbooksearch.Region;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.CSVStorage;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.FileReaderWriter;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database.BookEntity;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines.BookSpine;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines.BookSpineSegmentation;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines.LineSegment;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_bookcase.CompartmentRegion;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_bookcase.BookcaseSegmentation;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_compartment.AlignmentRegion;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_compartment.CompartmentSegmentation;

import static masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug.BOOKCASE;
import static masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug.DEBUGGING;
import static masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug.SEARCH_ALL_BOOKS;
import static masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug.STORE_FEATURE_VALUES;
import static masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database.BookViewModel.INDEX_AUTHOR;
import static masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database.BookViewModel.INDEX_BOOKCASE;
import static masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database.BookViewModel.INDEX_COLOR;
import static masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database.BookViewModel.INDEX_GENRE;
import static masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database.BookViewModel.INDEX_PUBLISHER;
import static masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database.BookViewModel.INDEX_SUBTITLE;
import static masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database.BookViewModel.INDEX_TITLE;
import static org.opencv.imgproc.Imgproc.LSD_REFINE_NONE;

public class BookSearch {

    private static final String TAG = "image_processing BookSearch";

    // Alle Linien, die kleiner sind werden verworfen
    public static final double LENGTH_THRESHOLD = 25;

    // LSD auf allen drei Farbkanälen (RGB) anwenden?
    private static final boolean SPLIT_COLOR_CHANNELS = false;

    // Einteilung der Linien in vertikal und horizontal
    private static final double ANGLE_THRESHOLD = 45;
    private static final double ANGLE_MIN_THRESHOLD = Math.toRadians(90 - ANGLE_THRESHOLD);
    private static final double ANGLE_MAX_THRESHOLD = Math.toRadians(90 + ANGLE_THRESHOLD);

    private LineSegmentDetector lsd;
    private BookEntity book;
    private FeatureExtraction featureExtraction;
    private Context context;

    public BookSearch(BookEntity book, Context context) {
        lsd = Imgproc.createLineSegmentDetector(LSD_REFINE_NONE);
        this.book = book;
        this.context = context;
    }

    public BookSpine processImage(Mat src) {
        if (Debug.TEST_FILE != null) FileReaderWriter.loadFile(Debug.TEST_FILE, src); // Testbild laden
        long start = System.currentTimeMillis();
        if (DEBUGGING) Debug.setMat(src);
        BookSpine.resetId();
        LineSegment.setSize(src.size());
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGRA2BGR);
        Mat gray = new Mat(), lines = new Mat();
        ArrayList<LineSegment> horizontalLines = new ArrayList<>();
        ArrayList<LineSegment> verticalLines = new ArrayList<>();
        if (SPLIT_COLOR_CHANNELS) {
            List<Mat> splittedImg = new ArrayList<>();
            Core.split(src, splittedImg);
            computeLines(horizontalLines, verticalLines, lines, splittedImg.toArray(new Mat[0]));
            /*
            if (Debug.DEBUGGING) {
                Debug.storeMat(splittedImg.get(0), "0");
                Debug.storeMat(splittedImg.get(1), "1");
                Debug.storeMat(splittedImg.get(2), "2");
            }
            */
        } else {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY); // LSD benötigt ein Grauwertbild als Eingabe
            computeLines(horizontalLines, verticalLines, lines, gray);
        }
        double time = (System.currentTimeMillis() - start) / 1000.0;
        Log.d(TAG, "LSD: " + time);

        // Buchregal wird in Fächer (CompartmentRegion) unterteilt
        start = System.currentTimeMillis();
        BookcaseSegmentation bookcaseSegmentation = new BookcaseSegmentation(src.size());
        ArrayList<CompartmentRegion> compartments = bookcaseSegmentation.getCompartments(horizontalLines);
        addLinesToRegions(verticalLines, horizontalLines, compartments);
        time = (System.currentTimeMillis() - start) / 1000.0;
        Log.d(TAG, "Faecher: " + time);

        // Fächer (CompartmentRegion) werden in Buchregionen (AlignmentRegion) mit unterschiedlicher Ausrichtung (liegend oder stehend) unterteilt
        start = System.currentTimeMillis();
        CompartmentSegmentation compartmentSegmentation = new CompartmentSegmentation(src.size());
        ArrayList<AlignmentRegion> alignments = new ArrayList<>();
        for (CompartmentRegion compartment : compartments) {
            ArrayList<AlignmentRegion> compartmentAlignments = compartmentSegmentation.getAlignments(compartment);
            addLinesToRegions(compartment.getVerticalLines(), compartment.getHorizontalLines(), compartmentAlignments);
            alignments.addAll(compartmentAlignments);
        }
        time = (System.currentTimeMillis() - start) / 1000.0;
        Log.d(TAG, "Ausrichtungsregionen: " + time);

        // In jeder Buchregion (AlignmentRegion) werden die Buchrücken (BookSpine) segmentiert
        start = System.currentTimeMillis();
        BookSpineSegmentation bookSpineSegmentation = new BookSpineSegmentation(src);
        ArrayList<BookSpine> bookSpines = new ArrayList<>();
        for (AlignmentRegion region : alignments) {
            bookSpines.addAll(bookSpineSegmentation.segmentBookSpines(region));
        }
        time = (System.currentTimeMillis() - start) / 1000.0;
        Log.d(TAG, "Segmentierung: " + time);

        if (SEARCH_ALL_BOOKS) { // Um alle Bücher auf einmal zu suchen
            ArrayList<BookEntity> books = getBookEntitiesFromDB();
            for (BookEntity searchedBook : books) {
                featureExtraction = new FeatureExtraction(searchedBook);
                for (BookSpine spine : bookSpines) {
                    spine.resetValuesForCSV();
                }
                BookSpine bestSpine = featureExtraction.extractFeatures(bookSpines);
                if (STORE_FEATURE_VALUES) CSVStorage.writeFeaturesToCSV(bookSpines, bestSpine, searchedBook);
                Point[] boundPoints = bestSpine.getCornerPoints();
                Debug.drawLinesFeatureValues(boundPoints, searchedBook);
            }
            return null;
        } else { // um ein Buch zu suchen
            featureExtraction = new FeatureExtraction(book);
            BookSpine bestSpine = featureExtraction.extractFeatures(bookSpines);
            if (DEBUGGING && bestSpine != null) {
                Point[] boundPoints = bestSpine.getCornerPoints();
                Debug.drawLine(Debug.RESULT_SPINES, boundPoints[0], boundPoints[1], Debug.GREEN, 5);
                Debug.drawLine(Debug.RESULT_SPINES, boundPoints[1], boundPoints[2], Debug.GREEN, 5);
                Debug.drawLine(Debug.RESULT_SPINES, boundPoints[2], boundPoints[3], Debug.GREEN, 5);
                Debug.drawLine(Debug.RESULT_SPINES, boundPoints[3], boundPoints[0], Debug.GREEN, 5);
            }
            time = (System.currentTimeMillis() - start) / 1000.0;
            Log.d(TAG, "Found " + bookSpines.size() + " book spines in " + time + " seconds.");
            if (DEBUGGING) Debug.storeResults();

            return bestSpine;
        }
    }

    // Die Linien werden zu den Regionen hinzugefügt in denen sie sich befinden
    private static void addLinesToRegions(ArrayList<LineSegment> verticalLines,
                                   ArrayList<LineSegment> horizontalLines,
                                   ArrayList<? extends Region> regions) {
        for (LineSegment line : verticalLines) {
            for (Region region : regions) {
                if (region.add(line, true)) break;
            }
        }
        for (LineSegment line : horizontalLines) {
            for (Region region : regions) {
                if (region.add(line, false)) break;
            }
        }
    }

    // Extrahiert Linien mit dem LSD und teilt sie in vertikale und horizontale Linien ein
    private void computeLines(ArrayList<LineSegment> horizontalLines, ArrayList<LineSegment> verticalLines, Mat lines, Mat... colorChannels) {
        float[] lineValues = new float[4]; // lineValues = (x1, y1, x2, y2)
        for (Mat mat : colorChannels) {
            lsd.detect(mat, lines);
            for (int i = 0; i < lines.rows(); i++) {
                lines.get(i, 0, lineValues);
                LineSegment line;
                if (lineValues[1] > lineValues[3]) {
                    line = new LineSegment(new Point(lineValues[0], lineValues[1]), new Point(lineValues[2], lineValues[3]));
                } else {
                    line = new LineSegment(new Point(lineValues[2], lineValues[3]), new Point(lineValues[0], lineValues[1]));
                }
                //Log.i(TAG, "angle: " + angle);
                Scalar color = new Scalar(i % 256, (i*2) % 256, (i*3) % 256);
                if (line.getLength() > LENGTH_THRESHOLD) {
                    if (line.getAngle() > ANGLE_MIN_THRESHOLD && line.getAngle() < ANGLE_MAX_THRESHOLD) {
                        verticalLines.add(line);
                        if (DEBUGGING) {
                            Debug.drawLine(Debug.FILTER_1_LINES_VERTICAL, line, Debug.YELLOW, 2);
                            Debug.drawLine(Debug.ALL_LINES, line, Debug.YELLOW, 2);
                        }
                    } else {
                        horizontalLines.add(line);
                        if (DEBUGGING) {
                            Debug.drawLine(Debug.FILTER_1_LINES_HORIZONTAL, line, Debug.YELLOW, 2);
                            Debug.drawLine(Debug.ALL_LINES, line, Debug.YELLOW, 2);
                        }
                    }
                } else {
                    if (DEBUGGING) {
                        Debug.drawLine(Debug.FILTER_1_LINES_DISCARDED, line, Debug.YELLOW, 2);
                        Debug.drawLine(Debug.ALL_LINES, line, Debug.YELLOW, 2);
                    }
                }
            }
        }
    }

    // Gibt eine Liste mit allen Büchern aus dem Bücherregal "BOOKCASE" zurück
    private ArrayList<BookEntity> getBookEntitiesFromDB() {
        ArrayList<BookEntity> books = new ArrayList<>();
        Gson gson = new Gson();
        try {
            Reader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("book_spine_data.csv")));
            CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build();
            String[] nextRecord;
            while ((nextRecord = csvReader.readNext()) != null) {
                if (nextRecord[INDEX_BOOKCASE].equals(BOOKCASE)) {
                    books.add(new BookEntity(
                            nextRecord[INDEX_TITLE],
                            nextRecord[INDEX_SUBTITLE],
                            nextRecord[INDEX_AUTHOR],
                            nextRecord[INDEX_PUBLISHER],
                            nextRecord[INDEX_GENRE],
                            gson.fromJson(nextRecord[INDEX_COLOR], float[].class)));
                }
            }
            csvReader.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return books;
    }

}
