package com.mocircle.cidrawingsample;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.mocircle.cidrawing.DrawingBoard;
import com.mocircle.cidrawing.DrawingBoardManager;
import com.mocircle.cidrawing.board.Layer;
import com.mocircle.cidrawing.board.LayerManager;
import com.mocircle.cidrawing.command.GroupElementCommand;
import com.mocircle.cidrawing.command.ReshapeCommand;
import com.mocircle.cidrawing.command.UngroupElementCommand;
import com.mocircle.cidrawing.element.shape.LineElement;
import com.mocircle.cidrawing.element.shape.RectElement;
import com.mocircle.cidrawing.mode.InsertShapeMode;
import com.mocircle.cidrawing.mode.PenMode;
import com.mocircle.cidrawing.mode.PointerMode;
import com.mocircle.cidrawing.mode.SkewMode;
import com.mocircle.cidrawing.view.CiDrawingView;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private CiDrawingView drawingView;
    private DrawerLayout drawer;
    private RecyclerView layersView;

    private DrawingBoard drawingBoard;
    private LayerAdapter layerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawingBoard = DrawingBoardManager.getInstance().createNewBoard();
        drawingBoard.getConfigManager().setDebugMode(true);
        setupView();
        setupLayer();
        setupDefault();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_more, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reshape_menu:
                reshape();
                return true;
            case R.id.switch_debug_menu:
                switchDebugMode();
                return true;
            case R.id.show_info_menu:
                showInfo();
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
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        drawingView = (CiDrawingView) findViewById(R.id.drawing_view);
        drawingBoard.setupDrawingView(drawingView);

        View selectButton = findViewById(R.id.select_button);
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                select();
            }
        });

        View skewButton = findViewById(R.id.skew_button);
        skewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skew();
            }
        });

        View groupButton = findViewById(R.id.group_button);
        groupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                group();
            }
        });

        View ungroupButton = findViewById(R.id.ungroup_button);
        ungroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ungroup();
            }
        });

        final View penButton = findViewById(R.id.pen_button);
        penButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pen(v);
            }
        });

        final View shapeButton = findViewById(R.id.shape_button);
        shapeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertShape(shapeButton);
            }
        });

        final View colorButton = findViewById(R.id.color_button);
        colorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeColor(colorButton);
            }
        });

        final View widthButton = findViewById(R.id.width_button);
        widthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeWidth(widthButton);
            }
        });

        View undoButton = findViewById(R.id.undo_button);
        undoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                undo();
            }
        });

        View redoButton = findViewById(R.id.redo_button);
        redoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redo();
            }
        });

    }

    private void setupDefault() {
        drawingBoard.getDrawingContext().setColor(Color.BLACK);
        drawingBoard.getDrawingContext().setStrokeWidth(6);
        select();
        drawingBoard.getElementManager().createNewLayer();
        drawingBoard.getElementManager().selectFirstVisibleLayer();
    }

    private void setupLayer() {
        layersView = (RecyclerView) findViewById(R.id.layers_view);
        layersView.setLayoutManager(new LinearLayoutManager(this));
        layerAdapter = new LayerAdapter();
        layersView.setAdapter(layerAdapter);

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


        View addButton = findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingBoard.getElementManager().createNewLayer();
                layerAdapter.notifyDataSetChanged();
            }
        });
        View removeButton = findViewById(R.id.remove_button);
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingBoard.getElementManager().removeLayer(drawingBoard.getElementManager().getCurrentLayer());
                drawingBoard.getElementManager().selectFirstVisibleLayer();
                layerAdapter.notifyDataSetChanged();
            }
        });
    }

    private void select() {
        drawingBoard.getDrawingContext().setDrawingMode(new PointerMode());
    }

    private void skew() {
        drawingBoard.getDrawingContext().setDrawingMode(new SkewMode());
    }

    private void group() {
        drawingBoard.getCommandManager().executeCommand(new GroupElementCommand());
    }

    private void ungroup() {
        drawingBoard.getCommandManager().executeCommand(new UngroupElementCommand());
    }

    private void pen(View v) {
        drawingBoard.getDrawingContext().setDrawingMode(new PenMode());
    }

    private void insertShape(View v) {
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
                    case R.id.rect_menu:
                        mode.setShapeType(RectElement.class);
                        break;
                }
                return true;
            }
        });
        popup.show();
    }

    private void insertPicture() {
    }

    private void insertText() {
    }

    private void changeColor(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.menu_color, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.black_menu:
                        drawingBoard.getDrawingContext().setColor(Color.BLACK);
                        break;
                    case R.id.blue_menu:
                        drawingBoard.getDrawingContext().setColor(Color.BLUE);
                        break;
                    case R.id.red_menu:
                        drawingBoard.getDrawingContext().setColor(Color.RED);
                        break;
                }
                return true;
            }
        });
        popup.show();
    }

    private void changeWidth(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.menu_width, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.large_menu:
                        drawingBoard.getDrawingContext().setStrokeWidth(10);
                        break;
                    case R.id.normal_menu:
                        drawingBoard.getDrawingContext().setStrokeWidth(6);
                        break;
                    case R.id.small_menu:
                        drawingBoard.getDrawingContext().setStrokeWidth(2);
                        break;
                }
                return true;
            }
        });
        popup.show();
    }

    private void undo() {
        drawingBoard.getCommandManager().undo();
    }

    private void redo() {
        drawingBoard.getCommandManager().redo();
    }

    private void reshape() {
        drawingBoard.getCommandManager().executeCommand(new ReshapeCommand());
        drawingView.notifyViewUpdated();
    }

    private void switchDebugMode() {
        drawingBoard.getConfigManager().setDebugMode(!drawingBoard.getConfigManager().isDebugMode());
        drawingView.notifyViewUpdated();
    }

    private void showInfo() {
    }

}