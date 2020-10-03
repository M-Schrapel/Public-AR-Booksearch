package masterarbeit_thilo.hci.luh.de.visualbooksearch;

import androidx.appcompat.app.AppCompatActivity;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database.BookEntity;

import android.content.Intent;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

// Detailansicht des ausgewählten Buches (4.2)
public class BookViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_view);
        Intent intent = getIntent();
        BookEntity book = BookEntity.getBookEntity(intent);

        TextView tv_title = findViewById(R.id.text_view_title);
        TextView tv_subtitle = findViewById(R.id.text_view_subtitle);
        TextView tv_publisher = findViewById(R.id.text_view_publisher);
        TextView tv_genre = findViewById(R.id.text_view_genre);
        TextView tv_author = findViewById(R.id.text_view_author);

        tv_title.setText(book.title);
        tv_subtitle.setText(book.subtitle);
        tv_publisher.setText(book.publisher);
        tv_genre.setText(book.genre);
        if (book.author.isEmpty()) {
            tv_author.setVisibility(View.INVISIBLE);
            findViewById(R.id.text_view_author_header).setVisibility(View.INVISIBLE);
        } else {
            tv_author.setText(book.author);
        }
        Button btnStartSearch = findViewById(R.id.btn_start_search);
        btnStartSearch.setOnClickListener(v -> {
            Intent intentAr = new Intent(this, CameraActivity.class);
            intentAr.replaceExtras(intent);
            startActivity(intentAr);
        });
    }

}
