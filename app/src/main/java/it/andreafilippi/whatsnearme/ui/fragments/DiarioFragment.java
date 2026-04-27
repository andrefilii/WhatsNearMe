package it.andreafilippi.whatsnearme.ui.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import it.andreafilippi.whatsnearme.databinding.DialogDettaglioDiarioBinding;
import it.andreafilippi.whatsnearme.databinding.FragmentDiarioBinding;
import it.andreafilippi.whatsnearme.entities.MyPlace;
import it.andreafilippi.whatsnearme.utils.DatabaseHelper;
import it.andreafilippi.whatsnearme.utils.LuoghiAdapter;
import it.andreafilippi.whatsnearme.utils.Utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiarioFragment extends Fragment {
    private FragmentDiarioBinding binding;

    private DatabaseHelper databaseHelper;
    private LuoghiAdapter adapter;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());


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
        executorService.execute(() -> {
            Cursor cursor = databaseHelper.getAllLuoghi();
            mainHandler.post(() -> {
                if (isAdded() && adapter != null) {
                    adapter.changeCursor(cursor);
                } else if (cursor != null) {
                    cursor.close();
                }
            });
        });
    }

    private void loadLuoghi() {
        executorService.execute(() -> {
            Cursor cursor = databaseHelper.getAllLuoghi();
            mainHandler.post(() -> {
                if (isAdded()) {
                    adapter = new LuoghiAdapter(requireContext(), cursor);
                    adapter.setOnItemClickListener(this::showDetailsDialog);
                    binding.recyclerView.setAdapter(adapter);
                } else if (cursor != null) {
                    cursor.close();
                }
            });
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
        databaseHelper.close();
    }

    @SuppressLint("range")
    private void showDetailsDialog(int position) {
        executorService.execute(() -> {
            Cursor cursor = databaseHelper.getAllLuoghi();
            if (cursor.moveToPosition(position)) {
                String id = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID));
                String nome = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_NOME));
                Double lat = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LATITUDINE));
                Double lng = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LONGITUDINE));
                String imageUri = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_FOTO_PATH));

                mainHandler.post(() -> {
                    if (isAdded()) {
                        LayoutInflater inflater = LayoutInflater.from(requireContext());
                        DialogDettaglioDiarioBinding dialogBinding = DialogDettaglioDiarioBinding.inflate(inflater, null, false);
                        dialogBinding.imageView.setImageURI(Uri.parse("file://" + imageUri));

                        new AlertDialog.Builder(requireContext())
                                .setTitle(nome)
                                .setView(dialogBinding.getRoot())
                                .setPositiveButton("Apri su Maps", (dialog, which) -> {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Utils.getPlaceLocationUrl(new MyPlace(nome, lat, lng))));
                                    intent.setPackage(null);
                                    startActivity(intent);
                                })
                                .setNegativeButton("Rimuovi", (dialog, which) -> {
                                    confermaRimozione(id);
                                })
                                .show();
                    }
                });
            }
            cursor.close();
        });
    }

    private void confermaRimozione(String id) {
        new AlertDialog.Builder(getContext())
                .setTitle("Conferma Rimozione")
                .setMessage("Sei sicuro di voler rimuovere questo elemento?")
                .setPositiveButton("Sì", (dialog, which) -> {
                    executorService.execute(() -> {
                        databaseHelper.removeLuogo(id);
                        changeCursor();
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }
}