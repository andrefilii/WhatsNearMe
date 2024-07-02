package it.andreafilippi.whatsnearme.ui.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.Serializable;

import it.andreafilippi.whatsnearme.R;
import it.andreafilippi.whatsnearme.databinding.FragmentDiarioBinding;
import it.andreafilippi.whatsnearme.utils.DatabaseHelper;
import it.andreafilippi.whatsnearme.utils.LuoghiAdapter;

public class DiarioFragment extends Fragment {
    private FragmentDiarioBinding binding;

    private DatabaseHelper databaseHelper;
    private LuoghiAdapter adapter;


    public static DiarioFragment newInstance() {
        DiarioFragment df = new DiarioFragment();
        return df;
    }

    public DiarioFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        binding = FragmentDiarioBinding.inflate(inflater);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        databaseHelper = new DatabaseHelper(requireContext());

        loadLuoghi();

        return binding.getRoot();
    }

    private void loadLuoghi() {
        Cursor cursor = databaseHelper.getAllLuoghi();

        requireActivity().runOnUiThread(() -> {
            adapter = new LuoghiAdapter(requireContext(), cursor);
            binding.recyclerView.setAdapter(adapter);

            adapter.setOnItemClickListener(this::showDetailsDialog);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        databaseHelper.close();
    }

    @SuppressLint("range")
    private void showDetailsDialog(int position) {
        Cursor cursor = databaseHelper.getAllLuoghi();
        if (cursor.moveToPosition(position)) {
            String nome = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_NOME));

            // TODO creare dialog specifico che mostri la foto
            //  forse fattibile dall'alert dialog ma non credo
            new AlertDialog.Builder(requireContext())
                    .setTitle(nome)
                    .setPositiveButton("Apri su maps", null)
                    .create()
                    .show();
        }
        cursor.close();
    }
}