package jeholmes.whatspouring;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;

public class MapsActivity extends FragmentActivity {

    class MyInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private final View myContentsView;

        MyInfoWindowAdapter(){
            myContentsView = getLayoutInflater().inflate(R.layout.windowlayout, null);
        }

        @Override
        public View getInfoContents(Marker marker) {

            TextView tvTitle = ((TextView)myContentsView.findViewById(R.id.title));
            tvTitle.setText(marker.getTitle());
            TextView tvSnippet = ((TextView)myContentsView.findViewById(R.id.snippet));
            tvSnippet.setText(marker.getSnippet());

            return myContentsView;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }
    }


    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        /*Thread welcomeThread = new Thread() {

            @Override
            public void run() {
                try {
                    super.run();
                    sleep(1000);  //Delay of 10 seconds
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {

                    Intent i = new Intent(MapsActivity.this,
                            Splash.class);
                    startActivity(i);
                    finish();
                }
            }
        };
        welcomeThread.start();*/

        scrapeThread.start();

        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private String[] breweries = {  "Powell Street Craft Brewery",  "Strange Fellows Brewing",  "Parallel 49 Brewing",  "Storm Brewing", "Bomber Brewing"   };
    private String[] addresses = {  "1357 Powell Street, Vancouver",           "1345 Clark Drive, Vancouver",         "1950 Triumph Street, Vancouver",  "310 Commercial Drive, Vancouver", "1488 Adanac Street, Vancouver"};
    private String[] urls = {"http://www.powellbeer.com/?_escaped_fragment_=beers/cee5", "http://strangefellowsbrewing.com/", "http://parallel49brewing.com/beers/", "http://www.stormbrewing.org/?_escaped_fragment_=thebeers/csgz", "http://www.bomberbrewing.com/beers#brewery-54"};
    private String[] listDOMid = {  "#i325kxr3",                    "#beers",                    "div.always_around", "#i4nv7xib", "div.view-brewery"};
    private String[] listContainer = {"li",                         "div.beer > div.inner",      "div.beer-list > div.beer > h2", "p.font_8 > span > span", "div.views-row > div.node-beer > div.row-fluid > div.span8 > div.field-name-title"};
    private String[] listSecondary = {"",                            "",                         "div.beer-list > div.beer > p.intro", "", ""};
    private String[] iconAssets = {"pscb.png",                      "sf.png",                      "p49.png",   "storm.png", "bomber.png"};

    private String[] beers = {"","","", "", ""};
    private double[] lats = {0,0,0,0,0};
    private double[] longs = {0,0,0,0,0};

    private volatile boolean scrapeFinished = false;

    final Object LOCK = new Object();

    int totalBreweries = 4; //breweries.length

    public JSONObject getLocationInfo(String address) {

        address = address + ", BC, Canada";
        address = address.replaceAll(" ","+");

        HttpGet httpGet = new HttpGet("http://maps.googleapis.com/maps/api/geocode/json?address="+address+"&sensor=true");
        HttpClient client = new DefaultHttpClient();
        HttpResponse response;
        StringBuilder stringBuilder = new StringBuilder();

        try {
            response = client.execute(httpGet);
            HttpEntity entity = response.getEntity();
            InputStream stream = entity.getContent();
            int b;
            while ((b = stream.read()) != -1) {
                stringBuilder.append((char) b);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject = new JSONObject(stringBuilder.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    Thread scrapeThread = new Thread() {
        public void run() {

            for (int i = 0; i < totalBreweries; i++) {

                //Log.v("iterator", "starting processing for brewery " + i);

                Document document = null;
                try {

                    //Log.v("iterator", "trying to get coord for " + i);
                    JSONObject ret = getLocationInfo(addresses[i]);

                    JSONObject location;

                    Double lat_val = 0.0;
                    Double long_val = 0.0;

                    try {
                        location = ret.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
                        lat_val = location.getDouble("lat");
                        long_val = location.getDouble("lng");
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }

                    lats[i] = lat_val;
                    longs[i] = long_val;

                    //Log.v("iterator", "trying to get webpage for " + i);
                    while (document==null) {
                        document = Jsoup.connect(urls[i]).get();
                    }

                    //Log.v("iterator", "connection success for " + i);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //Log.v("iterator", "asserting document for pointer " + i);
                assert document != null;

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

            scrapeFinished = true;
            synchronized (LOCK) {
                LOCK.notifyAll();
            }
        }
    };

    private void setUpMap() {

        mMap.setInfoWindowAdapter(new MyInfoWindowAdapter());

        double latAvg = 0.0;
        double longAvg = 0.0;

        synchronized (LOCK) {
            while (!scrapeFinished) {
                try { LOCK.wait(); }
                catch (InterruptedException e) {
                    break;
                }
            }
        }

        for (int i = 0; i < totalBreweries; i++) {
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(lats[i], longs[i]))
                    .title(breweries[i])
                    .snippet(beers[i])
                    .icon(BitmapDescriptorFactory.fromAsset(iconAssets[i])));
            latAvg += lats[i];
            longAvg += longs[i];
        }
        latAvg = latAvg / totalBreweries;
        longAvg = longAvg / totalBreweries;

        mMap.moveCamera( CameraUpdateFactory.newLatLngZoom(new LatLng(latAvg, longAvg), 14.5f) );
    }
}
