package it.andreafilippi.whatsnearme.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import it.andreafilippi.whatsnearme.databinding.ActivityCameraBinding;
import it.andreafilippi.whatsnearme.utils.Utils;

public class CameraActivity extends AppCompatActivity {
    public static final int RESULT_OK = AppCompatActivity.RESULT_OK;
    public static final int RESULT_CANCELED = AppCompatActivity.RESULT_CANCELED;
    public static final String EXTRA_IMAGE_PATH = "extra_image_path";

    private ActivityCameraBinding binding;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCameraBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permessi concessi, avvia la fotocamera
                        startCamera();
                    } else {
                        Utils.makeToastShort(this, "Permessi non concessi");
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });

        binding.captureBtn.setOnClickListener(this::onCaptureBtnClick);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CAMERAX", e.toString());
                setResult(RESULT_CANCELED);
                finish();
            }
        }, getMainExecutor());
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        // logica per foto
        imageCapture = new ImageCapture.Builder()
                .setTargetRotation(getDisplay().getRotation())
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    private void onCaptureBtnClick(View view) {
        File photoDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "WhatsNearMe");
        if (!photoDir.exists()) photoDir.mkdir();

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());

        File photoFile = new File(photoDir, timestamp + ".jpg");

        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(photoFile).build(),
                getMainExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        String photoPath = photoFile.getAbsolutePath();
                        Log.d("CAMERAX", photoPath);
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(EXTRA_IMAGE_PATH, photoPath);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("CAMERAX", "Errore scatto, " + exception);
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }
        );
    }
}