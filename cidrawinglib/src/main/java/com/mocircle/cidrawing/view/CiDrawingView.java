package com.mocircle.cidrawing.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.appcompat.widget.AppCompatImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * A view to provide drawing features.
 */
public class CiDrawingView extends AppCompatImageView implements DrawingView {

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

    public void loadBackgroundImage(String url) {
        post(() -> {
            int width = 0;
            int height = 0;

            // set the shortest width and leave the other dimension as 0 which
            // tells Picasso to make it dynamic to keep the image ratio
            if (getWidth() < getHeight()) {
                width = getWidth();
            } else {
               height = getHeight();
            }

            Picasso.get()
                    .load(url)
                    .resize(width, height)
                    .centerCrop()
                    .into(new Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                            int w = bitmap.getWidth();
                            int h = bitmap.getHeight();

                            // update the size of the view to be the same as the image
                            ViewGroup.LayoutParams params = getLayoutParams();
                            params.height = h;
                            params.width = w;

                            setLayoutParams(params);
                            requestLayout();

                            setBackground(new BitmapDrawable(getResources(), bitmap));
                        }

                        @Override
                        public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {

                        }
                    });
        });
    }

}
