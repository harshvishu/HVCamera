package com.brotherpowers.hvcamera;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;

/**
 * Created by harsh_v on 12/8/16.
 */

public class HVSwitch extends SwitchCompat {
    public HVSwitch(Context context) {
        super(context);
    }

    public HVSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HVSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
}
