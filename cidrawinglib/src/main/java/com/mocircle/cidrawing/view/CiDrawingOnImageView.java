package com.mocircle.cidrawing.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mocircle.cidrawing.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class CiDrawingOnImageView extends FrameLayout implements DrawingView {
    CiDrawingView drawingView;
    Handler handler = new Handler(Looper.getMainLooper());

    public CiDrawingOnImageView(@NonNull Context context) {
        super(context);
        initialize(context);
    }

    public CiDrawingOnImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public CiDrawingOnImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    public CiDrawingOnImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    @Override
    public void setViewProxy(DrawingViewProxy viewProxy) {
        if (drawingView != null) {
            drawingView.setViewProxy(viewProxy);
        }
    }

    @Override
    public void notifyViewUpdated() {
        if (drawingView != null) {
            drawingView.notifyViewUpdated();
        }
    }

    private void initialize(Context context) {
        View.inflate(context, R.layout.drawing_view, this);
        drawingView = findViewById(R.id.ci_drawing_view);
    }

    public void loadBackgroundImage(String url) {
        handler.postDelayed(() -> {
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
        }, 100);
    }

    public Bitmap saveAsBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas((bitmap));
        draw(canvas);

        return bitmap;
    }
}
