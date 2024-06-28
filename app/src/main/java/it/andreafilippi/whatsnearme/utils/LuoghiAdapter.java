package it.andreafilippi.whatsnearme.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import it.andreafilippi.whatsnearme.R;
import it.andreafilippi.whatsnearme.databinding.ItemDiarioBinding;

public class LuoghiAdapter extends RecyclerView.Adapter<LuoghiAdapter.LuogoViewHolder> {

    private Context context;
    private Cursor cursor;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public LuoghiAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;
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

        holder.nomeLuogo.setText(nome);
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    public static class LuogoViewHolder extends RecyclerView.ViewHolder {
        public TextView nomeLuogo;

        public LuogoViewHolder(View itemView, final OnItemClickListener listener) {
            super(itemView);
            nomeLuogo = itemView.findViewById(R.id.nomeLuogo);

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
