package masterarbeit_thilo.hci.luh.de.visualbooksearch.image_processing;

import android.util.Log;

import com.google.firebase.ml.vision.text.FirebaseVisionText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import info.debatty.java.stringsimilarity.WeightedLevenshtein;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database.BookEntity;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines.BookSpine;

public class FeatureMatching {

    private static final String TAG = "image_processing FeatureMatching";

    private static final double COLOR_SIMILARITY_THRESHOLD = 0.5;

    private static final double TOTAL_SIMILARITY_SCORE = 0;
    private static final double SIMILARITY_TITLE_FACTOR = 0.643608;
    private static final double SIMILARITY_SUBTITLE_FACTOR = 0.024739;
    private static final double SIMILARITY_AUTHOR_FACTOR = 0.223251;
    private static final double SIMILARITY_PUBLISHER_FACTOR = 0.029544;
    private static final double SIMILARITY_COLOR_FACTOR = 0.078858;

    private BookEntity book;
    private WeightedLevenshtein weightLev;

    public FeatureMatching(BookEntity book) {
        this.book = book;
        weightLev = new WeightedLevenshtein((c1, c2) -> {
            if (c1 == ',' && c2 == '/') {
                return 0;
            } else if (c1 == '/' && c2 == ',') {
                return 0;
            } else if (c1 == '0' && c2 == 'o') {
                return 0;
            } else if (c1 == 'o' && c2 == '0') {
                return 0;
            } else if (c1 == 'i' && c2 == '1') {
                return 0;
            } else if (c1 == '1' && c2 == 'i') {
                return 0;
            } else if (c1 == 'l' && c2 == '1') {
                return 0;
            } else if (c1 == '1' && c2 == 'l') {
                return 0;
            } else if (c1 == 'l' && c2 == 'i') {
                return 0;
            } else if (c1 == 'i' && c2 == 'l') {
                return 0;
            } else if (c1 == 'b' && c2 == '8') {
                return 0;
            } else if (c1 == '8' && c2 == 'b') {
                return 0;
            } else if (c1 == 'a' && c2 == 'ä') {
                return 0;
            } else if (c1 == 'ä' && c2 == 'a') {
                return 0;
            } else if (c1 == 'o' && c2 == 'ö') {
                return 0;
            } else if (c1 == 'ö' && c2 == 'o') {
                return 0;
            } else if (c1 == 'u' && c2 == 'ü') {
                return 0;
            } else if (c1 == 'ü' && c2 == 'u') {
                return 0;
            }
            return 1;
        });
    }

    public boolean checkColor(BookSpine bookSpine) {
        float[] colorEntity = book.color;
        float[] colorSpine = bookSpine.getColorFeatureVector();
        float colorDistance = 0;
        for (int i = 0; i < colorEntity.length; i++) {
            colorDistance += Math.pow(colorEntity[i] - colorSpine[i], 2);
        }
        double colorSimilarity = Math.max(1 - colorDistance, 0);
        bookSpine.setSimilarityColor(colorSimilarity);
        //Log.d(TAG, "Book " + bookSpine.getId() + ": " + Arrays.toString(colorSpine) + ", distance: " + colorDistance);
        return colorSimilarity >= COLOR_SIMILARITY_THRESHOLD;
    }

    public void checkText(BookSpine bookSpine, FirebaseVisionText text) {
        //if (!text.getText().isEmpty()) Log.d(TAG, "Book " + bookSpine.getId() + " text: " + text.getText());
        List<FirebaseVisionText.Element> elements = new ArrayList<>();
        for (FirebaseVisionText.TextBlock block : text.getTextBlocks()) {
            //Log.d(TAG, "Book " + bookSpine.getId() + ": " + block.getText());
            List<FirebaseVisionText.Line> lines = block.getLines();
            for (FirebaseVisionText.Line l : lines) {
                elements.addAll(l.getElements());
            }
        }
        for (int i = 0; i < elements.size(); i++) {
            String s = elements.get(i).getText();
            //Log.d(TAG, "Book " + bookSpine.getId() + " elements: " + s);
            checkSubString(bookSpine, s);
            for (int j = i+1; j < elements.size(); j++) {
                s += " " + elements.get(j).getText();
                checkSubString(bookSpine, s);
            }
        }
    }

