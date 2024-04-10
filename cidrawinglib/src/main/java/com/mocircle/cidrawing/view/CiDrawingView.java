package com.mocircle.cidrawing.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.AppCompatImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * A view to provide drawing features.
 */
public class CiDrawingView extends View implements DrawingView {

    protected DrawingViewProxy viewProxy;

    public CiDrawingView(Context context) {
        super(context);
        setupView();
    }

    public CiDrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupView();
    }

    public CiDrawingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupView();
    }

    @Override
    public void setViewProxy(DrawingViewProxy viewProxy) {
        this.viewProxy = viewProxy;
    }

    @Override
    public void notifyViewUpdated() {
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (viewProxy != null) {
            if (viewProxy.onTouchEvent(event)) {
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (viewProxy != null) {
            viewProxy.onDraw(canvas);
        }
    }

    protected void setupView() {
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public Bitmap saveAsBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas((bitmap));
        draw(canvas);

        return bitmap;
    }

}
