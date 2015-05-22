package jeholmes.whatspouring;

/**
 * Created by Jon on 12/05/2015.
 * Loading screen
 */

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

public class Splash extends Activity implements Runnable {

    private String[] breweries;
    //private String[] addresses;
    private String[] urls;
    private String[] listDOMid;
    private String[] listContainer;
    private String[] listSecondary;
    private String[] iconAssets;
    private double[] lats;
    private double[] longs;
    private String[] beers;

    public int totalBreweries;

    private volatile boolean scrapeFinished = false;

    Thread transitionThread  = new Thread(this);

    Thread scrapeThread = new Thread() {
        public void run() {

            String[] next;
            try {
                CSVReader reader = new CSVReader(new InputStreamReader(getAssets().open("data.csv")));
                int i = 0;
                while(true) {
                    next = reader.readNext();
                    if(next != null) {
                        switch (i) {
                            case 0:
                                totalBreweries = next.length;
                                breweries = next;
                                break;
                            case 1:
                                //addresses = next;
                                break;
                            case 2:
                                urls = next;
                                break;
                            case 3:
                                listDOMid = next;
                                break;
                            case 4:
                                listContainer = next;
                                break;
                            case 5:
                                listSecondary = next;
                                break;
                            case 6:
                                iconAssets = next;
                                break;
                            case 7:
                                double[] latsparsed = new double[next.length];
                                for (int j = 0; j<next.length; j++) latsparsed[j] = Double.valueOf(next[j]);
                                lats = latsparsed;
                                break;
                            case 8:
                                double[] longsparsed = new double[next.length];
                                for (int j = 0; j<next.length; j++) longsparsed[j] = Double.valueOf(next[j]);
                                longs = longsparsed;
                                break;
                        }
                        i++;
                    } else {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            beers = new String[totalBreweries];

            //Log.v("arrcheck", "breweries: " + breweries[0] + " - " + breweries[1] + " - " + breweries[2] + " - " + breweries[3]);

            for (int i = 0; i < totalBreweries; i++) {
                //Do synchronous?
                fetchBeerlist(i);
            }

            scrapeFinished = true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        scrapeThread.start();

        transitionThread.start();
    }

    @Override
    public void run()
    {
        try {
            while (!scrapeFinished) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        finally {
            Intent intent = new Intent(this, MapsActivity.class);

            intent.putExtra("total", totalBreweries);
            intent.putExtra("nameArr", new ArrayList<>(Arrays.asList(breweries)));
            //intent.putExtra("addrArr", new ArrayList<String>(Arrays.asList(addresses)));
            //intent.putExtra("urlArr", new ArrayList<String>(Arrays.asList(urls)));
            //intent.putExtra("listArr", new ArrayList<String>(Arrays.asList(listDOMid)));
            //intent.putExtra("contArr", new ArrayList<String>(Arrays.asList(listContainer)));
            //intent.putExtra("secArr", new ArrayList<String>(Arrays.asList(listSecondary)));
            intent.putExtra("iconArr", new ArrayList<>(Arrays.asList(iconAssets)));
            intent.putExtra("latArr", lats);
            intent.putExtra("longArr",longs);
            intent.putExtra("beerArr", new ArrayList<>(Arrays.asList(beers)));
            startActivity(intent);

            finish();
        }
    }

    private void fetchBeerlist(int i) {
        Document document = null;

        //Log.v("dump", "!!!!!!!!!! PAGE DUMP FOR " + breweries[i] + " !!!!!!!!!!");

        while (document==null) {
            try {
                document = Jsoup.connect(urls[i]).get();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        //Log.v("dump", document.toString());

        Elements listDOM = document.select(listDOMid[i]);
        Elements beerDOMs = listDOM.select(listContainer[i]);
        int j = 0;
        Elements typeDOMs = null;
        if (listSecondary[i].length() > 0) {
            typeDOMs = listDOM.select(listSecondary[i]);
        }

        String returnStr = "\n";

        for( Element beerDOM : beerDOMs) {

            if (beerDOM.text().length() > 0) {
                returnStr += "\u2022 " + beerDOM.text();

                if (listSecondary[i].length() > 0) {
                    assert typeDOMs != null;
                    returnStr += " " + typeDOMs.get(j).text();
                    j++;
                }
                returnStr += "\n";

                if (beerDOM.text().equals("BRAINSTORMS")) { // Bandage for Storm's website
                    break;
                }
            }
        }

        while (returnStr.substring(returnStr.length() - 2).equals("\n")) {
            returnStr = returnStr.substring(0, returnStr.length() - 2);
        }

        beers[i] = returnStr;
    }

}
