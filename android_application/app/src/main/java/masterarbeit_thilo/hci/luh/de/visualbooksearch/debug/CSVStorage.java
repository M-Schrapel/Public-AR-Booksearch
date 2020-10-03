package masterarbeit_thilo.hci.luh.de.visualbooksearch.debug;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database.BookEntity;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines.BookSpine;

// Debugklasse, um alle Similarity-Werte einer Suche in eine .csv-Datei zu schreiben
// für den RandomForest zur Bestimmung der Gewichte
public class CSVStorage {

    private static final String TAG = "CSVStorage";
    private static final String[] HEADER_RECORD = {"title", "subtitle", "author", "publisher", "color", "result"};

    public static void writeFeaturesToCSV(ArrayList<BookSpine> bookSpines, BookSpine bestSpine, BookEntity book) {
        try {
            String csvFile = FileReaderWriter.getFeatureImportancesFileName(book);
            boolean alreadyExists = new File(csvFile).exists();
            CSVWriter csvWriter = new CSVWriter(new FileWriter(csvFile, true));
            if (!alreadyExists) csvWriter.writeNext(HEADER_RECORD);
            for (BookSpine bookSpine : bookSpines) {
                String[] data = {Double.toString(bookSpine.getSimilarityTitle())
                        ,Double.toString(bookSpine.getSimilaritySubtitle())
                        ,Double.toString(bookSpine.getSimilarityAuthor())
                        ,Double.toString(bookSpine.getSimilarityPublisher())
                        ,Double.toString(bookSpine.getSimilarityColor())
                        ,Boolean.toString(bookSpine == bestSpine)};
                csvWriter.writeNext(data);
            }
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
