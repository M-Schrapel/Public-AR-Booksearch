package masterarbeit_thilo.hci.luh.de.visualbooksearch;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database.BookEntity;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

    private static final String TAG = "BookAdapter";
    private static List<BookEntity> data;

    // Wie stark muss der Titel aus der Datenbank mit dem Wort aus der Spracheingabe übereinstimmen,
    // um zur Liste hinzugefügt zu werden [0: keine Übereinstimmung, 1: komplette Übereinstimmung]
    private static final double SPEECH_QUERY_SIMILARITY = 0.4;

    public static class BookViewHolder extends RecyclerView.ViewHolder {

        private TextView title, subtitle;

        public BookViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.title);
            subtitle = view.findViewById(R.id.subtitle);
            view.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, BookViewActivity.class);
                BookEntity book = data.get(getAdapterPosition());
                book.wrapIntent(intent);
                context.startActivity(intent);
            });
        }

        public void bindData(BookEntity bookEntity) {
            title.setText(bookEntity.title);
            if (bookEntity.author.isEmpty()) subtitle.setText(bookEntity.subtitle);
            else subtitle.setText(bookEntity.author);
        }
    }

    public void setData(List<BookEntity> newData, String speechQuery) {
        if (speechQuery != null) {
            List<BookEntity> filteredData = new ArrayList<>();
            NormalizedLevenshtein normLev = new NormalizedLevenshtein();
            for (BookEntity b : newData) {
                double similarity = normLev.similarity(speechQuery, b.title);
                //Log.d(TAG, "normLev: " + similarity + "(" + speechQuery + ", " + b.title + ")");
                if (similarity > SPEECH_QUERY_SIMILARITY) filteredData.add(b);
            }
            data = filteredData;
        } else {
            data = newData;
        }
        notifyDataSetChanged();
    }

    @Override
    public BookViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BookViewHolder holder, int position) {
        holder.bindData(data.get(position));
    }

    @Override
    public int getItemCount() {
        return (data == null) ? 0 : data.size();
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.book_view_item;
    }
}
