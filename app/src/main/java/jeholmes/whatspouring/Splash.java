package jeholmes.whatspouring;

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

/**
 * Created by Jon on 12/05/2015.
 * A splash screen, which loads in the information from data.csv, then uses the information
 * to scrape the appropriate websites and retrieve the rest of the information before passing it
 * all on to the main maps activity.
 */

public class Splash extends Activity implements Runnable {

    // Arrays to hold information
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

    // Flag to signify loading complete
    private volatile boolean scrapeFinished = false;

    // Thread to handle the transition from this activity to the maps activity
    Thread transitionThread  = new Thread(this);

    /** Thread to handle loading the information and scraping the websites. */
    Thread scrapeThread = new Thread() {
        public void run() {

            String[] next;
            try {
                // Load data.csv
                CSVReader r = new CSVReader(new InputStreamReader(getAssets().open("data.csv")));
                int i = 0;
                // Assign each line of CSV file to appropriate array
                while(true) {
                    next = r.readNext();
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
                                double[] latsParsed = new double[next.length];
                                for (int j = 0; j < next.length; j++)
                                    latsParsed[j] = Double.valueOf(next[j]);
                                lats = latsParsed;
                                break;
                            case 8:
                                double[] longsParsed = new double[next.length];
                                for (int j = 0; j < next.length; j++)
                                    longsParsed[j] = Double.valueOf(next[j]);
                                longs = longsParsed;
                                break;
                        }
                        // Increment line count
                        i++;
                    } else {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Set total amount of breweries loaded
            beers = new String[totalBreweries];

            for (int i = 0; i < totalBreweries; i++) {
                // Scrapes the website for each brewery
                fetchBeerList(i);
            }

            // Flag loading complete
            scrapeFinished = true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        // Run loading thread
        scrapeThread.start();

        // Then run transition thread
        transitionThread.start();
    }

    /** Class thread to handle the transition from this activity to the main maps activity. */
    @Override
    public void run()
    {
        // Check the flag to make sure the loading is complete
        try {
            while (!scrapeFinished) {
                try {
                    // Wait a second
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        finally {
            Intent intent = new Intent(this, MapsActivity.class);

            // Bundle necessary arrays into intent extras.
            intent.putExtra("total", totalBreweries);
            intent.putExtra("nameArr", new ArrayList<>(Arrays.asList(breweries)));
            //intent.putExtra("addressArr", new ArrayList<String>(Arrays.asList(addresses)));
            //intent.putExtra("urlArr", new ArrayList<String>(Arrays.asList(urls)));
            intent.putExtra("iconArr", new ArrayList<>(Arrays.asList(iconAssets)));
            intent.putExtra("latArr", lats);
            intent.putExtra("longArr",longs);
            intent.putExtra("beerArr", new ArrayList<>(Arrays.asList(beers)));
            startActivity(intent);

            finish();
        }
    }

    /** Method to handle scraping each website to populate the beer lists. */
    private void fetchBeerList(int i) {
        Document document = null;

        // Use JSoup to retrieve html document from url
        while (document==null) {
            try {
                document = Jsoup.connect(urls[i]).get();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Log.v("page dump", document + "");

        // Extract DOMs that contain beer information
        Elements listDOM = document.select(listDOMid[i]);
        Elements beerDOMs = listDOM.select(listContainer[i]);

        // If website keeps style information in different DOMs, extract them as well
        Elements typeDOMs = null;
        if (listSecondary[i].length() > 0) {
            typeDOMs = listDOM.select(listSecondary[i]);
        }

        // Initialize return string
        String returnStr = "\n";

        // Iterate through DOMs and extract the text to construct a bullet point list as a string
        int j = 0;
        for (Element beerDOM : beerDOMs) {
            if (beerDOM.text().length() > 0) {
                // Add a bullet point and the element's text to the return string
                returnStr += "\u2022 " + beerDOM.text();

                // If there's a secondary element for style, extract and add to return string
                if (listSecondary[i].length() > 0) {
                    assert typeDOMs != null;
                    returnStr += " " + typeDOMs.get(j).text();
                    j++;
                }

                // Add end line to return string
                returnStr += "\n";

                // Edge case for Storm Brewing, where "BRAINSTORMS" is the last relevant DOM
                if (beerDOM.text().equals("BRAINSTORMS")) {
                    break;
                }
            }
        }

        // Trim the returns string by removing any trailing end line tokens
        while (returnStr.substring(returnStr.length() - 2).equals("\n")) {
            returnStr = returnStr.substring(0, returnStr.length() - 2);
        }

        // Return string to global beer string array
        beers[i] = returnStr;
    }
}
