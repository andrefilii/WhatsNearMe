package it.andreafilippi.whatsnearme.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.Locale;

import it.andreafilippi.whatsnearme.R;
import it.andreafilippi.whatsnearme.databinding.ItemDiarioBinding;

public class LuoghiAdapter extends RecyclerView.Adapter<LuoghiAdapter.LuogoViewHolder> {

    private Context context;
    private Cursor cursor;
    private OnItemClickListener listener;
    private Drawable noImagePlaceholder;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public LuoghiAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;

        this.noImagePlaceholder = ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_no_image, context.getTheme());
    }

    @NonNull
    @Override
    public LuogoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = ItemDiarioBinding.inflate(LayoutInflater.from(context), parent, false).getRoot();
        return new LuogoViewHolder(view, listener);
    }

    @SuppressLint("range")
    @Override
    public void onBindViewHolder(@NonNull LuogoViewHolder holder, int position) {
        if (!cursor.moveToPosition(position)) {
            return;
        }

        String nome = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_NOME));
        Double lat = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LATITUDINE));
        Double lng = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LONGITUDINE));
        String imagePath = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_FOTO_PATH));

        holder.nomeLuogo.setText(nome);
        holder.coordinate.setText(String.format(Locale.US, "%f, %f", lat, lng));

        if (imagePath != null && !imagePath.isBlank()) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Uri imageUri = Uri.parse("file://" + imagePath);
                holder.luogoPhoto.setImageURI(imageUri);
            } else {
                // era associata una foto, ma è stat eliminata dal sistema
                holder.luogoPhoto.setImageDrawable(noImagePlaceholder);
            }
        } else {
            holder.luogoPhoto.setImageDrawable(noImagePlaceholder);
        }
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    public Cursor getCursor() {
        return cursor;
    }

    public void changeCursor(Cursor cursor) {
        Cursor old = swapCursor(cursor);
        if (old != null) {
            old.close();
        }
    }

    private Cursor swapCursor(Cursor newCursor) {
        if (newCursor == cursor) {
            return null;
        }
        Cursor oldCursor = cursor;
        cursor = newCursor;
        if (newCursor != null) {
            // notifico che deve essere rifatta la lista
            notifyDataSetChanged();
        } else {
            notifyItemRangeRemoved(0, getItemCount());
        }
        return oldCursor;
    }

    public static class LuogoViewHolder extends RecyclerView.ViewHolder {
        protected TextView nomeLuogo;
        protected TextView coordinate;
        protected ImageView luogoPhoto;

        public LuogoViewHolder(View itemView, final OnItemClickListener listener) {
            super(itemView);
            nomeLuogo = itemView.findViewById(R.id.nomeLuogo);
            coordinate = itemView.findViewById(R.id.coordinate);
            luogoPhoto = itemView.findViewById(R.id.luogo_photo);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }
}
