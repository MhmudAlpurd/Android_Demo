package edu.cmu.pocketsphinx.demo.objectDetection;

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

import android.app.Fragment;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import edu.cmu.pocketsphinx.demo.R;
import edu.cmu.pocketsphinx.demo.objectDetection.customview.AutoFitTextureView;
import edu.cmu.pocketsphinx.demo.objectDetection.env.ImageUtils;
import edu.cmu.pocketsphinx.demo.objectDetection.env.Logger;

import java.io.IOException;
import java.util.List;

public class LegacyCameraConnectionFragment extends Fragment {
  private static final Logger LOGGER = new Logger();
  /** Conversion from screen rotation to JPEG orientation. */
  private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
  int ii = 0;

  static {
    ORIENTATIONS.append(Surface.ROTATION_0, 90);
    ORIENTATIONS.append(Surface.ROTATION_90, 0);
    ORIENTATIONS.append(Surface.ROTATION_180, 270);
    ORIENTATIONS.append(Surface.ROTATION_270, 180);
  }


  private Camera camera;
  private Camera.PreviewCallback imageListener;
  private Size desiredSize;
  /** The layout identifier to inflate for this Fragment. */
  private int layout;
  /** An {@link AutoFitTextureView} for camera preview. */
  private AutoFitTextureView textureView;
  /**
   * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a {@link
   * TextureView}.
   */
  private final TextureView.SurfaceTextureListener surfaceTextureListener =
      new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(
            final SurfaceTexture texture, final int width, final int height) {

          int index = getCameraId();
          camera = Camera.open(index);
          Log.d("fpsSupported", "LegacyCamera");
          try {
            Camera.Parameters parameters = camera.getParameters();
            //TODO: CHANGE FPS
            //parameters.setPreviewFpsRange(1000, 1000);
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes != null
                && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
              parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            ii += 1;
            List<int[]> getSupportedFPS = parameters.getSupportedPreviewFpsRange();
            Log.d("fpsSupported", "availableFPS: " + getSupportedFPS);
            Log.d("fpsSupported", "ii: " + ii);

/*            parameters.setPreviewFpsRange(1000, 1000);
            if ( parameters.isAutoExposureLockSupported() )
              parameters.setAutoExposureLock( true );*/
            List<Camera.Size> cameraSizes = parameters.getSupportedPreviewSizes();
            Size[] sizes = new Size[cameraSizes.size()];
            int i = 0;
            for (Camera.Size size : cameraSizes) {
              sizes[i++] = new Size(size.width, size.height);
            }
            Size previewSize =
                CameraConnectionFragment.chooseOptimalSize(
                    sizes, desiredSize.getWidth(), desiredSize.getHeight());
            parameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
            camera.setDisplayOrientation(90);
            camera.setParameters(parameters);
            camera.setPreviewTexture(texture);
          } catch (IOException exception) {
            camera.release();
          }

          camera.setPreviewCallbackWithBuffer(imageListener);
          Camera.Size s = camera.getParameters().getPreviewSize();
          camera.addCallbackBuffer(new byte[ImageUtils.getYUVByteSize(s.height, s.width)]);
          textureView.setAspectRatio(s.height, s.width);

          camera.startPreview();
        }

        @Override
        public void onSurfaceTextureSizeChanged(
            final SurfaceTexture texture, final int width, final int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(final SurfaceTexture texture) {
          return true;
        }

        @Override
        public void onSurfaceTextureUpdated(final SurfaceTexture texture) {}
      };
  /** An additional thread for running tasks that shouldn't block the UI. */
  private HandlerThread backgroundThread;

  public LegacyCameraConnectionFragment(
      final Camera.PreviewCallback imageListener, final int layout, final Size desiredSize) {
    this.imageListener = imageListener;
    this.layout = layout;
    this.desiredSize = desiredSize;
  }

  @Override
  public View onCreateView(
      final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
    Log.d("fpsSupported", "LegacyCamera1");
    return inflater.inflate(layout, container, false);
  }

  @Override
  public void onViewCreated(final View view, final Bundle savedInstanceState) {
    Log.d("fpsSupported", "LegacyCamera2");
    textureView = (AutoFitTextureView) view.findViewById(R.id.texture);
  }

  @Override
  public void onActivityCreated(final Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    Log.d("fpsSupported", "LegacyCamera3");
  }

  @Override
  public void onResume() {
    super.onResume();
    startBackgroundThread();
    Log.d("fpsSupported", "LegacyCamera4");
    // When the screen is turned off and turned back on, the SurfaceTexture is already
    // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
    // a camera and start preview from here (otherwise, we wait until the surface is ready in
    // the SurfaceTextureListener).

    if (textureView.isAvailable()) {
      camera.startPreview();
    } else {
      textureView.setSurfaceTextureListener(surfaceTextureListener);
    }
  }

  @Override
  public void onPause() {
    stopCamera();
    stopBackgroundThread();
    super.onPause();
    Log.d("fpsSupported", "LegacyCamera5");
  }

  /** Starts a background thread and its {@link Handler}. */
  private void startBackgroundThread() {
    backgroundThread = new HandlerThread("CameraBackground");
    backgroundThread.start();
  }

  /** Stops the background thread and its {@link Handler}. */
  private void stopBackgroundThread() {
    backgroundThread.quitSafely();
    try {
      backgroundThread.join();
      backgroundThread = null;
    } catch (final InterruptedException e) {
      LOGGER.e(e, "Exception!");
    }
  }

  protected void stopCamera() {
    if (camera != null) {
      camera.stopPreview();
      camera.setPreviewCallback(null);
      camera.release();
      camera = null;
    }
  }

  private int getCameraId() {
    Log.d("fpsSupported", "LegacyCamera6");
    CameraInfo ci = new CameraInfo();
    for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
      Camera.getCameraInfo(i, ci);
      if (ci.facing == CameraInfo.CAMERA_FACING_BACK) return i;
    }
    return -1; // No camera found
  }
}
