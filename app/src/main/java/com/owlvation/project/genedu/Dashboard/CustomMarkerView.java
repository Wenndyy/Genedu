package com.owlvation.project.genedu.Dashboard;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.owlvation.project.genedu.R;

import java.util.Locale;

public class CustomMarkerView  extends MarkerView {

    private TextView tvContent;

    public CustomMarkerView (Context context, int layoutResource) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvContent);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int minutes = (int) e.getY();
        tvContent.setText(String.format(Locale.getDefault(), "%d " + getContext().getString(R.string.minute), minutes));
        super.refreshContent(e, highlight);
    }

    private MPPointF mOffset;

    @Override
    public MPPointF getOffset() {
        if(mOffset == null) {
            mOffset = new MPPointF(-(getWidth() / 2), -getHeight() + 8);
        }
        return mOffset;
    }
}