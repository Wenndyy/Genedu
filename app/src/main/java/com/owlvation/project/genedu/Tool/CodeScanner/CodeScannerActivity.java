package com.owlvation.project.genedu.Tool.CodeScanner;

import static android.Manifest.permission_group.CAMERA;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
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
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.owlvation.project.genedu.R;

import java.io.IOException;
import java.util.List;

public class CodeScannerActivity extends AppCompatActivity {

    private ImageView capture, icBack;
    private TextView resultText;
    private LinearLayout detect, pick, copy;
    private Bitmap imageBitmap;
    private ClipboardManager clipboardManager;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PICK_IMAGE_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code_scanner);
        icBack = findViewById(R.id.ic_back);

        capture = findViewById(R.id.captureCode);
        resultText = findViewById(R.id.detectedTextResult);
        detect = findViewById(R.id.scan);
        pick = findViewById(R.id.pickCode);
        copy = findViewById(R.id.copyText);

        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

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
                detectBarcode();
            }
        });

        icBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
                finish();
            }
        });
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
        int camerapermission = ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA);
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

        if (takePicture.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePicture, REQUEST_IMAGE_CAPTURE);
        }
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

    private void detectBarcode() {
        if (imageBitmap == null) {
            Toast.makeText(getApplicationContext(), getString(R.string.warning_detect_text), Toast.LENGTH_SHORT).show();
            return;
        }

        InputImage image = InputImage.fromBitmap(imageBitmap, 0);
        BarcodeScanner scanner = BarcodeScanning.getClient();
        Task<List<Barcode>> result = scanner.process(image).addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
            @Override
            public void onSuccess(List<Barcode> barcodes) {
                StringBuilder result = new StringBuilder();
                for (Barcode barcode : barcodes) {
                    Rect bounds = barcode.getBoundingBox();
                    Point[] corners = barcode.getCornerPoints();

                    String rawValue = barcode.getRawValue();
                    result.append(rawValue).append("\n");

                    int valueType = barcode.getValueType();
                    switch (valueType) {
                        case Barcode.TYPE_WIFI:
                            String ssid = barcode.getWifi().getSsid();
                            String password = barcode.getWifi().getPassword();
                            int type = barcode.getWifi().getEncryptionType();
                            result.append("SSID: ").append(ssid).append("\n");
                            result.append("Password: ").append(password).append("\n");
                            result.append("Encryption Type: ").append(type).append("\n");
                            break;
                        case Barcode.TYPE_URL:
                            String title = barcode.getUrl().getTitle();
                            String url = barcode.getUrl().getUrl();
                            result.append("Title: ").append(title).append("\n");
                            result.append("URL: ").append(url).append("\n");
                            break;
                    }
                }
                resultText.setText(result.toString());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), getString(R.string.failed_to_detect_text_from_image) + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
