package com.owlvation.project.genedu.Tool.TextRecognition;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.owlvation.project.genedu.Network.NetworkChangeReceiver;
import com.owlvation.project.genedu.R;

import java.io.IOException;

public class TextRecognitionActivity extends AppCompatActivity {

    private ImageView capture, icBack;
    private TextView resultText;
    private LinearLayout detect, pick, copy;
    private Bitmap imageBitmap;
    private ClipboardManager clipboardManager;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PICK_IMAGE_REQUEST = 2;
    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_recognition);
        icBack = findViewById(R.id.ic_back);

        capture = findViewById(R.id.captureImage);
        resultText = findViewById(R.id.detectedText);
        detect = findViewById(R.id.detect);
        pick = findViewById(R.id.pick);
        copy = findViewById(R.id.copy);

        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        networkChangeReceiver = new NetworkChangeReceiver();

        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyTextToClipboard();
            }
        });

        pick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showImageDialog();
            }
        });

        detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detectText();
            }
        });

        icBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void copyTextToClipboard() {
        String textToCopy = resultText.getText().toString();
        if (!textToCopy.isEmpty()) {
            ClipData clip = ClipData.newPlainText("Detected Text", textToCopy);
            clipboardManager.setPrimaryClip(clip);
            Toast.makeText(this, R.string.successfully_copied_text_to_clipboard, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.failed_no_text_to_copy, Toast.LENGTH_SHORT).show();
        }
    }


    private void showImageDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_choose_image_source, null);

        LinearLayout capture = dialogView.findViewById(R.id.capture);
        LinearLayout gallery = dialogView.findViewById(R.id.gallery);
        Button cancelButton = dialogView.findViewById(R.id.btnCancel);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        dialog.show();

        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (checkPermission()) {
                    captureImage();
                } else {
                    requestPermission();
                }
            }
        });

        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                pickImageFromGallery();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Toast.makeText(getApplicationContext(), getString(R.string.action_canceled), Toast.LENGTH_SHORT).show();
            }
        });

        dialog.setCanceledOnTouchOutside(true);
    }

    private boolean checkPermission() {
        int camerapermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);
        return camerapermission == PackageManager.PERMISSION_GRANTED;
    }


    private void requestPermission() {
        int PERMISSION_CODE = 200;
        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.CAMERA
                },
                PERMISSION_CODE
        );
    }

    private void captureImage() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePicture, REQUEST_IMAGE_CAPTURE);
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0) {

            boolean cameraPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (cameraPermission) {
                Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_SHORT).show();
                captureImage();
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            capture.setImageBitmap(imageBitmap);
        } else if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                capture.setImageBitmap(imageBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.failed_to_load_image, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void detectText() {
        if (imageBitmap == null) {
            Toast.makeText(getApplicationContext(), getString(R.string.warning_detect_text), Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap processedBitmap = toGrayscale(imageBitmap);
        processedBitmap = sharpenImage(processedBitmap);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(processedBitmap, 1500, 1500, true);
        InputImage image = InputImage.fromBitmap(resizedBitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text text) {
                        if (text.getTextBlocks().isEmpty()) {
                            Toast.makeText(getApplicationContext(), getString(R.string.no_text_detected), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        StringBuilder resultBuilder = new StringBuilder();
                        for (Text.TextBlock block : text.getTextBlocks()) {
                            resultBuilder.append(block.getText()).append("\n");
                        }

                        resultText.setText(resultBuilder.toString());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), getString(R.string.failed_to_detect_text_from_image) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public static Bitmap sharpenImage(Bitmap original) {
        Matrix matrix = new Matrix();
        matrix.setScale(1.2f, 1.2f);
        Bitmap sharpenedBitmap = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
        return sharpenedBitmap;
    }

    public static Bitmap toGrayscale(Bitmap original) {
        Bitmap grayscaleBitmap = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
        for (int x = 0; x < original.getWidth(); x++) {
            for (int y = 0; y < original.getHeight(); y++) {
                int pixel = original.getPixel(x, y);
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;
                int gray = (int) (0.3 * red + 0.59 * green + 0.11 * blue);
                grayscaleBitmap.setPixel(x, y, Color.rgb(gray, gray, gray));
            }
        }
        return grayscaleBitmap;
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkChangeReceiver);
    }
}