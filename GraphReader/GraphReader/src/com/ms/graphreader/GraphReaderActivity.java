package com.ms.graphreader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
//import org.opencv.core.Rect;
import android.graphics.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap.Config;

@SuppressWarnings("unused")
public class GraphReaderActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG                 = "GraphReader::Activity";

    public static final int      VIEW_MODE_RGBA      = 0;
    public static final int      VIEW_MODE_GRAPH      = 1;
    public static final int      VIEW_MODE_PAUSE      = 2;
    public static final int      VIEW_MODE_RESUME      = 3;
    private Scalar               CONTOUR_COLOR;
	
    private Dialog dialog;


    


    private MenuItem             mItemReset;
    private MenuItem             mItemPreProcess;
    private MenuItem             mItemPostProcess;
    private MenuItem             mItemLoadFile;


    

    private CameraBridgeViewBase mOpenCvCameraView;



    private Mat                  mRgba;
    private Mat                  mGray;
    private Mat                  mHierarchy;

    private Mat                  mIntermediateMat;
    private int                  mTouchedX;
    private int                  mTouchedY;

    

    public static int           viewMode = VIEW_MODE_RGBA;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mGray = new Mat();
                    mRgba = new Mat();
                    mHierarchy = new Mat();
                    mIntermediateMat = new Mat();
                    CONTOUR_COLOR = new Scalar(255,0,0,255);
                    mOpenCvCameraView.enableView();

                    mOpenCvCameraView.setOnTouchListener(GraphReaderActivity.this);

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public GraphReaderActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	dialog =  new Dialog(this);


        setContentView(R.layout.activity_graph_reader);
        
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.activity_graph_reader);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

/*    public void DisMiss()
    {
    	dialog.dismiss();
    	dialog.hide();
    	
    }*/
    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        if (mRgba != null)
            mRgba.release();
        if (mHierarchy != null)
        	mHierarchy.release();
        if (mGray != null)
            mGray.release();
        if (mIntermediateMat != null)
            mIntermediateMat.release();

        mRgba = null;
        mGray = null;
        mIntermediateMat = null;
        mHierarchy = null;
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemReset  = menu.add("Reset");
        mItemPreProcess  = menu.add("Pre Process");  
        mItemPostProcess  = menu.add("Post Process");
        mItemLoadFile  = menu.add("Load File");      

        return true;
    }
    
    
    @SuppressWarnings("unused")
	private void cameraPause()
    {
    	
    	mOpenCvCameraView.disableView();
    	
    	if(mIntermediateMat!=null)
    	{
    		

           Bitmap mCacheBitmap = Bitmap.createBitmap(mIntermediateMat.width(),mIntermediateMat.height(), Bitmap.Config.ARGB_8888  );

           Utils.matToBitmap(mIntermediateMat, mCacheBitmap);
           
			Canvas canvas = mOpenCvCameraView.getHolder().lockCanvas();
           if ((canvas != null )&& (mCacheBitmap!=null)) {
               canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);

              canvas.drawBitmap(mCacheBitmap, new Rect(0,0,mCacheBitmap.getWidth(), mCacheBitmap.getHeight()), new Rect((canvas.getWidth() - mCacheBitmap.getWidth()) / 2,
                        (canvas.getHeight() - mCacheBitmap.getHeight()) / 2,
                        (canvas.getWidth() - mCacheBitmap.getWidth()) / 2 + mCacheBitmap.getWidth(),
                        (canvas.getHeight() - mCacheBitmap.getHeight()) / 2 + mCacheBitmap.getHeight()),  null);
              
              Log.i(TAG, "drawBitmap done");

               
               }

                mOpenCvCameraView.getHolder().unlockCanvasAndPost(canvas);
    		
    	}

    	
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemReset)
        {
            viewMode = VIEW_MODE_RGBA;
        	mOpenCvCameraView.enableView();

        }
        else if (item == mItemPreProcess)
        viewMode = VIEW_MODE_GRAPH;
        else if(item == mItemPostProcess)
        	cameraPause();
        else if(item == mItemLoadFile)
        	mOpenCvCameraView.enableView();
          


        return true;
    }

    public void onCameraViewStarted(int width, int height) {
/*        mGray = new Mat();
        mRgba = new Mat();
        mIntermediateMat = new Mat();*/
        
    }

   
    public void onCameraViewStopped() {
        // Explicitly deallocate Mats

/*        if (mRgba != null)
            mRgba.release();
        if (mGray != null)
            mGray.release();
        if (mIntermediateMat != null)
            mIntermediateMat.release();

        mRgba = null;
        mGray = null;
        mIntermediateMat = null;*/

    }
    
    
    
    @SuppressWarnings("deprecation")
	public boolean onTouch(View v, MotionEvent event) {
        int cols = mIntermediateMat.cols();
        int rows = mIntermediateMat.rows();
        

/*        int cols = mRgba.cols();
        int rows = mRgba.rows();*/
        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        mTouchedX = (int)event.getX() - xOffset;
        mTouchedY = (int)event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + mTouchedX + ", " + mTouchedY + ")");
        
        

	        double[] returned =  mIntermediateMat.get(mTouchedY, mTouchedX);
	        
	        int val = (int)(returned[0]);
	        
	        Log.i(TAG, "Value at this location (" + val + ")");
	       

	        if(val!=0)
	        {
        	dialog.setContentView(R.layout.popup);
        	TextView txt = (TextView)dialog.findViewById(R.id.textView2);
        	txt.setText( "("+  Integer.toString(mTouchedX) + ","+ Integer.toString(mTouchedY) +")");
        	dialog.show();
	        }
        
        
        	

        if ((mTouchedX < 0) || (mTouchedY < 0) || (mTouchedX > cols) || (mTouchedY > rows)) return false;
        
        

        return false; // don't need subsequent touch events
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        switch (GraphReaderActivity.viewMode) {
        case GraphReaderActivity.VIEW_MODE_RGBA:
        	
            break;
            
            
        case GraphReaderActivity.VIEW_MODE_GRAPH:
        	//EqualizeHist
            Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_RGB2GRAY, 0);
        	Imgproc.equalizeHist(mRgba, mRgba);
            

            Size s = new Size(7,7); // Try for 5x5
            Imgproc.GaussianBlur(mRgba, mRgba, s, 1.5, 1.5); //try with 0,0)

            Imgproc.Canny(mRgba, mRgba, 80, 90);
            
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

            Imgproc.findContours(mRgba, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            Log.e(TAG, "Contours count: " + contours.size());
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR); // how to use contours next chapter
            

            
            mRgba.copyTo(mIntermediateMat);
            

           break;

        }

        return mRgba;
    }
}
