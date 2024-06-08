package it.andreafilippi.whatsnearme.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;

import it.andreafilippi.whatsnearme.R;
import it.andreafilippi.whatsnearme.databinding.DialogMarkerBinding;
import it.andreafilippi.whatsnearme.entities.Place;

public class MarkerDialog extends DialogFragment {
    private DialogMarkerBinding binding;
    private Place place;

    public MarkerDialog(Place place) {
        this.place = place;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        binding = DialogMarkerBinding.inflate(inflater, container, false);

        binding.title.setText(place.getName());

        binding.indicazioniBtn.setOnClickListener(this::onIndicazioniBtnClick);
        binding.segnaVisitatoBtn.setOnClickListener(this::onSegnaVisitatoBtnClick);
        binding.condividiBtn.setOnClickListener(this::onCondividiBtnClick);

        if (place.getTags() != null) {
            //creazione chips
            ChipGroup chipGroup = binding.chipGroup;
            ViewGroup.LayoutParams layoutParams =
                    new ChipGroup.LayoutParams(ChipGroup.LayoutParams.WRAP_CONTENT, ChipGroup.LayoutParams.WRAP_CONTENT);
            for (String tag : place.getTags()) {
                Chip chip = new Chip(requireContext());
                chip.setLayoutParams(layoutParams);
                chip.setClickable(false);
                chip.setText(tag);
                chip.setChipDrawable(ChipDrawable.createFromAttributes(requireContext(), null, 0, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter));

                chipGroup.addView(chip);
            }
        }

        getDialog().setCanceledOnTouchOutside(true);

        return binding.getRoot();
    }

    private void onCondividiBtnClick(View view) {
    }

    private void onSegnaVisitatoBtnClick(View view) {
    }

    private void onIndicazioniBtnClick(View view) {
    }
}
