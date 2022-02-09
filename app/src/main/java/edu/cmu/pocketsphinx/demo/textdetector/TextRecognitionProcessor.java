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

  TitleRecognizer titleRecognizer = new TitleRecognizer();
  int numberOfFrame = 0;
  int numberOfSpeech = 0;
  int numberOfnullFrame = 0;


  public TextRecognitionProcessor(
      Context context, TextRecognizerOptionsInterface textRecognizerOptions, String isLabel_or_isCard) {
    super(context);
    this.context = context;
    shouldGroupRecognizedTextInBlocks = PreferenceUtils.shouldGroupRecognizedTextInBlocks(context);
    showLanguageTag = PreferenceUtils.showLanguageTag(context);
    textRecognizer = TextRecognition.getClient(textRecognizerOptions);
    card_or_label = isLabel_or_isCard;
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
    Log.d(TAG, "On-device Text detection successful");
    Log.d("isLabel_or_isCard", "onSuccess");


    logExtrasForTesting(text);
    graphicOverlay.add(
        new TextGraphic(graphicOverlay, text, shouldGroupRecognizedTextInBlocks, showLanguageTag));
  }

  private void logExtrasForTesting(Text text) {
    numberOfFrame += 1 ;
    Log.v("isLabel_or_isCard", "logExtras: " + "entrance");
    if (text != null && numberOfFrame % 40 == 0) {
      Log.v(MANUAL_TESTING_LOG, "Detected text has : " + text.getTextBlocks().size() + " blocks");
      Log.d("Text243", text.getText()); //text line by line!

      for (int i = 0; i < text.getTextBlocks().size(); ++i) {
        List<Line> lines = text.getTextBlocks().get(i).getLines();
        if(card_or_label.equals("Label")){
          title = titleRecognizer.recognizeTitleForLabel(text, lines);
          Log.v("isLabel_or_isCard", "logExtras: " + "Label");
          Log.d("isLabel_or_isCard", "Title_card: " + title);

        }else if (card_or_label.equals("Card")){
          title = titleRecognizer.recognizeTitleForCard(text);
          Log.d("isLabel_or_isCard", "Title_card: " + title);
          Log.v("isLabel_or_isCard", "logExtras: " + "Card");

        }else {
          title = titleRecognizer.recognizeTitleForLabel(text, lines);
          Log.d("biggestBB", "Title_else: " + title);
        }
        //Log.d("titletalk", "before: " + title);
        if(title != null && title != ""){
          numberOfSpeech += 1 ;
          numberOfnullFrame = 0;
          Log.d("titletalk", "after: " + title);
          Speech.talk(title, context);

          if(numberOfSpeech == 40){
            Speech.stopTalking(context);
            Intent intent = new Intent(context, PocketSphinxActivity.class);
            intent.putExtra("leaving_message", "Text reading module disabled");
            context.startActivity(intent);
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
    }else {
      numberOfnullFrame += 1;
      //agar bad az 100 frame hich texti kashf nakard.bargard be warning bedeh, age 400 null boud bargard mainactivity.
      switch (numberOfnullFrame){
        case 100:
        case 200:
        case 300:
          Speech.talk("There is no card in front of the camera.", context);
          break;
        case 400:
          Speech.stopTalking(context);
          Intent intent = new Intent(context, PocketSphinxActivity.class);
          intent.putExtra("leaving_message", "Text reading module disabled");
          context.startActivity(intent);

        default:
          Log.v("default", "defalut");




      }
    }
  }

  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.d("isLabel_or_isCard", "onFailure: " + e);


    Log.w(TAG, "Text detection failed." + e);
  }

  public void delay(int milliSecondsToSleep){
    try {
      TimeUnit.MILLISECONDS.sleep(milliSecondsToSleep);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }


}
