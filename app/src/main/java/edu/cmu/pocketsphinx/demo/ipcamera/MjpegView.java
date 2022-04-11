package edu.cmu.pocketsphinx.demo.ipcamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import edu.cmu.pocketsphinx.demo.objectDetection.DetectorActivity;
import edu.cmu.pocketsphinx.demo.objectDetection.tflite.Classifier;
import edu.cmu.pocketsphinx.demo.objectDetection.tracking.MultiBoxTracker;

import java.io.IOException;
import java.util.List;

public class MjpegView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "MjpegView";
    public final static int POSITION_UPPER_LEFT = 9;
    public final static int POSITION_UPPER_RIGHT = 3;
    public final static int POSITION_LOWER_LEFT = 12;
    public final static int POSITION_LOWER_RIGHT = 6;

    public final static int SIZE_STANDARD = 1;
    public final static int SIZE_BEST_FIT = 4;
    public final static int SIZE_FULLSCREEN = 8;
    Float mposX=0.0f;
    Float mposY=0.0f;
    String mtext="";
    Float mposXW=0.0f;
    Float mposYW=0.0f;
    List<Classifier.Recognition> RecResults;
    DetectorActivity detectorActivity = new DetectorActivity();
    MultiBoxTracker tracker;
    private Context context;
    Paint mbgPaint;

    private MjpegViewThread thread;
    private MjpegInputStream mIn = null;
    private boolean showFps = false;
    private boolean mRun = false;
    private boolean surfaceDone = false;
    private Paint overlayPaint;
    private int overlayTextColor;
    private int overlayBackgroundColor;
    private int ovlPos;
    private int dispWidth;
    private int dispHeight;
    private int displayMode;
    static Bitmap everyBitmap = null;

    public class MjpegViewThread extends Thread {
        private SurfaceHolder mSurfaceHolder;
        private int frameCounter = 0;
        private long start;
        private Bitmap ovl;

        public MjpegViewThread(SurfaceHolder surfaceHolder, Context context) {
            mSurfaceHolder = surfaceHolder;
        }

        private Rect destRect(int bmw, int bmh) {
            int tempx;
            int tempy;
            if (displayMode == MjpegView.SIZE_STANDARD) {
                tempx = (dispWidth / 2) - (bmw / 2);
                tempy = (dispHeight / 2) - (bmh / 2);
                return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
            }
            if (displayMode == MjpegView.SIZE_BEST_FIT) {
                float bmasp = (float) bmw / (float) bmh;
                bmw = dispWidth;
                bmh = (int) (dispWidth / bmasp);
                if (bmh > dispHeight) {
                    bmh = dispHeight;
                    bmw = (int) (dispHeight * bmasp);
                }
                tempx = (dispWidth / 2) - (bmw / 2);
                tempy = (dispHeight / 2) - (bmh / 2);
                return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
            }
            if (displayMode == MjpegView.SIZE_FULLSCREEN) {
                return new Rect(0, 0, dispWidth, dispHeight);
            }
            return null;
        }

        public void setSurfaceSize(int width, int height) {
            synchronized (mSurfaceHolder) {
                dispWidth = width;
                dispHeight = height;
            }
        }



        private Bitmap makeFpsOverlay(Paint p, String text) {
            Rect b = new Rect();
            p.getTextBounds(text, 0, text.length(), b);
            int bwidth = b.width() + 2;
            int bheight = b.height() + 2;
            Bitmap bm = Bitmap.createBitmap(bwidth, bheight, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            p.setColor(overlayBackgroundColor);
            c.drawRect(0, 0, bwidth, bheight, p);
            p.setColor(overlayTextColor);
            c.drawText(text, -b.left + 1, (bheight / 2) - ((p.ascent() + p.descent()) / 2) + 1, p);
            return bm;
        }

        private final int[] COLORS = {
                Color.BLUE,
                Color.RED,
                Color.GREEN,
                Color.YELLOW,
                Color.CYAN,
                Color.MAGENTA,
                Color.WHITE,
                Color.parseColor("#55FF55"),
                Color.parseColor("#FFA500"),
                Color.parseColor("#FF8888"),
                Color.parseColor("#AAAAFF"),
                Color.parseColor("#FFFFAA"),
                Color.parseColor("#55AAAA"),
                Color.parseColor("#AA33AA"),
                Color.parseColor("#0D0068")
        };

        private void drawRect(int mWidth, int mHeight, Paint mPaint) {
            RecResults = DetectorActivity.getResults();
            for (final Classifier.Recognition result : RecResults) {
                if (result.getLocation() == null) {
                    continue;
                }



/*              result.getConfidence();
                Log.v("getCOnfidece", "getConf: " + result.getConfidence());
                result.getLocation();
                Log.v("getCOnfidece", "getLoc: " + result.getLocation());
                result.getDetectedClass();
                result.getId();
                result.getTitle();*/
            }
            //   trackedObjects.clear();


        }

/*
        private Bitmap drawRect_2() {
            Log.v("MjpegContext", "MjpegContext: " + context);
            RecResults = detectorActivity.getResults();


           // tracker.trackResults(RecResults);
           // mbgPaint = BorderedText.getS_paint();
            //Bitmap bmp = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888);
            //Canvas c = new Canvas(bmp);
            //p.setStyle(Paint.Style.STROKE);
            //p.setAlpha(160);

            // p.setColor(overlayBackgroundColor);
            c.drawRect(mposX, mposXW, mposY, mposYW, p);
          //  c.drawRect(mposX+20, mposXW+20, mposYW+20, mposYW, p);

            //  p.setColor(overlayTextColor);
           // c.drawText(text, -b.left + 1, (bheight / 2) - ((p.ascent() + p.descent()) / 2) + 1, p);
            return bmp;
        }*/

        public void run() {
            start = System.currentTimeMillis();
            PorterDuffXfermode mode = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);
            Bitmap bm;
            int width;
            int height;
            Rect destRect;
            Canvas c = null;
            Paint p = new Paint();
            String fps;
            while (mRun) {
                if (surfaceDone) {
                    try {
                        c = mSurfaceHolder.lockCanvas();
                        synchronized (mSurfaceHolder) {
                            try {
                                bm = mIn.readMjpegFrame();

                                if(bm != null){
                                    everyBitmap = bm;
                                }

                                destRect = destRect(bm.getWidth(), bm.getHeight());
                                c.drawColor(Color.BLACK);
                                c.drawBitmap(bm, null, destRect, p);
                                if (showFps) {
                                    Log.v("checkCanvascheckCanvas", "frameJpeg");
                                    p.setXfermode(mode);
                                    if (ovl != null) {
                                        height = ((ovlPos & 1) == 1) ? destRect.top : destRect.bottom - ovl.getHeight();
                                        width = ((ovlPos & 8) == 8) ? destRect.left : destRect.right - ovl.getWidth();
                                        c.drawBitmap(ovl, width, height, null);
                                        //drawRect();
                                        // c.drawBitmap(drawRect(overlayPaint, bm.getWidth(), bm.getHeight()), 100, 100, null);
                                    }
                                    p.setXfermode(null);
                                    frameCounter++;
                                    if ((System.currentTimeMillis() - start) >= 1000) {
                                        fps = String.valueOf(frameCounter) + " fps";
                                        frameCounter = 0;
                                        start = System.currentTimeMillis();
                                        ovl = makeFpsOverlay(overlayPaint, fps);


                                    }
                                }
                            } catch (IOException e) {
                                e.getStackTrace();
                                Log.d(TAG, "catch IOException hit in run", e);
                            }
                        }
                    } finally {
                        if (c != null) {
                            mSurfaceHolder.unlockCanvasAndPost(c);
                        }
                    }
                }
            }
        }
    }

    private void init(Context context) {
        this.context = context;
        Log.v("MjpegViewContext", "Context: " + context);

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        thread = new MjpegViewThread(holder, context);
        setFocusable(true);
        overlayPaint = new Paint();
        overlayPaint.setTextAlign(Paint.Align.LEFT);
        overlayPaint.setTextSize(12);
        overlayPaint.setTypeface(Typeface.DEFAULT);
        overlayTextColor = Color.WHITE;
        overlayBackgroundColor = Color.BLACK;
        ovlPos = MjpegView.POSITION_LOWER_RIGHT;
        displayMode = MjpegView.SIZE_STANDARD;
        dispWidth = getWidth();
        dispHeight = getHeight();
    }

    public void startPlayback() {
        if (mIn != null) {
            mRun = true;
            thread.start();
        }
    }

    public void stopPlayback() {
        mRun = false;
        boolean retry = true;
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
                e.getStackTrace();
                Log.d(TAG, "catch IOException hit in stopPlayback", e);
            }
        }
    }

    public MjpegView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        // this.context = context;
    }

    public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) {
        thread.setSurfaceSize(w, h);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceDone = false;
        stopPlayback();
    }


    public MjpegView(Context context) {
        super(context);
        init(context);
        //  this.context = context;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        surfaceDone = true;
    }

    public void showFps(boolean b) {
        showFps = b;
    }

    public void setSource(MjpegInputStream source) {
        mIn = source;
        startPlayback();
    }


    public void setOverlayPaint(Paint p) {
        overlayPaint = p;
    }

    public void setOverlayTextColor(int c) {
        overlayTextColor = c;
    }

    public void setOverlayBackgroundColor(int c) {
        overlayBackgroundColor = c;
    }

    public void setOverlayPosition(int p) {
        ovlPos = p;
    }

    public void setDisplayMode(int s) {
        displayMode = s;
    }

    public static Bitmap getBitmap() {
        return everyBitmap;
    }


    //MultiBoxTracker

}

