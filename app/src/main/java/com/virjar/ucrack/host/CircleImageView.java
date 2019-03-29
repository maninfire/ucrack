package com.virjar.ucrack.host;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Created by virjar on 2018/9/22.
 */

public class CircleImageView extends android.support.v7.widget.AppCompatImageView {
    Paint mPaint = new Paint();

    public CircleImageView(Context context) {
        super(context);
        setMaxHeight(120);
        setMinimumHeight(120);
        setMaxWidth(120);
        setMinimumHeight(120);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setAntiAlias(true);
        //canvas.drawColor(Color.BLUE);
        mPaint.setColor(Color.WHITE);
        canvas.drawCircle(60, 60, 60, mPaint);
        canvas.save();
        canvas.scale(0.7f, 0.7f, 60, 60);
        mPaint.setColor(Color.RED);
        canvas.drawCircle(60, 60, 60, mPaint);
        canvas.restore();
    }
}
