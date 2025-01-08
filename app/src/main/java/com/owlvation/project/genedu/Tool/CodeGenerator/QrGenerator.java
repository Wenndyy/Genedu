package com.owlvation.project.genedu.Tool.CodeGenerator;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
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
import com.owlvation.project.genedu.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class QrGenerator extends AppCompatActivity {

    private EditText etCreateQrCode;
    private ImageView imageQrCode;
    private LinearLayout download, share, generate;
    private Bitmap qrcodeBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_generator);

        etCreateQrCode = findViewById(R.id.etCreateQrCode);
        imageQrCode = findViewById(R.id.imageQrCode);
        download = findViewById(R.id.downloadQr);
        share = findViewById(R.id.shareQr);
        generate = findViewById(R.id.generateQr);

        generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateQrCode();
            }
        });

        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveQrCodeToGallery();
            }
        });

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareQrCode();
            }
        });
    }

    private void generateQrCode() {
        String content = etCreateQrCode.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_text_for_the_barcode), Toast.LENGTH_SHORT).show();
            return;
        }

        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(content, BarcodeFormat.QR_CODE, 500, 500);
            int padding = dpToPx(20);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap qrcodeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    qrcodeBitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            qrcodeBitmap = addWatermark(qrcodeBitmap, height + padding);

            if (imageQrCode != null) {
                imageQrCode.setImageBitmap(qrcodeBitmap);
            } else {
                Toast.makeText(this, getString(R.string.error_imageview_not_found), Toast.LENGTH_SHORT).show();
            }

            this.qrcodeBitmap = qrcodeBitmap;

        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.error_generating_qr_code), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap addWatermark(Bitmap originalBitmap, int qrcodeBottomY) {
        Bitmap watermarkBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.app_logo);

        Bitmap newBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight() + dpToPx(40), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(newBitmap);
        canvas.drawColor(Color.WHITE);

        canvas.drawBitmap(originalBitmap, 0, 0, null);

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
        int startX = (newBitmap.getWidth() - totalWidth) / 2;
        int y = originalBitmap.getHeight() + dpToPx(5);

        canvas.drawBitmap(watermarkBitmap, startX, y, null);

        float textX = startX + logoSize + dpToPx(5);
        float textY = y + logoSize - dpToPx(5);

        canvas.drawText(appName, textX, textY, paint);

        return newBitmap;
    }


    private void saveQrCodeToGallery() {
        if (qrcodeBitmap != null) {
            MediaStore.Images.Media.insertImage(getContentResolver(), qrcodeBitmap, getString(R.string.app_name), null);
            Toast.makeText(QrGenerator.this, getString(R.string.successfully_saved_to_gallery), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(QrGenerator.this, getString(R.string.please_generate_the_qr_code_first), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareQrCode() {
        if (qrcodeBitmap != null) {
            try {
                File cachePath = new File(getCacheDir(), "images");
                cachePath.mkdirs();
                FileOutputStream stream = new FileOutputStream(new File(cachePath, "image.png"));
                qrcodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                stream.close();

                File imagePath = new File(getCacheDir(), "images");
                File newFile = new File(imagePath, "image.png");
                Uri contentUri = FileProvider.getUriForFile(this, "com.ndregt.project.genedu.fileprovider", newFile);

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
                Toast.makeText(QrGenerator.this, getString(R.string.failed_to_share_qr_code), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(QrGenerator.this, R.string.please_generate_the_qr_code_first, Toast.LENGTH_SHORT).show();
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private int spToPx(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }

}
