package com.mocircle.cidrawingsample;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mocircle.cidrawing.ConfigManager;
import com.mocircle.cidrawing.DrawingBoard;
import com.mocircle.cidrawing.DrawingBoardManager;
import com.mocircle.cidrawing.board.Layer;
import com.mocircle.cidrawing.board.LayerManager;
import com.mocircle.cidrawing.element.DrawElement;
import com.mocircle.cidrawing.element.PhotoElement;
import com.mocircle.cidrawing.element.TextElement;
import com.mocircle.cidrawing.element.shape.ArcElement;
import com.mocircle.cidrawing.element.shape.CircleElement;
import com.mocircle.cidrawing.element.shape.IsoscelesTriangleElement;
import com.mocircle.cidrawing.element.shape.LineElement;
import com.mocircle.cidrawing.element.shape.OvalElement;
import com.mocircle.cidrawing.element.shape.RectElement;
import com.mocircle.cidrawing.element.shape.RightTriangleElement;
import com.mocircle.cidrawing.element.shape.SquareElement;
import com.mocircle.cidrawing.mode.DrawingMode;
import com.mocircle.cidrawing.mode.InsertPhotoMode;
import com.mocircle.cidrawing.mode.InsertShapeMode;
import com.mocircle.cidrawing.mode.InsertTextMode;
import com.mocircle.cidrawing.mode.PointerMode;
import com.mocircle.cidrawing.mode.eraser.ObjectEraserMode;
import com.mocircle.cidrawing.mode.selection.LassoSelectionMode;
import com.mocircle.cidrawing.mode.selection.OvalSelectionMode;
import com.mocircle.cidrawing.mode.selection.RectSelectionMode;
import com.mocircle.cidrawing.mode.stroke.EraserStrokeMode;
import com.mocircle.cidrawing.mode.stroke.PlainStrokeMode;
import com.mocircle.cidrawing.mode.stroke.SmoothStrokeMode;
import com.mocircle.cidrawing.mode.transformation.MoveMode;
import com.mocircle.cidrawing.mode.transformation.ResizeMode;
import com.mocircle.cidrawing.mode.transformation.RotateMode;
import com.mocircle.cidrawing.mode.transformation.SkewMode;
import com.mocircle.cidrawing.operation.AlignmentOperation;
import com.mocircle.cidrawing.operation.ArrangeOperation;
import com.mocircle.cidrawing.operation.FlipOperation;
import com.mocircle.cidrawing.operation.GroupElementOperation;
import com.mocircle.cidrawing.operation.PathOperation;
import com.mocircle.cidrawing.operation.ReshapeOperation;
import com.mocircle.cidrawing.operation.UngroupElementOperation;
import com.mocircle.cidrawing.persistence.ExportData;
import com.mocircle.cidrawing.view.CiDrawingOnImageView;
import com.mocircle.cidrawing.view.CiDrawingView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private CiDrawingView drawingView;
    private DrawerLayout drawer;
    private RecyclerView layersView;

    private DrawingBoard drawingBoard;
    private LayerAdapter layerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupView();
        setupLayerView();

        drawingBoard = DrawingBoardManager.getInstance().createDrawingBoard();
        setupDrawingBoard();
        drawingBoard.getElementManager().createNewLayer();
        drawingBoard.getElementManager().selectFirstVisibleLayer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_more, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.switch_drawing_type_menu:
                switchDrawingType();
                return true;
            case R.id.switch_debug_menu:
                switchDebugMode();
                return true;
            case R.id.show_info_menu:
                showInfo();
                return true;
            case R.id.save_menu:
                saveDrawing();
                return true;
            case R.id.load_menu:
                loadDrawing();
                return true;
            case R.id.export_menu:
                exportPicture();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setScrimColor(Color.TRANSPARENT);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupDrawingBoard() {
        drawingView = findViewById(R.id.drawing_view);
        drawingBoard.setupDrawingView(drawingView);
        drawingBoard.getDrawingContext().getPaint().setColor(Color.BLACK);
        drawingBoard.getDrawingContext().getPaint().setStrokeWidth(6);
        drawingBoard.getDrawingContext().setDrawingMode(new PointerMode());

        layerAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                drawingBoard.getDrawingView().notifyViewUpdated();
            }
        });
        layerAdapter.setOnItemClick(new LayerAdapter.OnRecyclerViewItemClickListener() {
            @Override
            public void onItemClick(View view, Layer layer) {
                drawingBoard.getElementManager().selectLayer(layer);
                layerAdapter.notifyDataSetChanged();
            }
        });
        drawingBoard.getElementManager().addLayerChangeListener(new LayerManager.LayerChangeListener() {
            @Override
            public void onLayerChanged() {
                layerAdapter.setLayers(Arrays.asList(drawingBoard.getElementManager().getLayers()));
            }
        });
    }

    private void setupLayerView() {
        layersView = (RecyclerView) findViewById(R.id.layers_view);
        layersView.setLayoutManager(new LinearLayoutManager(this));
        layerAdapter = new LayerAdapter();
        layersView.setAdapter(layerAdapter);
    }

    // First row

    public void pointer(View v) {
        PointerMode mode = new PointerMode();
        drawingBoard.getDrawingContext().setDrawingMode(mode);
    }

    public void select(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.menu_select, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                PointerMode mode = new PointerMode();
                switch (item.getItemId()) {
                    case R.id.rect_select_menu:
                        mode.setSelectionMode(new RectSelectionMode());
                        break;
                    case R.id.oval_select_menu:
                        mode.setSelectionMode(new OvalSelectionMode());
                        break;
                    case R.id.lasso_menu:
                        mode.setSelectionMode(new LassoSelectionMode());
                        break;
                }
                drawingBoard.getDrawingContext().setDrawingMode(mode);
                return true;
            }
        });
        popup.show();
    }

    public void transform(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.menu_transform, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                DrawingMode mode = null;
                switch (item.getItemId()) {
                    case R.id.move_menu:
                        mode = new MoveMode(true);
                        break;
                    case R.id.rotate_menu:
                        mode = new RotateMode(true);
                        break;
                    case R.id.resize_menu:
                        mode = new ResizeMode(true);
                        break;
                    case R.id.skew_menu:
                        mode = new SkewMode(true);
                        break;
                }
                drawingBoard.getDrawingContext().setDrawingMode(mode);
                return true;
            }
        });
        popup.show();
    }

    public void eraser(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.menu_eraser, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                DrawingMode mode = null;
                switch (item.getItemId()) {
                    case R.id.object_eraser_menu:
                        mode = new ObjectEraserMode();
                        break;
                }
                drawingBoard.getDrawingContext().setDrawingMode(mode);
                return true;
            }
        });
        popup.show();
    }

    // Second row

    public void stroke(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.menu_stroke, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                DrawingMode mode = null;
                switch (item.getItemId()) {
                    case R.id.plain_stroke_menu:
                        mode = new PlainStrokeMode();
                        break;
                    case R.id.smooth_stroke_menu:
                        mode = new SmoothStrokeMode();
                        break;
                    case R.id.eraser_stroke_menu:
                        mode = new EraserStrokeMode();
                        break;
                }
                if (mode != null) {
                    drawingBoard.getDrawingContext().setDrawingMode(mode);
                }
                return true;
            }
        });
        popup.show();
    }

    public void insertShape(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.menu_insert_shape, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                InsertShapeMode mode = new InsertShapeMode();
                drawingBoard.getDrawingContext().setDrawingMode(mode);
                switch (item.getItemId()) {
                    case R.id.line_menu:
                        mode.setShapeType(LineElement.class);
                        break;
                    case R.id.arc_menu:
                        mode.setShapeType(ArcElement.class);
                        break;
                    case R.id.rect_menu:
                        mode.setShapeType(RectElement.class);
                        break;
                    case R.id.squre_menu:
                        mode.setShapeType(SquareElement.class);
                        break;
                    case R.id.oval_menu:
                        mode.setShapeType(OvalElement.class);
                        break;
                    case R.id.circle_menu:
                        mode.setShapeType(CircleElement.class);
                        break;
                    case R.id.isosceles_triangle_menu:
                        mode.setShapeType(IsoscelesTriangleElement.class);
                        break;
                    case R.id.right_triangle_menu:
                        RightTriangleElement shape = new RightTriangleElement();
                        shape.setLeftRightAngle(true);
                        mode.setShapeInstance(shape);
                        break;
                }
                return true;
            }
        });
        popup.show();
    }

    public void insertPhoto(View v) {
        InsertPhotoMode mode = new InsertPhotoMode();
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(getAssets().open("sample.jpg"));
            PhotoElement element = new PhotoElement();
            element.setBitmap(bitmap);
            element.setLockAspectRatio(true);
            mode.setPhotoElement(element);
        } catch (IOException e) {
            e.printStackTrace();
        }
        drawingBoard.getDrawingContext().setDrawingMode(mode);
    }

    public void insertText(View v) {
        InsertTextMode mode = new InsertTextMode();
        TextElement element = new TextElement();
        element.setText("Sample text");
        element.setTextSize(60);
        mode.setTextElement(element);
        drawingBoard.getDrawingContext().setDrawingMode(mode);
    }

    // Left drawer panel

    public void addLayer(View v) {
        drawingBoard.getElementManager().createNewLayer();
        layerAdapter.notifyDataSetChanged();
    }

    public void removeLayer(View v) {
        drawingBoard.getElementManager().removeLayer(drawingBoard.getElementManager().getCurrentLayer());
        drawingBoard.getElementManager().selectFirstVisibleLayer();
        layerAdapter.notifyDataSetChanged();
    }

    // Right drawer panel

    public void reshape(View v) {
        drawingBoard.getOperationManager().executeOperation(new ReshapeOperation());
    }

    public void group(View v) {
        drawingBoard.getOperationManager().executeOperation(new GroupElementOperation());
    }

    public void ungroup(View v) {
        drawingBoard.getOperationManager().executeOperation(new UngroupElementOperation());
    }

    public void pathUnion(View v) {
        PathOperation operation = new PathOperation();
        operation.setPathOp(Path.Op.UNION);
        drawingBoard.getOperationManager().executeOperation(operation);
    }

    public void pathIntersect(View v) {
        PathOperation operation = new PathOperation();
        operation.setPathOp(Path.Op.INTERSECT);
        drawingBoard.getOperationManager().executeOperation(operation);
    }

    public void pathDifferent(View v) {
        PathOperation operation = new PathOperation();
        operation.setPathOp(Path.Op.DIFFERENCE);
        drawingBoard.getOperationManager().executeOperation(operation);
    }

    public void pathXor(View v) {
        PathOperation operation = new PathOperation();
        operation.setPathOp(Path.Op.XOR);
        drawingBoard.getOperationManager().executeOperation(operation);
    }

    public void alignLeft(View v) {
        AlignmentOperation operation = new AlignmentOperation();
        operation.setAlignmentType(AlignmentOperation.AlignmentType.HorizontalLeft);
        drawingBoard.getOperationManager().executeOperation(operation);
    }

    public void alignCenter(View v) {
        AlignmentOperation operation = new AlignmentOperation();
        operation.setAlignmentType(AlignmentOperation.AlignmentType.HorizontalCenter);
        drawingBoard.getOperationManager().executeOperation(operation);
    }

    public void alignRight(View v) {
        AlignmentOperation operation = new AlignmentOperation();
        operation.setAlignmentType(AlignmentOperation.AlignmentType.HorizontalRight);
        drawingBoard.getOperationManager().executeOperation(operation);
    }

    public void alignTop(View v) {
        AlignmentOperation operation = new AlignmentOperation();
        operation.setAlignmentType(AlignmentOperation.AlignmentType.VerticalTop);
        drawingBoard.getOperationManager().executeOperation(operation);
    }

    public void alignMiddle(View v) {
        AlignmentOperation operation = new AlignmentOperation();
        operation.setAlignmentType(AlignmentOperation.AlignmentType.VerticalMiddle);
        drawingBoard.getOperationManager().executeOperation(operation);
    }

    public void alignBottom(View v) {
        AlignmentOperation operation = new AlignmentOperation();
        operation.setAlignmentType(AlignmentOperation.AlignmentType.VerticalBottom);
        drawingBoard.getOperationManager().executeOperation(operation);
    }

    public void flipVertical(View v) {
        FlipOperation operation = new FlipOperation();
        operation.setFlipType(FlipOperation.FlipType.Vertical);
        drawingBoard.getOperationManager().executeOperation(operation);
    }

    public void flipHorizontal(View v) {
        FlipOperation operation = new FlipOperation();
        operation.setFlipType(FlipOperation.FlipType.Horizontal);
        drawingBoard.getOperationManager().executeOperation(operation);
    }

    // Bottom row

    public void changeColor(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.menu_color, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.black_menu:
                        drawingBoard.getDrawingContext().getPaint().setColor(Color.BLACK);
                        break;
                    case R.id.blue_menu:
                        drawingBoard.getDrawingContext().getPaint().setColor(Color.BLUE);
                        break;
                    case R.id.red_menu:
                        drawingBoard.getDrawingContext().getPaint().setColor(Color.RED);
                        break;
                }
                return true;
            }
        });
        popup.show();
    }

    public void changeColor2(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.menu_color2, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nocolor_menu:
                        drawingBoard.getDrawingContext().getPaint().setSecondaryColor(null);
                        break;
                    case R.id.black_menu:
                        drawingBoard.getDrawingContext().getPaint().setSecondaryColor(Color.BLACK);
                        break;
                    case R.id.blue_menu:
                        drawingBoard.getDrawingContext().getPaint().setSecondaryColor(Color.BLUE);
                        break;
                    case R.id.red_menu:
                        drawingBoard.getDrawingContext().getPaint().setSecondaryColor(Color.RED);
                        break;
                }
                return true;
            }
        });
        popup.show();
    }

    public void changeWidth(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.menu_width, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.large_menu:
                        drawingBoard.getDrawingContext().getPaint().setStrokeWidth(10);
                        break;
                    case R.id.normal_menu:
                        drawingBoard.getDrawingContext().getPaint().setStrokeWidth(6);
                        break;
                    case R.id.small_menu:
                        drawingBoard.getDrawingContext().getPaint().setStrokeWidth(2);
                        break;
                }
                return true;
            }
        });
        popup.show();
    }

    public void changeStyle(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.menu_style, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.stroke_menu:
                        drawingBoard.getDrawingContext().getPaint().setStyle(Paint.Style.STROKE);
                        break;
                    case R.id.fill_menu:
                        drawingBoard.getDrawingContext().getPaint().setStyle(Paint.Style.FILL);
                        break;
                    case R.id.fill_stroke_menu:
                        drawingBoard.getDrawingContext().getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
                        break;
                }
                return true;
            }
        });
        popup.show();
    }

    public void undo(View v) {
        drawingBoard.getOperationManager().undo();
    }

    public void redo(View v) {
        drawingBoard.getOperationManager().redo();
    }

    public void arrange(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.menu_arrange, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.get_order_menu) {
                    DrawElement element = drawingBoard.getElementManager().getSelection().getSingleElement();
                    if (element != null) {
                        int index = drawingBoard.getElementManager().getCurrentLayer().getElementOrder(element);
                        Toast.makeText(MainActivity.this, "Element order = " + index, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "You should select one element", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }

                ArrangeOperation operation = new ArrangeOperation();
                switch (item.getItemId()) {
                    case R.id.bring_forward_menu:
                        operation.setArrangeType(ArrangeOperation.ArrangeType.BringForward);
                        break;
                    case R.id.bring_front_menu:
                        operation.setArrangeType(ArrangeOperation.ArrangeType.BringToFront);
                        break;
                    case R.id.send_backward_menu:
                        operation.setArrangeType(ArrangeOperation.ArrangeType.SendBackward);
                        break;
                    case R.id.send_back_menu:
                        operation.setArrangeType(ArrangeOperation.ArrangeType.SendToBack);
                        break;
                }
                drawingBoard.getOperationManager().executeOperation(operation);
                return true;
            }
        });
        popup.show();
    }

    // More menu

    public void switchDrawingType() {
        if (drawingBoard.getConfigManager().getDrawingType() == ConfigManager.DrawingType.Vector) {
            drawingBoard.getConfigManager().setDrawingType(ConfigManager.DrawingType.Painting);
            Toast.makeText(this, "Switch to Painting type.", Toast.LENGTH_SHORT).show();
        } else {
            drawingBoard.getConfigManager().setDrawingType(ConfigManager.DrawingType.Vector);
            Toast.makeText(this, "Switch to Vector type.", Toast.LENGTH_SHORT).show();
        }
    }

    public void switchDebugMode() {
        drawingBoard.getConfigManager().setDebugMode(!drawingBoard.getConfigManager().isDebugMode());
        drawingView.notifyViewUpdated();
        Toast.makeText(this, "Debug mode=" + drawingBoard.getConfigManager().isDebugMode() + ".", Toast.LENGTH_SHORT).show();
    }

    public void showInfo() {
        DrawElement element = drawingBoard.getElementManager().getSelection().getSingleElement();
        if (element != null) {
            String msg = "Type: " + element.getClass().getSimpleName();
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    public void saveDrawing() {
        ExportData data = drawingBoard.exportData();
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString("drawings", data.getMetaData().toString());
        editor.commit();

        Map<String, byte[]> resMap = data.getResources();
        for (String key : resMap.keySet()) {
            File file = new File(this.getExternalCacheDir(), key);
            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(resMap.get(key));
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.i(TAG, "Save drawing as: " + data.getMetaData().toString());
        Toast.makeText(this, "Drawing saved.", Toast.LENGTH_SHORT).show();
    }

    public void loadDrawing() {
        String drawings = PreferenceManager.getDefaultSharedPreferences(this).getString("drawings", "");
        Map<String, byte[]> resources = new HashMap<>();
        try {
            File[] files = this.getExternalCacheDir().listFiles();
            for (File file : files) {
                try {
                    RandomAccessFile fis = new RandomAccessFile(file, "r");
                    byte[] bs = new byte[(int) fis.length()];
                    fis.readFully(bs);
                    fis.close();

                    resources.put(file.getName(), bs);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            JSONObject obj = new JSONObject(drawings);
            drawingBoard = DrawingBoardManager.getInstance().createDrawingBoard(obj);
            setupDrawingBoard();
            drawingBoard.importData(obj, resources);
            drawingBoard.getElementManager().selectFirstVisibleLayer();
            Toast.makeText(this, "Drawing loaded.", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
            Toast.makeText(this, "Load drawing failed.", Toast.LENGTH_SHORT).show();
        }
    }

    public void exportPicture() {
        Bitmap bitmap = drawingView.saveAsBitmap();

        File file = new File(getFilesDir(), "image.jpg");

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        if (!file.exists()) {
            throw new IllegalStateException("Failed to save file");
        }

        long size = file.length();

        if (size <= 0) {
            throw new IllegalStateException("Failed to save file");
        }

        Bitmap bitmap2 = BitmapFactory.decodeFile(file.getPath()).copy( Bitmap.Config.ARGB_8888 , true);

        for (int i = 0; i < bitmap2.getWidth() - 1; i++) {
            bitmap2.setPixel(i, 0, Color.BLUE);
        }

        for (int i = 0; i < bitmap2.getWidth() - 1; i++) {
            bitmap2.setPixel(i, 3, Color.BLUE);
        }

        for (int i = 0; i < bitmap2.getWidth() - 1; i++) {
            bitmap2.setPixel(i, 5, Color.BLUE);
        }

        for (int i = 0; i < bitmap2.getWidth() - 1; i++) {
            bitmap2.setPixel(i, 7, Color.BLUE);
        }

        for (int i = 0; i < bitmap2.getWidth() - 1; i++) {
            bitmap2.setPixel(i, 10, Color.BLUE);
        }

        drawingView.setBackground(new BitmapDrawable(getResources(), bitmap2));
    }

}
