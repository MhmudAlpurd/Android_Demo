/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.cmu.pocketsphinx.demo.objectDetection;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

import edu.cmu.pocketsphinx.demo.R;
import edu.cmu.pocketsphinx.demo.objectDetection.customview.OverlayView;
import edu.cmu.pocketsphinx.demo.objectDetection.customview.OverlayView.DrawCallback;
import edu.cmu.pocketsphinx.demo.objectDetection.env.BorderedText;
import edu.cmu.pocketsphinx.demo.objectDetection.env.ImageUtils;
import edu.cmu.pocketsphinx.demo.objectDetection.env.Logger;
import edu.cmu.pocketsphinx.demo.objectDetection.env.Utils;
import edu.cmu.pocketsphinx.demo.ipcamera.MjpegView;
import edu.cmu.pocketsphinx.demo.objectDetection.tflite.Classifier;
import edu.cmu.pocketsphinx.demo.objectDetection.tflite.DetectorFactory;
import edu.cmu.pocketsphinx.demo.objectDetection.tflite.YoloV5Classifier;
import edu.cmu.pocketsphinx.demo.objectDetection.tracking.MultiBoxTracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();
    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.3f;
    private static final boolean MAINTAIN_ASPECT = true;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(320, 320);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    public static final int TF_OD_API_INPUT_SIZE = 320;
    static List<Classifier.Recognition> RecResults =
            new LinkedList<Classifier.Recognition>();


    OverlayView trackingOverlay;
    private Integer sensorOrientation;

    private YoloV5Classifier detector;
    int frameInOnePeriod = 0;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;
    Bitmap  croppedBitmap_20 = null;
    Bitmap  croppedBitmap_original = null;

    Matrix matrix_90 = new Matrix();

    private boolean computingDetection = false;

    private long timestamp = 0;
    private int jj=0;


    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;
    private Bitmap sourceBitmap;

    public int ii = 0;
    long startTimeFrame = SystemClock.uptimeMillis();
    long endTimeFrame;
    Bitmap btmp = null;


    private BorderedText borderedText;


    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        final int modelIndex = modelView.getCheckedItemPosition();
        final String modelString = modelStrings.get(modelIndex);

        try {
            detector = DetectorFactory.getDetector(getAssets(), modelString);
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        int cropSize = detector.getInputSize();

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        if (isDebug()) {
                            tracker.drawDebug(canvas);
                        }
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    protected void updateActiveModel(int model_checked, int acc_checked) {
        // Get UI information before delegating to background
/*        final int modelIndex = modelView.getCheckedItemPosition();
        final int deviceIndex = deviceView.getCheckedItemPosition();*/

        final int modelIndex = model_checked;
        final int deviceIndex = acc_checked;
        String threads = threadsTextView.getText().toString().trim();
        final int numThreads = Integer.parseInt(threads);

        handler.post(() -> {
            if (modelIndex == currentModel && deviceIndex == currentDevice
                    && numThreads == currentNumThreads) {
                return;
            }
            currentModel = modelIndex; //cpu:0, gpu:1, nnapi:2
            currentDevice = deviceIndex; //fp16:0, int8:1
            currentNumThreads = numThreads;

            // Disable classifier while updating
            if (detector != null) {
                detector.close();
                detector = null;
            }

            // Lookup names of parameters.
            String modelString = modelStrings.get(modelIndex);
            String device = deviceStrings.get(deviceIndex);

            LOGGER.i("Changing model to " + modelString + " device " + device);

            // Try to load model.

            try {
                detector = DetectorFactory.getDetector(getAssets(), modelString);
                // Customize the interpreter to the type of device we want to use.
                if (detector == null) {
                    return;
                }
            }
            catch(IOException e) {
                e.printStackTrace();
                LOGGER.e(e, "Exception in updateActiveModel()");
                Toast toast =
                        Toast.makeText(
                                getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
                toast.show();
                finish();
            }


            if (device.equals("CPU")) {
               detector.useCPU();
               // detector.useNNAPI();
            } else if (device.equals("GPU")) {
               detector.useGpu();
                //detector.useNNAPI();
            } else if (device.equals("NNAPI")) {
                detector.useNNAPI();
            }

            detector.setNumThreads(numThreads);

            int cropSize = detector.getInputSize();
            croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

            frameToCropTransform =
                    ImageUtils.getTransformationMatrix(
                            previewWidth, previewHeight,
                            cropSize, cropSize,
                            sensorOrientation, MAINTAIN_ASPECT);

            cropToFrameTransform = new Matrix();
            frameToCropTransform.invert(cropToFrameTransform);
        });
    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();
        croppedBitmap_20 =Bitmap.createScaledBitmap(MjpegView.getBitmap(),320, 320, false ) ;


        final Canvas canvas = new Canvas(croppedBitmap_20);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap_20);
        }

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        LOGGER.i("Running detection on image " + currTimestamp);
                        final long startTime = SystemClock.uptimeMillis();
                        ii += 1;
                        Log.v("frames", "frames: " + ii);
                        endTimeFrame = SystemClock.uptimeMillis();
                        long spendedTime = endTimeFrame - startTimeFrame;
                        double fpsdetected = ii / ((endTimeFrame - startTimeFrame)*0.001) ;
                        //final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
                        ArrayList<Classifier.Recognition> results = new ArrayList<Classifier.Recognition>();
                        //TODO: CONTROL FRAMES! 250ms
                        Log.d("spendedTime", "spendedTime1: " + spendedTime);

                       if (spendedTime >= 0) {
                           // results = detector.recognizeImage(croppedBitmap);
                           if(MjpegView.getBitmap() != null){
                               matrix_90.postRotate(90);
                               croppedBitmap_original =Bitmap.createScaledBitmap(MjpegView.getBitmap(),320, 320, true );
                               croppedBitmap_20 = Bitmap.createBitmap(croppedBitmap_original, 0, 0, croppedBitmap_original.getWidth(), croppedBitmap_original.getHeight(), matrix_90, true);

                           }else {
                               sourceBitmap = Utils.getBitmapFromAsset(DetectorActivity.this, "kite.jpg");
                               croppedBitmap_20 = Utils.processBitmap(sourceBitmap, TF_OD_API_INPUT_SIZE);
                           }
                           results = detector.recognizeImage(croppedBitmap_20);
                           Log.v("RecognizedResults", "results: " + results);
                           lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                            startTimeFrame = SystemClock.uptimeMillis();
                        }else {
                            lastProcessingTimeMs = 0;
                        }

                        Log.d("resultsTest", "test: " + results);


                        Log.e("CHECK", "run: " + results.size());

                       // cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        cropCopyBitmap = Bitmap.createScaledBitmap(MjpegView.getBitmap(),320, 320, false ) ;
                        final Canvas canvas = new Canvas(cropCopyBitmap);
                        final Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Style.STROKE);
                        paint.setStrokeWidth(2.0f);

                        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                        switch (MODE) {
                            case TF_OD_API:
                                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                                break;
                        }

                        final List<Classifier.Recognition> mappedRecognitions =
                                new LinkedList<Classifier.Recognition>();

                        for (final Classifier.Recognition result : results) {
                            final RectF location = result.getLocation();
                            if (location != null && result.getConfidence() >= minimumConfidence) {
                                canvas.drawRect(location, paint);

                                cropToFrameTransform.mapRect(location);

                                result.setLocation(location);
                                mappedRecognitions.add(result);
                                Log.v("testtest", "DetectorActivity: "+ result);
                            }
                        }
                        Log.v("testss", "mappedRecognition: "+ mappedRecognitions);
                        RecResults = mappedRecognitions;

                        tracker.trackResults(mappedRecognitions);
                        trackingOverlay.postInvalidate();

                        computingDetection = false;

                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        showFrameInfo(previewWidth + "x" + previewHeight);
                                        showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                                        showInference(lastProcessingTimeMs + "ms");
                                    }
                                });
                    }
                });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.tfe_od_camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API;
    }

    @Override
    protected void setUseNNAPI(final boolean isChecked) {
        runInBackground(() -> detector.setUseNNAPI(isChecked));
    }

    @Override
    protected void setNumThreads(final int numThreads) {
        runInBackground(() -> detector.setNumThreads(numThreads));
    }

    public Bitmap setBitmap(Bitmap bmp){
        Log.v("ttteee", "fragmentbmp2: " + bmp);
        return bmp;
    }

    public static List<Classifier.Recognition> getResults(){
        Log.v("RecResults", "RecResults_D: " + RecResults);
        return RecResults;
    }
}
