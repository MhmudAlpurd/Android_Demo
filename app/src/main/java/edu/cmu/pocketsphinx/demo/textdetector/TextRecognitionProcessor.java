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

package edu.cmu.pocketsphinx.demo.textdetector;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import edu.cmu.pocketsphinx.demo.PocketSphinxActivity;
import edu.cmu.pocketsphinx.demo.textReading.GraphicOverlay;
import edu.cmu.pocketsphinx.demo.textReading.VisionProcessorBase;
import edu.cmu.pocketsphinx.demo.preference.PreferenceUtils;
import edu.cmu.pocketsphinx.demo.titleRec.TitleRecognizer;
import edu.cmu.pocketsphinx.demo.textToSpeech.Speech;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.Text.Element;
import com.google.mlkit.vision.text.Text.Line;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Processor for the text detector demo. */
public class TextRecognitionProcessor extends VisionProcessorBase<Text> {

  private static final String TAG = "TextRecProcessor";

  private final TextRecognizer textRecognizer;
  private final Boolean shouldGroupRecognizedTextInBlocks;
  private final Boolean showLanguageTag;

  String filename = "";
  String filepath = "";
  String fileContent = "";
  String title = null;
  String desc = "description_test";
  String txtArea = "area";
  String imgText = "text";
  String card_or_label = "";
  Context context;
  int iterat = 0;

  TitleRecognizer titleRecognizer = new TitleRecognizer();
  private static long startTime = 0;



  public TextRecognitionProcessor(
      Context context, TextRecognizerOptionsInterface textRecognizerOptions, String isLabel_or_isCard) {
    super(context);
    this.context = context;
    shouldGroupRecognizedTextInBlocks = PreferenceUtils.shouldGroupRecognizedTextInBlocks(context);
    showLanguageTag = PreferenceUtils.showLanguageTag(context);
    textRecognizer = TextRecognition.getClient(textRecognizerOptions);
    card_or_label = isLabel_or_isCard;
    startTime = SystemClock.uptimeMillis();
    Log.v("isLabel_or_isCard", "TextRecognition: " + card_or_label);
  }

  @Override
  public void stop() {
    Log.d("isLabel_or_isCard", "Stop");
    super.stop();
    textRecognizer.close();
  }

  @Override
  protected Task<Text> detectInImage(InputImage image) {
    Log.d("isLabel_or_isCard", "detectImage");
    Log.v("gggttt","image_name: "  +image.getBitmapInternal());
    return textRecognizer.process(image);
  }

  @Override
  protected void onSuccess(@NonNull Text text, @NonNull GraphicOverlay graphicOverlay) {
    goToMain(startTime, SystemClock.uptimeMillis());
    logExtrasForTesting(text);
    graphicOverlay.add(
        new TextGraphic(graphicOverlay, text, shouldGroupRecognizedTextInBlocks, showLanguageTag));
  }

  private void logExtrasForTesting(Text text) {
    goToMain(startTime, SystemClock.uptimeMillis());
    if (text != null) {
      Log.v(MANUAL_TESTING_LOG, "Detected text has : " + text.getTextBlocks().size() + " blocks");
      Log.d("Text243", text.getText()); //text line by line!
      Log.d("testnull", "all: " + spendedTime(startTime,SystemClock.uptimeMillis()  ));

      if(text.getText().isEmpty()){
        goToMain_ifNotFound(startTime, SystemClock.uptimeMillis());
        Log.d("testnulll", "cardtime: " + spendedTime(startTime,SystemClock.uptimeMillis() ));
      }

      Log.d("testnulll", "cardtext: " + text.getText());

      for (int i = 0; i < text.getTextBlocks().size(); ++i) {
        List<Line> lines = text.getTextBlocks().get(i).getLines();
        if(card_or_label.equals("Label")){
          title = titleRecognizer.recognizeTitleForLabel(text, lines);
          Log.d("label_text", "title: "+title);
        }else if (card_or_label.equals("Card")){
          title = titleRecognizer.recognizeTitleForCard(text);
        }else {
          title = titleRecognizer.recognizeTitleForLabel(text, lines);
        }

        if(title != null && title != ""){
          iterat ++;
          startTime = SystemClock.uptimeMillis();
          if ( iterat % 300 == 0){
            Speech.talk(title, context);
            iterat = 0;
          }
        }



        Log.v(
            MANUAL_TESTING_LOG,
            String.format("Detected text block %d has %d lines", i, lines.size()));
        for (int j = 0; j < lines.size(); ++j) {
          List<Element> elements = lines.get(j).getElements();
          Log.v(
              MANUAL_TESTING_LOG,
              String.format("Detected text line %d has %d elements", j, elements.size()));
          for (int k = 0; k < elements.size(); ++k) {
            Element element = elements.get(k);
            Log.v("Test123", "Corner Points: " + Arrays.toString(element.getCornerPoints()));
            Log.v(
                MANUAL_TESTING_LOG,
                String.format("Detected text element %d says: %s", k, element.getText()));
            Log.v(
                MANUAL_TESTING_LOG,
                String.format(
                    "Detected text element %d has a bounding box: %s",
                    k, element.getBoundingBox().flattenToString()));
            Log.v(
                MANUAL_TESTING_LOG,
                String.format(
                    "Expected corner point size is 4, get %d", element.getCornerPoints().length));
            for (Point point : element.getCornerPoints()) {
              Log.v(
                  MANUAL_TESTING_LOG,
                  String.format(
                      "Corner point for element %d is located at: x - %d, y = %d",
                      k, point.x, point.y));
            }
          }
        }
      }
    }
  }

  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.d("isLabel_or_isCard", "onFailure: " + e);
    goToMain(startTime, SystemClock.uptimeMillis());
    Log.w(TAG, "Text detection failed." + e);
  }



  public void toFirstActivity(){
    Speech.stopTalking(context);
    Intent intent = new Intent(context, PocketSphinxActivity.class);
    context.startActivity(intent);
  }

  public void goToMain(long start, long end){

    if (spendedTime(start, end) > 40){
      toFirstActivity();
    }else {
      //nothing
    }
  };

  int iteration= 0;
  public void goToMain_ifNotFound(long start, long end){
    if (10 < spendedTime(start, end) && spendedTime(start, end) < 11 && iteration==0){
        iteration += 1;
        Speech.talk("There is nothing in front of the camera.", context);
    }else if (20 < spendedTime(start, end) && spendedTime(start, end) < 21 && iteration==1){
        iteration += 1;
        Speech.talk("There is nothing in front of the camera.", context);
    }else if (30 < spendedTime(start, end) && spendedTime(start, end) < 30.2 && iteration==2) {
      iteration += 1;
      Speech.talk("Text reading module disabled", context);
    }else if (spendedTime(start, end) > 30.2){
      toFirstActivity();
    }else {
      //nothing
    }
  };

  public double spendedTime(long startTime, long endTime){
    double elapsedTimeInSecond =  (double) (endTime - startTime) / 1000 ;
    return  elapsedTimeInSecond;

  }

}
