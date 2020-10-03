package masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database;

import android.content.Intent;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

@Entity(tableName = "book_entities")
public class BookEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;

    public String subtitle;

    public String author;

    public String publisher;

    public String genre;

    @TypeConverters({Converters.class})
    public float[] color;

    public BookEntity(String title, String subtitle, String author, String publisher, String genre, float[] color) {
        this.title = title;
        this.subtitle = subtitle;
        this.author = author;
        this.publisher = publisher;
        this.genre = genre;
        this.color = color;
    }

    @Ignore
    public static final String BOOK_ENTITY_TITLE = "masterarbeit_thilo.hci.luh.de.visualbooksearch.TITLE";
    @Ignore
    public static final String BOOK_ENTITY_SUBTITLE = "masterarbeit_thilo.hci.luh.de.visualbooksearch.SUBTITLE";
    @Ignore
    public static final String BOOK_ENTITY_AUTHOR = "masterarbeit_thilo.hci.luh.de.visualbooksearch.AUTHOR";
    @Ignore
    public static final String BOOK_ENTITY_PUBLISHER = "masterarbeit_thilo.hci.luh.de.visualbooksearch.PUBLISHER";
    @Ignore
    public static final String BOOK_ENTITY_GENRE = "masterarbeit_thilo.hci.luh.de.visualbooksearch.GENRE";
    @Ignore
    public static final String BOOK_ENTITY_COLOR = "masterarbeit_thilo.hci.luh.de.visualbooksearch.COLOR";

    @Ignore
    public BookEntity() {}

    @Ignore
    public void wrapIntent(Intent intent) {
        intent.putExtra(BOOK_ENTITY_TITLE, title);
        intent.putExtra(BOOK_ENTITY_SUBTITLE, subtitle);
        intent.putExtra(BOOK_ENTITY_AUTHOR, author);
        intent.putExtra(BOOK_ENTITY_PUBLISHER, publisher);
        intent.putExtra(BOOK_ENTITY_GENRE, genre);
        intent.putExtra(BOOK_ENTITY_COLOR, color);
    }

    @Ignore
    public static BookEntity getBookEntity(Intent intent) {
        BookEntity book = new BookEntity();
        book.title = intent.getStringExtra(BOOK_ENTITY_TITLE);
        book.subtitle = intent.getStringExtra(BOOK_ENTITY_SUBTITLE);
        book.author = intent.getStringExtra(BOOK_ENTITY_AUTHOR);
        book.publisher = intent.getStringExtra(BOOK_ENTITY_PUBLISHER);
        book.genre = intent.getStringExtra(BOOK_ENTITY_GENRE);
        book.color = intent.getFloatArrayExtra(BOOK_ENTITY_COLOR);
        return book;
    }
}
