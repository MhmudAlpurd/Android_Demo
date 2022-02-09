package edu.cmu.pocketsphinx.demo.titleRec;

import android.graphics.Point;
import android.util.Log;

import com.google.mlkit.vision.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TitleRecognizer {
    Map<Integer, String> map = new HashMap<>();
    int cardFrameIteration = 0 ;

    String[] card_keywords={"Java","Python","PHP","C++"};
    List<String> card_keywords_list = new ArrayList<String>();

    public String recognizeTitleForLabel(Text txt, List<Text.Line> lines){
        //return: text of biggest bounding box for label, and ... for card!
        String textOfBiggestBox = null;
        map.clear();

        for (int i=0; i<txt.getTextBlocks().size(); i++){
            for(int j=0; j<txt.getTextBlocks().get(i).getLines().size(); j++){ //lines of one block!
                Point[] cornersOfLine = txt.getTextBlocks().get(i).getLines().get(j).getCornerPoints();
                //cornersOfLine: (top_left:(x_0, y_0), top_right:(x_1, y_0), bottom_right:(x_1, y_1), bottom_left:(x_0, y_1))
                //biggestBoundingBox = (x_1 - x_0) * (y_1 - y_0)
                //biggestHeight = (y_1 - y_0)

                assert cornersOfLine != null;
                if(cornersOfLine.length != 0){
                    Log.d("Text247", "cornersofline: " + Arrays.toString(cornersOfLine));
                    int y_0 = cornersOfLine[0].y ;
                    int y_1 = cornersOfLine[2].y ;

                   // Log.d("Text247", "y_0: " + y_0);
                   // Log.d("Text247", "y_1: " + y_1);

                    int height_length = Math.abs(y_1 - y_0);
                    String txtOfLine = txt.getTextBlocks().get(i).getLines().get(j).getText();

                    Log.d("Text247", "textofline: " + txtOfLine);

                    map.put(height_length, txtOfLine);
                   // Log.d("Text247", "liney: " + y_0);
                   // Log.d("Text247", "linexy: " + Arrays.toString(new Point[]{cornersOfLine[2]}));

                }


            }
}
        Log.d("TESTMAP", map.toString());
        textOfBiggestBox = textOfMaxLength(map);
        return textOfBiggestBox;

    }


    public String recognizeTitleForCard(Text txt){
        cardFrameIteration +=1;
        int wordNumberIteration = 0;
        StringBuilder resText = null;
        String cardReturned = wordExistance(txt);
        if(cardReturned == null){
            String text = txt.getText().replaceAll("\\d", " ");
            String res = spliteTXT(text, 10);
            return res;
        }else {
            Log.v("testt841", "cardreturned: " + cardReturned);

            return cardReturned;
        }
    }


    public String textOfMaxLength(Map<Integer, String> map){
        int maxKey = 0;

         Set<Integer> mapKeys =  map.keySet();

         for (int i : mapKeys) {
             if (i > maxKey) {
                 maxKey = i;
             }
         }

        Log.d("TESTMAP", "Values: " + map.values());
        Log.d("TESTMAP", "Keys: " + map.keySet());
        Log.d("TESTMAP", "maxKey: " + maxKey);
        Log.d("TESTMAP", "maxValue: " + map.get(maxKey));
        return map.get(maxKey);

    }

    public String isExist(Text txt) {
        String existedKeyword = null;
        boolean isContains = false;

        for (int i = 0; i < txt.getTextBlocks().size(); i++) {
            for (int j = 0; j < txt.getTextBlocks().get(i).getLines().size(); j++) { //lines of one block!
                for (int k = 0; k < txt.getTextBlocks().get(i).getLines().get(j).getElements().size(); k++){
                    String kElement = txt.getTextBlocks().get(i).getLines().get(j).getElements().get(k).getText();
                    isContains = Arrays.stream(card_keywords).anyMatch(kElement.toLowerCase().trim() :: equals);
                    Log.d("cardRet123", "word: " + kElement);
                    if (isContains){
                        existedKeyword = kElement.toLowerCase().trim();
                        Log.d("cardRet123", "isContains: " + "True");
                        break;
                    }
                    Log.d("cardRet123", "isContains: " + "False");

                }
            }
        }
        return existedKeyword;
    }


    public String wordExistance(Text txt) {
        String existedKeyword = null;
        card_keywords_list.add("bank");
        card_keywords_list.add("library");
        card_keywords_list.add("identification");

        for(String txtSearch : card_keywords_list){
            Log.d("contain123", "txtSearch: " + txtSearch);
            Pattern pattern = Pattern.compile(txtSearch, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(txt.getText());
            Log.d("contain123", "txt: " + txt.getText());
            boolean matchFound = matcher.find();
            if(matchFound) {
                existedKeyword = txtSearch;
                break;
            }
        }

        Log.d("contain123", "exitword: " + existedKeyword);
        return existedKeyword;

    }


    private String spliteTXT(String s, int num){

        String[] splited = s.split(" ", num+1);
        List<String> list = new ArrayList<String>(Arrays.asList(splited));
        list.remove(num);

        String result = list.stream()
                .map(n -> String.valueOf(n))
                .collect(Collectors.joining(" ", "", ""));

        return result;
    }




}
