/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.cmu.pocketsphinx.demo.textReading;

import static android.widget.Toast.makeText;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.core.content.ContextCompat;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;
import edu.cmu.pocketsphinx.demo.ModulesActivity;
import edu.cmu.pocketsphinx.demo.PocketSphinxActivity;
import edu.cmu.pocketsphinx.demo.R;
import edu.cmu.pocketsphinx.demo.preference.SettingsActivity;
import edu.cmu.pocketsphinx.demo.textdetector.TextRecognitionProcessor;
import com.google.android.gms.common.annotation.KeepName;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/** Live preview demo for ML Kit APIs. */
@KeepName
public final class LivePreviewActivity extends Activity
    implements OnRequestPermissionsResultCallback,
        OnItemSelectedListener,
        CompoundButton.OnCheckedChangeListener, RecognitionListener {
  private static final String OBJECT_DETECTION = "Object Detection";
  private static final String TEXT_RECOGNITION_LATIN = "Text Recognition Latin";


  private static final String TAG = "LivePreviewActivity";
  private static final int PERMISSION_REQUESTS = 1;
  String isLabel_or_isCard ;
  private edu.cmu.pocketsphinx.demo.textReading.CameraSource cameraSource = null;
  private CameraSourcePreview preview;
  private GraphicOverlay graphicOverlay;
  private String selectedModel = OBJECT_DETECTION;

  String message_from_SpeechRecActivity= null;

  /* Named searches allow to quickly reconfigure the decoder */
  private static final String KWS_SEARCH = "thanks buddy";
  private static final String FORECAST_SEARCH = "forecast";
  private static final String DIGITS_SEARCH = "digits";
  private static final String PHONE_SEARCH = "phones";
  private static final String MENU_SEARCH = "stop buddy";
  private static final String KEYPHRASE = "stop buddy";

  /* Used to handle permission request */
  private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
  private static SpeechRecognizer recognizer;

  private static long startTime = 0;
  double elapsedTimeInSecond = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_vision_live_preview);

    startTime = SystemClock.uptimeMillis();

    preview = findViewById(R.id.preview_view);
    if (preview == null) {
      Log.d(TAG, "Preview is null");
    }
    graphicOverlay = findViewById(R.id.graphic_overlay);
    if (graphicOverlay == null) {
      Log.d(TAG, "graphicOverlay is null");
    }


    String whichModule = getIntent().getStringExtra("whichModule");
    String whichObject = getIntent().getStringExtra("whichObject");

    if (whichObject != null) {
      isLabel_or_isCard = whichObject;
    }else {
      isLabel_or_isCard = "label";
    }

    Spinner spinner = findViewById(R.id.spinner);
    List<String> options = new ArrayList<>();
    options.add(TEXT_RECOGNITION_LATIN);


    // Creating adapter for spinner
    ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_style, options);
    // Drop down layout style - list view with radio button
    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    // attaching data adapter to spinner
    spinner.setAdapter(dataAdapter);
    spinner.setOnItemSelectedListener(this);

    ToggleButton facingSwitch = findViewById(R.id.facing_switch);
    facingSwitch.setOnCheckedChangeListener(this);

    ImageView settingsButton = findViewById(R.id.settings_button);
    settingsButton.setOnClickListener(
        v -> {
          Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
          intent.putExtra(
              SettingsActivity.EXTRA_LAUNCH_SOURCE, SettingsActivity.LaunchSource.LIVE_PREVIEW);
          startActivity(intent);
        });

    if (allPermissionsGranted()) {
      createCameraSource(selectedModel);
    } else {
      getRuntimePermissions();
    }

    // Check if user has given permission to record audio
    int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
      return;
    }


    new LivePreviewActivity.SetupTask(this).execute();
  }

  private static class SetupTask extends AsyncTask<Void, Void, Exception> {
    WeakReference<LivePreviewActivity> activityReference;
    SetupTask(LivePreviewActivity activity) {
      this.activityReference = new WeakReference<>(activity);
    }

    @Override
    protected Exception doInBackground(Void... params) {
      try {
        Assets assets = new Assets(activityReference.get());
        File assetDir = assets.syncAssets();
        activityReference.get().setupRecognizer(assetDir);
      } catch (IOException e) {
        return e;
      }
      return null;
    }
    @Override
    protected void onPostExecute(Exception result) {
      Log.v("DB_Modules", "onPostExecute: " + "01");

      if (result != null) {
        //  ((TextView) activityReference.get().findViewById(R.id.caption_text)).setText("Failed to init recognizer " + result);
      } else {
        activityReference.get().switchSearch(KWS_SEARCH);
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         @NonNull String[] permissions, @NonNull  int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    goToMain(startTime, SystemClock.uptimeMillis());
    if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new LivePreviewActivity.SetupTask(this).execute();
      } else {
        finish();
      }
    }

    Log.i(TAG, "Permission granted!");
    if (allPermissionsGranted()) {
      createCameraSource(selectedModel);
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  @Override
  public synchronized void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
    // An item was selected. You can retrieve the selected item using
    // parent.getItemAtPosition(pos)
    selectedModel = parent.getItemAtPosition(pos).toString();
    Log.d(TAG, "Selected model: " + selectedModel);
    Log.d("TestOnCreate", "onItemSelected");
    preview.stop();
    if (allPermissionsGranted()) {
      createCameraSource(selectedModel);
      startCameraSource();
    } else {
      getRuntimePermissions();
    }
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
    // Do nothing.
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    Log.d("TestOnCreate", "onCheckedChanged");

    Log.d(TAG, "Set facing");
    if (cameraSource != null) {
      if (isChecked) {
        cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
      } else {
        cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
      }
    }
    preview.stop();
    startCameraSource();
  }

  private void createCameraSource(String model) {
    Log.d("TestOnCreate", "createCameraSource");

    // If there's no existing cameraSource, create one.
    if (cameraSource == null) {
      cameraSource = new CameraSource(this, graphicOverlay);
    }

    try {

        //case TEXT_RECOGNITION_LATIN:
          Log.i(TAG, "Using on-device Text recognition Processor for Latin.");
          cameraSource.setMachineLearningFrameProcessor(
              new TextRecognitionProcessor(this, new TextRecognizerOptions.Builder().build(), isLabel_or_isCard));

    } catch (RuntimeException e) {
      Log.e(TAG, "Can not create image processor: " + model, e);
      Toast.makeText(
              getApplicationContext(),
              "Can not create image processor: " + e.getMessage(),
              Toast.LENGTH_LONG)
          .show();
    }
  }

  /**
   * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
   * (e.g., because onResume was called before the camera source was created), this will be called
   * again when the camera source is created.
   */
  private void startCameraSource() {
    Log.d("TestOnCreate", "startCameraSource");
    if (cameraSource != null) {
      try {
        if (preview == null) {
          Log.d(TAG, "resume: Preview is null");
        }
        if (graphicOverlay == null) {
          Log.d(TAG, "resume: graphOverlay is null");
        }
        preview.start(cameraSource, graphicOverlay);
      } catch (IOException e) {
        Log.e(TAG, "Unable to start camera source.", e);
        cameraSource.release();
        cameraSource = null;
      }
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d("TestOnCreate", "onResume");

    Log.d(TAG, "onResume");
    createCameraSource(selectedModel);
    startCameraSource();
  }

  /** Stops the camera. */
  @Override
  protected void onPause() {
    super.onPause();
    preview.stop();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (cameraSource != null) {
      cameraSource.release();
    }
    goToMain(startTime, SystemClock.uptimeMillis());
    super.onDestroy();
    Log.v("DB_Modules", "onDestroy: " + "01");

    if (recognizer != null) {
      recognizer.cancel();
      recognizer.shutdown();
    }
  }

  private String[] getRequiredPermissions() {
    try {
      PackageInfo info =
          this.getPackageManager()
              .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
      String[] ps = info.requestedPermissions;
      if (ps != null && ps.length > 0) {
        return ps;
      } else {
        return new String[0];
      }
    } catch (Exception e) {
      return new String[0];
    }
  }

  private boolean allPermissionsGranted() {
    for (String permission : getRequiredPermissions()) {
      if (!isPermissionGranted(this, permission)) {
        return false;
      }
    }
    return true;
  }

  private void getRuntimePermissions() {
    List<String> allNeededPermissions = new ArrayList<>();
    for (String permission : getRequiredPermissions()) {
      if (!isPermissionGranted(this, permission)) {
        allNeededPermissions.add(permission);
      }
    }

    if (!allNeededPermissions.isEmpty()) {
      ActivityCompat.requestPermissions(
          this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
    }
  }

  private static boolean isPermissionGranted(Context context, String permission) {
    if (ContextCompat.checkSelfPermission(context, permission)
        == PackageManager.PERMISSION_GRANTED) {
      Log.i(TAG, "Permission granted: " + permission);
      return true;
    }
    Log.i(TAG, "Permission NOT granted: " + permission);
    return false;
  }

  @Override
  public void onPartialResult(Hypothesis hypothesis) {
    goToMain(startTime, SystemClock.uptimeMillis());

    Log.v("DB_Modules", "onPartialResult: " + "01");

    if (hypothesis == null)
      return;

    String text = hypothesis.getHypstr();
    if (text.equals(KEYPHRASE) | text.equals(KWS_SEARCH)) {
      Log.v("TEST_ASR_SERVICE", "stop Buddy");
     // switchSearch(MENU_SEARCH);
      intentToPocketActivity();
      onDestroy();
    }
        /*else if (text.equals(DIGITS_SEARCH))
            switchSearch(DIGITS_SEARCH);
        else if (text.equals(PHONE_SEARCH))
            switchSearch(PHONE_SEARCH);
        else if (text.equals(FORECAST_SEARCH))
            switchSearch(FORECAST_SEARCH);*/
    else {
      //((TextView) findViewById(R.id.result_text)).setText(text);
    }

  }

  private void intentToPocketActivity() {
    goToMain(startTime, SystemClock.uptimeMillis());
    Log.v("DB_Modules", "intentToPocketActivity: " + "01");

    if (recognizer != null) {
      recognizer.cancel();
      recognizer.shutdown();
    }
    Intent intent = new Intent(LivePreviewActivity.this, PocketSphinxActivity.class);
    startActivity(intent);
  }
  @Override
  public void onResult(Hypothesis hypothesis) {
    goToMain(startTime, SystemClock.uptimeMillis());

    Log.v("DB_Modules", "onResult: " + "01");

    if (hypothesis != null) {
      String text = hypothesis.getHypstr();
      makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onBeginningOfSpeech() {
    goToMain(startTime, SystemClock.uptimeMillis());

    Log.v("DB_Modules", "onBeginningOfSpeech: " + "01");

  }

  /**
   * We stop recognizer here to get a final result
   */
  @Override
  public void onEndOfSpeech() {
    goToMain(startTime, SystemClock.uptimeMillis());

    Log.v("DB_Modules", "onEndOfSpeech: " + "01");

    if (!recognizer.getSearchName().equals(KWS_SEARCH))
      switchSearch(KWS_SEARCH);
  }


  private void switchSearch(String searchName) {
    goToMain(startTime, SystemClock.uptimeMillis());
    recognizer.stop();

    // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
    if (searchName.equals(KWS_SEARCH))
      recognizer.startListening(searchName);
    else
      recognizer.startListening(searchName, 10000);
  }

  private void setupRecognizer(File assetsDir) throws IOException {
    goToMain(startTime, SystemClock.uptimeMillis());
    // The recognizer can be configured to perform multiple searches
    // of different kind and switch between them
    int sensibility = 50;
    recognizer = SpeechRecognizerSetup.defaultSetup()
            .setAcousticModel(new File(assetsDir, "en-us-ptm"))
            .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
            .setKeywordThreshold(Float.valueOf("1.e-" + 2 * sensibility)) //Sensibility

            .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)

            .getRecognizer();
    recognizer.addListener(this);

        /* In your application you might not need to add all those searches.
          They are added here for demonstration. You can leave just one.
         */

    // Create keyword-activation search.
    recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

    // Create grammar-based search for selection between demos
    File menuGrammar = new File(assetsDir, "menu.gram");
    recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);

    // Create grammar-based search for digit recognition
    File digitsGrammar = new File(assetsDir, "digits.gram");
    recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);

    // Create language model search
    File languageModel = new File(assetsDir, "weather.dmp");
    recognizer.addNgramSearch(FORECAST_SEARCH, languageModel);

    // Phonetic search
    File phoneticModel = new File(assetsDir, "en-phone.dmp");
    recognizer.addAllphoneSearch(PHONE_SEARCH, phoneticModel);
  }



  @Override
  public void onError(Exception e) {
    Log.v("DB_Modules", "onError: " + "01");
    goToMain(startTime, SystemClock.uptimeMillis());
    // ((TextView) findViewById(R.id.caption_text)).setText(error.getMessage());
  }

  @Override
  public void onTimeout() {
    Log.v("DB_Modules", "onTimeout: " + "01");

    switchSearch(KWS_SEARCH);
  }


  public double goToMain(long start, long end){
    elapsedTimeInSecond =  (double) (end - start) / 1000 ;
    if (elapsedTimeInSecond > 20){
      intentToPockActivity();
    }else {

    }
    return  elapsedTimeInSecond;
  };

  private void intentToPockActivity() {
    if (recognizer != null) {
      recognizer.cancel();
      recognizer.shutdown();
    }
    Intent intent = new Intent(LivePreviewActivity.this, PocketSphinxActivity.class);
    startActivity(intent);
  }
}
