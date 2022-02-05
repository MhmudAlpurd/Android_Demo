package edu.cmu.pocketsphinx.demo.commandRecogniton;

import android.util.Log;

import java.util.List;

public class runTriggerWord {

    String first_word = "" ;
    String second_word = "" ;
    static boolean isExist;

    static String[] trigger_word_main_keywords ={"hi", "hey", "the"};
    //static List<String> trigger_word_main_keywords_lst = Arrays.asList(trigger_word_main_keywords);

    static String[] trigger_word_secondary_keywords ={"buddy", "body", "by"};
    //static List<String> trigger_word_secondary_keywords_lst = Arrays.asList(trigger_word_secondary_keywords);

    public static boolean recTriggerWord(List lst_words){
        isExist = false;
            if(lst_words.size() > 1) {
                Object first_word = lst_words.get(0);
                Object second_word = lst_words.get(1);
                for (String i : trigger_word_main_keywords) {
                    if (i.equals(first_word)) {  //find word exactly!
                        isExist = true;
                        break;
                    } else {
                        isExist = false;
                    }
                }
                if (isExist) {
                    for (String j : trigger_word_secondary_keywords) {
                        if (j.equals(second_word)) {  //find word exactly!
                            isExist = true;
                            break;
                        } else {
                            isExist = false;
                        }

                    }

                }
            }
        Log.v("res0123", "isExist"+ isExist);
        return isExist;
        }








}
