package com.suleymanbilgin.cmeimageprocessing;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;

/**
 * Created by Laptop on 22.10.2015.
 *
 * @author Süleyman Bilgin
 * @since 1.0
 */
public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener, CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "OCV::MainActivity";

    private static final long DRAWER_CLOSE_DELAY_MS = 250;
    private static final String NAV_ITEM_ID = "navItemId";

    private final Handler mDrawerActionHandler = new Handler();
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private int mNavItemId;

    Toolbar toolbar;

    public static final int VIEW_MODE_RGBA = 0;
    public static final int VIEW_MODE_HIST = 1;
    public static final int VIEW_MODE_NEGATIVE = 2;
    public static final int VIEW_MODE_LOG = 8;
    public static final int VIEW_MODE_POWER_LAW = 9;

    private CameraBridgeViewBase mOpenCvCameraView;

    private Size mSize0;

    private Mat mIntermediateMat;
    private Mat mMat0;
    private MatOfInt mChannels[];
    private MatOfInt mHistSize;
    private int mHistSizeNum = 25;
    private MatOfFloat mRanges;
    private Scalar mColorsRGB[];
    private Scalar mColorsHue[];
    private Scalar mWhilte;
    private Point mP1;
    private Point mP2;
    private float mBuff[];
    int ch;

    public static int viewMode = VIEW_MODE_RGBA;

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        init();
        if (null == savedInstanceState) {
            mNavItemId = R.id.rgb;
        } else {
            mNavItemId = savedInstanceState.getInt(NAV_ITEM_ID);
        }
        drawerLayoutSetup();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mIntermediateMat = new Mat();
        mSize0 = new Size();
        mChannels = new MatOfInt[]{new MatOfInt(0), new MatOfInt(1), new MatOfInt(2)};
        mBuff = new float[mHistSizeNum];
        mHistSize = new MatOfInt(mHistSizeNum);
        mRanges = new MatOfFloat(0f, 256f);
        mMat0 = new Mat();
        mColorsRGB = new Scalar[]{new Scalar(200, 0, 0, 255), new Scalar(0, 200, 0, 255), new Scalar(0, 0, 200, 255)};
        mColorsHue = new Scalar[]{
                new Scalar(255, 0, 0, 255), new Scalar(255, 60, 0, 255), new Scalar(255, 120, 0, 255), new Scalar(255, 180, 0, 255), new Scalar(255, 240, 0, 255),
                new Scalar(215, 213, 0, 255), new Scalar(150, 255, 0, 255), new Scalar(85, 255, 0, 255), new Scalar(20, 255, 0, 255), new Scalar(0, 255, 30, 255),
                new Scalar(0, 255, 85, 255), new Scalar(0, 255, 150, 255), new Scalar(0, 255, 215, 255), new Scalar(0, 234, 255, 255), new Scalar(0, 170, 255, 255),
                new Scalar(0, 120, 255, 255), new Scalar(0, 60, 255, 255), new Scalar(0, 0, 255, 255), new Scalar(64, 0, 255, 255), new Scalar(120, 0, 255, 255),
                new Scalar(180, 0, 255, 255), new Scalar(255, 0, 255, 255), new Scalar(255, 0, 215, 255), new Scalar(255, 0, 85, 255), new Scalar(255, 0, 0, 255)
        };
        mWhilte = Scalar.all(255);
        mP1 = new Point();
        mP2 = new Point();

    }

    public void onCameraViewStopped() {
        // Explicitly deallocate Mats
        if (mIntermediateMat != null)
            mIntermediateMat.release();

        mIntermediateMat = null;
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        Size sizeRgba = rgba.size();

        Mat rgbaInnerWindow;

        int rows = (int) sizeRgba.height;
        int cols = (int) sizeRgba.width;

        int left = cols / 8;
        int top = rows / 8;

        int width = cols * 3 / 4;
        int height = rows * 3 / 4;

        switch (MainActivity.viewMode) {
            case MainActivity.VIEW_MODE_RGBA:
                break;

            case MainActivity.VIEW_MODE_NEGATIVE:
                rgbaInnerWindow = rgba.submat(rows / 2 - 9 * rows / 100, rows / 2 + 9 * rows / 100, cols / 2 - 9 * cols / 100, cols / 2 + 9 * cols / 100);

                rows = rgbaInnerWindow.rows();
                cols = rgbaInnerWindow.cols();
                ch = rgbaInnerWindow.channels();

                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        double[] data = rgbaInnerWindow.get(i, j); //Stores element in an array
                        for (int k = 0; k < ch; k++) //Runs for the available number of channels
                        {
                            data[k] = 255 - data[k]; //Pixel modification done here
                        }
                        rgbaInnerWindow.put(i, j, data); //Puts element back into matrix
                    }
                }

                rgbaInnerWindow.release();
                break;

            /**
             * Log Transformation
             */
            case MainActivity.VIEW_MODE_LOG:
                rgbaInnerWindow = rgba.submat(rows / 2 - 9 * rows / 100, rows / 2 + 9 * rows / 100, cols / 2 - 9 * cols / 100, cols / 2 + 9 * cols / 100);
                //Log.e("data", k + String.valueOf(data[k]));
                rows = rgbaInnerWindow.rows();
                cols = rgbaInnerWindow.cols();
                ch = rgbaInnerWindow.channels();

                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        double[] data = rgbaInnerWindow.get(i, j); //Stores element in an array
                        for (int k = 0; k < ch; k++) //Runs for the available number of channels
                        {
                            // need constant value
                            data[k] = 20 * Math.log(1 + data[k]); //Pixel modification done here
                        }
                        rgbaInnerWindow.put(i, j, data); //Puts element back into matrix
                    }
                }

                rgbaInnerWindow.release();
                break;

            case MainActivity.VIEW_MODE_POWER_LAW:
                rgbaInnerWindow = rgba.submat(rows / 2 - 9 * rows / 100, rows / 2 + 9 * rows / 100, cols / 2 - 9 * cols / 100, cols / 2 + 9 * cols / 100);
                //Log.e("data", k + String.valueOf(data[k]));
                rows = rgbaInnerWindow.rows();
                cols = rgbaInnerWindow.cols();
                ch = rgbaInnerWindow.channels();

                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        double[] data = rgbaInnerWindow.get(i, j); //Stores element in an array
                        for (int k = 0; k < ch; k++) //Runs for the available number of channels
                        {
                            // need constant value
                            data[k] = 1 * Math.pow(data[k], 5); //Pixel modification done here
                        }
                        rgbaInnerWindow.put(i, j, data); //Puts element back into matrix
                    }
                }

                rgbaInnerWindow.release();
                break;
        }

        return rgba;
    }


    void init() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        viewMode = VIEW_MODE_RGBA;
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.rgb));
    }

    public void drawerLayoutSetup() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation);
        navigationView.setNavigationItemSelectedListener(this);

        navigationView.getMenu().findItem(mNavItemId).setChecked(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.open,
                R.string.close);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        navigate(mNavItemId);
    }

    private void navigate(final int itemId) {
        Fragment f = null;
        switch (itemId) {
            case R.id.rgb:
                viewMode = VIEW_MODE_RGBA;
                getSupportActionBar().setTitle(getString(R.string.rgb));
                break;
            case R.id.negative:
                viewMode = VIEW_MODE_NEGATIVE;
                getSupportActionBar().setTitle(getString(R.string.negative));
                break;
            case R.id.log_transform:
                viewMode = VIEW_MODE_LOG;
                getSupportActionBar().setTitle(getString(R.string.log_transform));
                break;
            case R.id.power_law_tranform:
                viewMode = VIEW_MODE_POWER_LAW;
                getSupportActionBar().setTitle(getString(R.string.power_law_transform));
                break;
            default:
        }
    }

    @Override
    public boolean onNavigationItemSelected(final MenuItem menuItem) {
        // update highlighted item in the navigation menu
        menuItem.setChecked(true);
        mNavItemId = menuItem.getItemId();

        // allow some time after closing the drawer before performing real navigation
        // so the user can see what is happening
        mDrawerLayout.closeDrawer(GravityCompat.START);
        mDrawerActionHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                navigate(menuItem.getItemId());
            }
        }, DRAWER_CLOSE_DELAY_MS);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_info) {
            Intent info = new Intent(MainActivity.this, InfoActivity.class);
            startActivity(info);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
}