    private void checkSubString(BookSpine bookSpine, String s) {
        if (!book.title.isEmpty()) {
            double similarity_title = 1 - (weightLev.distance(book.title.toLowerCase(), s.toLowerCase()) / book.title.length());
            bookSpine.setSimilarityTitle(similarity_title);
            //if (similarity_title > SIMILARITY_TITLE_MINIMUM) Log.i(TAG, "WL(" + book.title + ", " + s + "): " + similarity_title);
        }

        if (!book.subtitle.isEmpty()) {
            double similarity_subtitle = 1 - (weightLev.distance(book.subtitle.toLowerCase(), s.toLowerCase()) / book.subtitle.length());
            bookSpine.setSimilaritySubtitle(similarity_subtitle);
            //if (similarity_subtitle > SIMILARITY_SUBTITLE_MINIMUM) Log.i(TAG, "WL(" + book.subtitle + ", " + s + "): " + similarity_subtitle);
        }

        if (!book.author.isEmpty()) {
            double similarity_author = 1 - (weightLev.distance(book.author.toLowerCase(), s.toLowerCase()) / book.author.length());
            bookSpine.setSimilarityAuthor(similarity_author);
            //if (similarity_author > SIMILARITY_AUTHOR_MINIMUM) Log.i(TAG, "WL(" + book.author + ", " + s + "): " + similarity_author);
        }

        if (!book.publisher.isEmpty()) {
            double similarity_publisher = 1 - (weightLev.distance(book.publisher.toLowerCase(), s.toLowerCase()) / book.publisher.length());
            bookSpine.setSimilarityPublisher(similarity_publisher);
            //if (similarity_publisher > SIMILARITY_PUBLISHER_MINIMUM) Log.i(TAG, "WL(" + book.publisher + ", " + s + "): " + similarity_publisher);
        }
    }

    public BookSpine findBestMatch(ArrayList<BookSpine> book_spines) {
        BookSpine bestMatch = null;
        double bestSimilarity = 0;
        for (BookSpine b : book_spines) {
            double totalSimilarity = (b.getSimilarityTitle() * SIMILARITY_TITLE_FACTOR
                    + b.getSimilaritySubtitle() * SIMILARITY_SUBTITLE_FACTOR
                    + b.getSimilarityAuthor() * SIMILARITY_AUTHOR_FACTOR
                    + b.getSimilarityPublisher() * SIMILARITY_PUBLISHER_FACTOR
                    + b.getSimilarityColor() * SIMILARITY_COLOR_FACTOR);
                    /*
                    / (SIMILARITY_TITLE_FACTOR + SIMILARITY_SUBTITLE_FACTOR + SIMILARITY_AUTHOR_FACTOR
                    + SIMILARITY_PUBLISHER_FACTOR + SIMILARITY_COLOR_FACTOR); // ist jetzt immer eins
                    */
            //Log.d(TAG, "BookSpine " + b.getId() + ": " + totalSimilarity);
            if (totalSimilarity > bestSimilarity) {
                bestMatch = b;
                bestSimilarity = totalSimilarity;
            }
        }

        if (bestSimilarity > TOTAL_SIMILARITY_SCORE) {
            Log.d(TAG, "Best similarity: " + bestSimilarity + " (BookSpine " + bestMatch.getId() + ")");
            return bestMatch;
        } else {
            if (bestMatch != null) Log.d(TAG, "No BookSpine found. Best similarity: " + bestSimilarity + " (BookSpine " + bestMatch.getId() + ")");
            else Log.d(TAG, "No BookSpine found.");
            return null;
        }
    }
}
