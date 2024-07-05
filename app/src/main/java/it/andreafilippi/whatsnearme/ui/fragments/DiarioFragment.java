package it.andreafilippi.whatsnearme.ui.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
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
import it.andreafilippi.whatsnearme.databinding.DialogDettaglioDiarioBinding;
import it.andreafilippi.whatsnearme.databinding.FragmentDiarioBinding;
import it.andreafilippi.whatsnearme.entities.Place;
import it.andreafilippi.whatsnearme.utils.DatabaseHelper;
import it.andreafilippi.whatsnearme.utils.LuoghiAdapter;
import it.andreafilippi.whatsnearme.utils.Utils;

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

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && adapter != null) {
            changeCursor();
        }
    }

    private void changeCursor() {
        new Thread(() -> {
            Cursor cursor = databaseHelper.getAllLuoghi();
            adapter.changeCursor(cursor);
        }).start();
    }

    private void loadLuoghi() {
        new Thread(() -> {
            Cursor cursor = databaseHelper.getAllLuoghi();

            adapter = new LuoghiAdapter(requireContext(), cursor);
            adapter.setOnItemClickListener(this::showDetailsDialog);
            requireActivity().runOnUiThread(() -> {
                binding.recyclerView.setAdapter(adapter);
            });
        }).start();
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
            String id = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID));
            String nome = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_NOME));
            Double lat = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LATITUDINE));
            Double lng = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LONGITUDINE));
            String imageUri = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_FOTO_PATH));

            LayoutInflater inflater = requireActivity().getLayoutInflater();
            DialogDettaglioDiarioBinding dialogBinding = DialogDettaglioDiarioBinding.inflate(inflater, null, false);

            dialogBinding.imageView.setImageURI(Uri.parse("file://" + imageUri));

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                    .setTitle(nome)
                    .setView(dialogBinding.getRoot())
                    .setPositiveButton("Apri su Maps", (dialog, which) -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Utils.getPlaceLocationUri(new Place(nome, lat, lng))));
                        intent.setPackage(null);
                        startActivity(intent);
                    })
                    .setNegativeButton("Rimuovi", (dialog, which) -> {
                        confermaRimozione(id);
                    });

            builder.create().show();
        }
        cursor.close();
    }

    private void confermaRimozione(String id) {
        new AlertDialog.Builder(getContext())
                .setTitle("Conferma Rimozione")
                .setMessage("Sei sicuro di voler rimuovere questo elemento?")
                .setPositiveButton("Sì", (dialog, which) -> {
                        databaseHelper.removeLuogo(id);
                        adapter.changeCursor(databaseHelper.getAllLuoghi());
                })
                .setNegativeButton("No", null)
                .show();
    }
}