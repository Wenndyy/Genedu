package com.owlvation.project.genedu.Tool.CodeGenerator;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.owlvation.project.genedu.Network.NetworkChangeReceiver;
import com.owlvation.project.genedu.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class BarGenerator extends AppCompatActivity {

    private EditText etCreateBarCode;
    private ImageView imageBarCode;
    private LinearLayout download, share, generate;
    private Bitmap barcodeBitmap;
    private ImageView icBack;
    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bar_generator);

        etCreateBarCode = findViewById(R.id.etCreateBarCode);
        imageBarCode = findViewById(R.id.imageBarCode);
        download = findViewById(R.id.downloadBar);
        share = findViewById(R.id.shareBar);
        generate = findViewById(R.id.generateBar);
        networkChangeReceiver = new NetworkChangeReceiver();


        generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                barCodeButton(v);
            }
        });

        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveBarcodeToGallery();
            }
        });

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareBarcode();
            }
        });

        icBack = findViewById(R.id.ic_back);
        icBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
                finish();
            }
        });
    }

    public void barCodeButton(View view) {
        String content = etCreateBarCode.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_text_for_the_barcode), Toast.LENGTH_SHORT).show();
            return;
        }

        if (content.length() > 80) {
            Toast.makeText(this, getString(R.string.failed_content_length_exceeds_barcode_limit_80_characters), Toast.LENGTH_SHORT).show();
            return;
        }

        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            int padding = dpToPx(20);
            int width = imageBarCode.getWidth() - (2 * padding);
            int height = imageBarCode.getHeight() - (2 * padding);
            int extraHeight = dpToPx(20);

            BitMatrix bitMatrix = multiFormatWriter.encode(content, BarcodeFormat.CODE_128, width, height);

            Bitmap barcodeBitmap = Bitmap.createBitmap(width + (2 * padding), height + (2 * padding) + extraHeight, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(barcodeBitmap);
            canvas.drawColor(Color.WHITE);

            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    if (bitMatrix.get(i, j)) {
                        canvas.drawPoint(i + padding, j + padding, paint);
                    }
                }
            }

            barcodeBitmap = addWatermark(barcodeBitmap, height + padding);

            if (imageBarCode != null) {
                imageBarCode.setImageBitmap(barcodeBitmap);
            } else {
                Toast.makeText(this, R.string.error_imageview_not_found, Toast.LENGTH_SHORT).show();
            }

            this.barcodeBitmap = barcodeBitmap;

        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error_generating_barcode, Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap addWatermark(Bitmap originalBitmap, int barcodeBottomY) {
        Bitmap watermarkBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.app_logo);

        Canvas canvas = new Canvas(originalBitmap);
        int logoSize = dpToPx(24);
        watermarkBitmap = Bitmap.createScaledBitmap(watermarkBitmap, logoSize, logoSize, true);

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(spToPx(12));
        paint.setTypeface(Typeface.create("ubuntu-medium", Typeface.NORMAL));
        paint.setAntiAlias(true);

        String appName = getString(R.string.app_name);
        float textWidth = paint.measureText(appName);

        int totalWidth = logoSize + dpToPx(10) + (int) textWidth;
        int startX = (originalBitmap.getWidth() - totalWidth) / 2;
        int y = barcodeBottomY + dpToPx(5);

        canvas.drawBitmap(watermarkBitmap, startX, y, null);

        float textX = startX + logoSize + dpToPx(5);
        float textY = y + (logoSize + paint.getTextSize()) / 2;

        canvas.drawText(appName, textX, textY, paint);

        return originalBitmap;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private int spToPx(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }


    private void saveBarcodeToGallery() {
        if (barcodeBitmap != null) {
            MediaStore.Images.Media.insertImage(getContentResolver(), barcodeBitmap, getString(R.string.app_name), null);
            Toast.makeText(BarGenerator.this, getString(R.string.successfully_saved_to_gallery), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(BarGenerator.this, getString(R.string.please_generate_the_barcode_first), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareBarcode() {
        if (barcodeBitmap != null) {
            try {
                File cachePath = new File(getCacheDir(), "images");
                cachePath.mkdirs();
                FileOutputStream stream = new FileOutputStream(new File(cachePath, "image.png"));
                barcodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                stream.close();

                File imagePath = new File(getCacheDir(), "images");
                File newFile = new File(imagePath, "image.png");
                Uri contentUri = FileProvider.getUriForFile(this, "com.owlvation.project.genedu.fileprovider", newFile);

                if (contentUri != null) {
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.choose_an_app)));
                }

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(BarGenerator.this, R.string.failed_to_share_barcode, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(BarGenerator.this, R.string.please_generate_the_barcode_first, Toast.LENGTH_SHORT).show();
        }
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
