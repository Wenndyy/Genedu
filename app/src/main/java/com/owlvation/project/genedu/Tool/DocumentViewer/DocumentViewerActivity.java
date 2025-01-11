package com.owlvation.project.genedu.Tool.DocumentViewer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.owlvation.project.genedu.R;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFAbstractNum;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFNum;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentViewerActivity extends AppCompatActivity {

    TextView numberOfPage;
    ImageView result;
    LinearLayout pickFile, prevPage, nextPage;
    PdfRenderer pdfRenderer;
    XWPFDocument docxDocument;
    HWPFDocument docDocument;
    int totalPages = 0;
    int currentPage = 0;
    public static final int PICK_FILE = 99;
    private ImageView icBack;
    private String currentFileType = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_viewer);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        pickFile = findViewById(R.id.pickFile);
        prevPage = findViewById(R.id.previous);
        nextPage = findViewById(R.id.next);
        numberOfPage = findViewById(R.id.numberPage);
        result = findViewById(R.id.resultFile);
        icBack = findViewById(R.id.ic_back);
    }

    private void setupClickListeners() {
        pickFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] mimetypes = {
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            };
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
            startActivityForResult(intent, PICK_FILE);
        });

        prevPage.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                displayPage(currentPage);
            }
        });

        nextPage.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                displayPage(currentPage);
            }
        });

        icBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            String mimeType = getContentResolver().getType(uri);
            try {
                openDocument(uri, mimeType);
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.error_opening_file) + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openDocument(Uri uri, String mimeType) throws IOException {
        currentFileType = mimeType;

        switch (mimeType) {
            case "application/pdf":
                openPdfDocument(uri);
                break;
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                openWordDocument(uri);
                break;
        }

        currentPage = 0;
        displayPage(currentPage);
    }

    private void openPdfDocument(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
        pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        totalPages = pdfRenderer.getPageCount();
    }

    private void openWordDocument(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);

        if (currentFileType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            docxDocument = new XWPFDocument(inputStream);
        } else{
            Toast.makeText(this, R.string.invalid_file_format,Toast.LENGTH_LONG).show();
        }

        totalPages = calculateTotalPages();
        inputStream.close();
    }

    private int calculateTotalPages() {
        if (docxDocument != null) {
            return renderWordContentToBitmap(null, -1, true);
        } else {
            Toast.makeText(this, R.string.invalid_file_format,Toast.LENGTH_LONG).show();
        }
        return 0;
    }

    private void displayPage(int pageNumber) {
        Bitmap bitmap = Bitmap.createBitmap(1000, 1500, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        if (currentFileType.equals("application/pdf")) {
            try {
                PdfRenderer.Page page = pdfRenderer.openPage(pageNumber);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.error_rendering_pdf_page) + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else if (docxDocument != null) {
            renderWordContentToBitmap(canvas, pageNumber, false);
        }

        result.setImageBitmap(bitmap);
        numberOfPage.setText((pageNumber + 1) + "/" + totalPages);
    }

    private int renderWordContentToBitmap(Canvas canvas, int targetPage, boolean countOnly) {
        float currentHeight = 50;
        int pageHeight = 1500;
        int totalPages = 0;
        Map<BigInteger, Integer> numCounters = new HashMap<>();
        float marginInPixels = 72;
        float topMargin = marginInPixels;
        float leftMargin = marginInPixels;
        float rightMargin = marginInPixels;
        float bottomMargin = marginInPixels;
        try {
            for (IBodyElement element : docxDocument.getBodyElements()) {
                if (element instanceof XWPFParagraph) {
                    XWPFParagraph paragraph = (XWPFParagraph) element;
                    String text = paragraph.getText();

                    if (text.trim().isEmpty() && !paragraph.getRuns().isEmpty()) {
                        currentHeight += 20;
                        text = "\n";
                    }

                    Layout.Alignment canvasAlignment;
                    switch (paragraph.getAlignment()) {
                        case CENTER:
                            canvasAlignment = Layout.Alignment.ALIGN_CENTER;
                            break;
                        case RIGHT:
                            canvasAlignment = Layout.Alignment.ALIGN_OPPOSITE;
                            break;
                        default:
                            canvasAlignment = Layout.Alignment.ALIGN_NORMAL;
                            break;
                    }

                    if (!text.trim().isEmpty()) {
                        TextPaint textPaint = new TextPaint();
                        textPaint.setColor(Color.BLACK);
                        textPaint.setTypeface(Typeface.create("times new roman", Typeface.NORMAL));
                        textPaint.setTextSize(20);

                        float firstLineIndent = paragraph.getFirstLineIndent() > 0 ? paragraph.getFirstLineIndent() * 0.12f : 0;



                        if (paragraph.getNumIlvl() != null) {
                            BigInteger numId = paragraph.getNumID();
                            XWPFNum xwpfNum = docxDocument.getNumbering().getNum(numId);
                            XWPFAbstractNum abstractNum = docxDocument.getNumbering()
                                    .getAbstractNum(xwpfNum.getCTNum().getAbstractNumId().getVal());
                            CTLvl level = abstractNum.getAbstractNum().getLvlArray(paragraph.getNumIlvl().intValue());
                            String numFmt = level.getNumFmt() != null ? String.valueOf(level.getNumFmt().getVal()) : "";
                            String lvlText = level.getLvlText() != null ? level.getLvlText().getVal() : "";
                            int indentLevel = paragraph.getNumIlvl().intValue();

                            leftMargin += (indentLevel * 30);

                            int currentCounter = numCounters.getOrDefault(numId, 1);

                            if (numFmt.isEmpty() ||numFmt == null) {
                                if (lvlText.matches(".%[0-9]+.")) {
                                    if (lvlText.matches(".*[a-z].*")) {
                                        numFmt = "loweralpha";
                                    } else if (lvlText.matches(".*[A-Z].*")) {
                                        numFmt = "upperalpha";
                                    }
                                }else {
                                    numFmt = "decimal";
                                }



                            }

                            String numberPrefix = "";
                            String suffix = lvlText.contains(")") ? ") " : ". ";

                            switch (numFmt.toLowerCase()) {
                                case "decimal":
                                    numberPrefix = currentCounter + suffix;
                                    break;
                                case "loweralpha":
                                case "lowerletter":
                                    numberPrefix = (char) ('a' + (currentCounter - 1)) + suffix;
                                    break;
                                case "upperalpha":
                                case "upperletter":
                                    numberPrefix = (char) ('A' + (currentCounter - 1)) + suffix;
                                    break;
                                case "romanlower":
                                    numberPrefix = toRoman(currentCounter).toLowerCase() + suffix;
                                    break;
                                case "romanupper":
                                    numberPrefix = toRoman(currentCounter) + suffix;
                                    break;
                                case "bullet":
                                    numberPrefix = "\u2022 ";
                                    break;
                                default:
                                    if (lvlText.matches(".*[a-z].*")) {
                                        numberPrefix = (char) ('a' + (currentCounter - 1)) + suffix;
                                    } else if (lvlText.matches(".*[A-Z].*")) {
                                        numberPrefix = (char) ('A' + (currentCounter - 1)) + suffix;
                                    } else {
                                        numberPrefix = currentCounter + suffix;
                                    }
                                    break;
                            }


                            text = numberPrefix + text;
                            numCounters.put(numId, currentCounter + 1);
                        } else {

                            firstLineIndent = paragraph.getFirstLineIndent() > 0 ? paragraph.getFirstLineIndent() * 0.12f : 0;

                            if (paragraph.getIndentationLeft() > 0) {
                                leftMargin += paragraph.getIndentationLeft() * 0.12f;
                            }
                            if (paragraph.getIndentationRight() > 0) {
                                rightMargin += paragraph.getIndentationRight() * 0.12f;
                            }
                        }

                        for (XWPFRun run : paragraph.getRuns()) {
                            String color = run.getColor();
                            if (color != null) {
                                try {

                                    int textColor = Color.parseColor("#" + color);
                                    textPaint.setColor(textColor);
                                } catch (IllegalArgumentException e) {
                                    textPaint.setColor(Color.BLACK);
                                }
                            }

                            if (run.isBold()) {
                                textPaint.setTypeface(Typeface.create(textPaint.getTypeface(), Typeface.BOLD));
                            } else if (run.isItalic()) {
                                textPaint.setTypeface(Typeface.create(textPaint.getTypeface(), Typeface.ITALIC));
                            }


                            String fontFamily = run.getFontFamily();
                            if (fontFamily != null) {
                                textPaint.setTypeface(Typeface.create(fontFamily, Typeface.NORMAL));
                            }
                        }


                        int availableWidth = (int)(950 - leftMargin - rightMargin);

                        StaticLayout layout = StaticLayout.Builder.obtain( text.isEmpty() ? "\n" : text, 0, text.length(), textPaint, availableWidth)
                                .setAlignment(canvasAlignment)
                                .setLineSpacing(0f, 1.2f)
                                .setIncludePad(true)
                                .build();

                        if (currentHeight + layout.getHeight() > pageHeight) {
                            totalPages++;
                            currentHeight = topMargin;
                        }

                        if (!countOnly && totalPages == targetPage) {
                            canvas.save();
                            canvas.translate(leftMargin + firstLineIndent, currentHeight);
                            layout.draw(canvas);
                            canvas.restore();
                        }

                        currentHeight += layout.getHeight() + 25;
                    }

                    for (XWPFRun run : paragraph.getRuns()) {

                        for (XWPFPicture picture : run.getEmbeddedPictures()) {
                            try {
                                XWPFPictureData pictureData = picture.getPictureData();
                                byte[] bytes = pictureData.getData();
                                Bitmap imageBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                                if (imageBitmap != null) {
                                    float aspectRatio = (float) imageBitmap.getWidth() / imageBitmap.getHeight();
                                    int imageWidth = Math.min(500, imageBitmap.getWidth());
                                    int imageHeight = (int) (imageWidth / aspectRatio);

                                     leftMargin = marginInPixels;
                                     rightMargin = marginInPixels;
                                    if (paragraph.getIndentationLeft() > 0) {
                                        leftMargin += paragraph.getIndentationLeft() * 0.12f;
                                    }
                                    if (paragraph.getIndentationRight() > 0) {
                                        rightMargin += paragraph.getIndentationRight() * 0.12f;
                                    }

                                    float imageX;
                                    switch (canvasAlignment) {
                                        case ALIGN_CENTER:
                                            imageX = (950 - imageWidth) / 2;
                                            break;
                                        case ALIGN_OPPOSITE:
                                            imageX = 950 - imageWidth - rightMargin;
                                            break;
                                        default:
                                            imageX = leftMargin;
                                            break;
                                    }
                                    if (currentHeight + imageHeight > pageHeight) {
                                        totalPages++;
                                        currentHeight = topMargin;
                                    }

                                    if (!countOnly && totalPages == targetPage) {
                                        RectF imageRect = new RectF(
                                                imageX,
                                                currentHeight,
                                                imageX + imageWidth,
                                                currentHeight + imageHeight
                                        );
                                        canvas.drawBitmap(imageBitmap, null, imageRect, null);
                                    }

                                    currentHeight += imageHeight + 20;
                                    imageBitmap.recycle();
                                }
                            } catch (Exception e) {
                                Log.e("DEBUG", "Error processing paragraph image: " + e.getMessage());
                            }
                        }
                    }
                }
                else if (element instanceof XWPFTable) {
                    XWPFTable table = (XWPFTable) element;

                    Paint tablePaint = new Paint();
                    tablePaint.setStyle(Paint.Style.STROKE);
                    tablePaint.setStrokeWidth(1f);

                    TextPaint cellTextPaint = new TextPaint();
                    cellTextPaint.setColor(Color.BLACK);
                    cellTextPaint.setTextSize(20);
                    cellTextPaint.setTypeface(Typeface.create("times new roman", Typeface.NORMAL));

                    float tableWidth = 900;
                    float startX = 50;
                    float cellPadding = 8;


                    List<Float> columnWidths = new ArrayList<>();
                    int numColumns = table.getRow(0).getTableCells().size();
                    for (int i = 0; i < numColumns; i++) {
                        columnWidths.add(tableWidth / numColumns);
                    }

                    for (XWPFTableRow row : table.getRows()) {
                        float maxRowHeight = 0;
                        List<XWPFTableCell> cells = row.getTableCells();


                        for (int i = 0; i < cells.size(); i++) {
                            XWPFTableCell cell = cells.get(i);
                            String cellText = cell.getText().trim();
                            float cellWidth = columnWidths.get(i);

                            if (!cellText.isEmpty()) {
                                float textContentHeight = 0;
                                for (XWPFParagraph cellParagraph : cell.getParagraphs()) {
                                    String paragraphText = cellParagraph.getText();

                                    if (paragraphText.trim().isEmpty() || paragraphText.matches("^\\s*$")) {
                                        if (cellParagraph.getRuns().isEmpty()) {
                                            paragraphText = " ";
                                        } else {
                                            paragraphText = "\n";
                                        }
                                    }

                                    StaticLayout cellLayout = StaticLayout.Builder.obtain(paragraphText, 0, paragraphText.length(),
                                                    cellTextPaint, (int) (cellWidth - 2 * cellPadding))
                                            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                            .setLineSpacing(0f, 1.5f)
                                            .setIncludePad(true)
                                            .build();

                                    textContentHeight += cellLayout.getHeight();
                                }
                                maxRowHeight = Math.max(maxRowHeight, textContentHeight + 2 * cellPadding);
                            }

                            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                                for (XWPFRun run : paragraph.getRuns()) {
                                    for (XWPFPicture picture : run.getEmbeddedPictures()) {
                                        try {
                                            XWPFPictureData pictureData = picture.getPictureData();
                                            byte[] bytes = pictureData.getData();
                                            Bitmap imageBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                                            if (imageBitmap != null) {
                                                float aspectRatio = (float) imageBitmap.getWidth() / imageBitmap.getHeight();
                                                int maxImageWidth = Math.min(150, (int)(cellWidth / 3));
                                                int imageWidth = Math.min(maxImageWidth, imageBitmap.getWidth());
                                                int imageHeight = (int) (imageWidth / aspectRatio);

                                                int maxHeight = 200;
                                                if (imageHeight > maxHeight) {
                                                    imageHeight = maxHeight;
                                                    imageWidth = (int) (imageHeight * aspectRatio);
                                                }

                                                maxRowHeight = Math.max(maxRowHeight, imageHeight + 2 * cellPadding);
                                                imageBitmap.recycle();
                                            }
                                        } catch (Exception e) {
                                            Log.e("DEBUG", "Error calculating image height: " + e.getMessage());
                                        }
                                    }
                                }
                            }
                        }

                        maxRowHeight = Math.max(maxRowHeight, 40);

                        if (!countOnly && totalPages == targetPage) {
                            float currentX = startX;
                            for (int i = 0; i < cells.size(); i++) {
                                XWPFTableCell cell = cells.get(i);
                                float cellWidth = columnWidths.get(i);

                                canvas.drawRect(currentX, currentHeight,
                                        currentX + cellWidth, currentHeight + maxRowHeight,
                                        tablePaint);

                                String cellText = cell.getText().trim();
                                float contentY = currentHeight + cellPadding;

                                if (!cellText.isEmpty()) {
                                    float textY = contentY;
                                    for (XWPFParagraph cellParagraph : cell.getParagraphs()) {
                                        String paragraphText = cellParagraph.getText();



                                        if (paragraphText.trim().isEmpty() || paragraphText.matches("^\\s*$")) {
                                            if (cellParagraph.getRuns().isEmpty()) {
                                                paragraphText = " ";
                                            } else {
                                                paragraphText = "\n";
                                            }
                                        }

                                        Layout.Alignment alignment = Layout.Alignment.ALIGN_NORMAL;
                                        if (cellParagraph.getAlignment() == ParagraphAlignment.CENTER) {
                                            alignment = Layout.Alignment.ALIGN_CENTER;
                                        } else if (cellParagraph.getAlignment() == ParagraphAlignment.RIGHT) {
                                            alignment = Layout.Alignment.ALIGN_OPPOSITE;
                                        }

                                        StaticLayout cellLayout = StaticLayout.Builder.obtain(paragraphText, 0, paragraphText.length(),
                                                        cellTextPaint, (int) (cellWidth - 2 * cellPadding))
                                                .setAlignment(alignment)
                                                .setLineSpacing(0f, 1.5f)
                                                .setIncludePad(true)
                                                .build();

                                        canvas.save();
                                        canvas.translate(currentX + cellPadding, textY);
                                        cellLayout.draw(canvas);
                                        canvas.restore();

                                        textY += cellLayout.getHeight();
                                    }
                                    contentY = textY + cellPadding;
                                }

                                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                                    for (XWPFRun run : paragraph.getRuns()) {
                                        for (XWPFPicture picture : run.getEmbeddedPictures()) {
                                            try {
                                                XWPFPictureData pictureData = picture.getPictureData();
                                                byte[] bytes = pictureData.getData();
                                                Bitmap imageBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                                                if (imageBitmap != null) {
                                                    float aspectRatio = (float) imageBitmap.getWidth() / imageBitmap.getHeight();
                                                    int maxImageWidth = Math.min(150, (int)(cellWidth - 2 * cellPadding));
                                                    int imageWidth = Math.min(maxImageWidth, imageBitmap.getWidth());
                                                    int imageHeight = (int) (imageWidth / aspectRatio);


                                                    int maxHeight = 200;
                                                    if (imageHeight > maxHeight) {
                                                        imageHeight = maxHeight;
                                                        imageWidth = (int) (imageHeight * aspectRatio);
                                                    }

                                                    float imageX = currentX + cellPadding + (cellWidth - 2 * cellPadding - imageWidth) / 2;

                                                    RectF imageRect = new RectF(
                                                            imageX,
                                                            contentY,
                                                            imageX + imageWidth,
                                                            contentY + imageHeight
                                                    );

                                                    canvas.drawBitmap(imageBitmap, null, imageRect, null);
                                                    contentY += imageHeight + cellPadding;
                                                    imageBitmap.recycle();
                                                }
                                            } catch (Exception e) {
                                                Log.e("DEBUG", "Error drawing cell image: " + e.getMessage());
                                            }
                                        }
                                    }
                                }

                                currentX += cellWidth;
                            }
                        }

                        currentHeight += maxRowHeight;
                    }
                }

            }

            if (currentHeight > 50) {
                totalPages++;
            }

        } catch (Exception e) {
            Log.e("DEBUG", "Error in renderWordContentToBitmap: " + e.getMessage());
            e.printStackTrace();
        }

        return totalPages;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanupResources();
    }

    private void cleanupResources() {
        if (pdfRenderer != null) {
            pdfRenderer.close();
        }
        if (docxDocument != null) {
            try {
                docxDocument.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (docDocument != null) {
            try {
                docDocument.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private String toRoman(int number) {
        String[] romans = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < values.length; i++) {
            while (number >= values[i]) {
                result.append(romans[i]);
                number -= values[i];
            }
        }

        return result.toString();
    }
}