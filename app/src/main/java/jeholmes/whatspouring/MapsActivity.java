package jeholmes.whatspouring;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by Jon on 11/05/2015.
 * The main activity of the application. Loads google maps and populates the markers with the
 * loaded data retrieved from the splash activity.
 */

public class MapsActivity extends FragmentActivity {
    
    // The google maps object
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    // Array to hold marker objects for map
    private Marker[] markers;

    // Variables for setting map camera
    double latAvg;
    double longAvg;
    float zoom = 13.5f;

    // String arrays to hold brewery information
    private String[] breweries;
    //private String[] addresses;
    //private String[] urls;
    private String[] iconAssets;
    private double[] lats;
    private double[] longs;
    private String[] beers;

    // String array to hold the beer styles for the filter drop down menu
    private String[] arraySpinner = new String[] {
            "All Beers",
            "Wheat Ale",
            "Lager/Pilsner",
            "Pale Ale/IPA",
            "Best Bitter/ESB",
            "Belgian Ale",
            "Amber Ale",
            "Brown Ale",
            "Porter/Stout"
    };

    // String arrays of different terms for different beer styles
    private static String[] wheatTerms = {"wheat","hefeweizen","wit","blonde","weisse"};
    private static String[] lagerTerms = {"lager","pilsner","kolsch","k\u00F6lsch", "marzen",
            "m\u00E4rzen","helles"};
    private static String[] paleTerms = {"pale","india","IPA","I.P.A"};
    private static String[] bitterTerms = {"bitter","ESB","E.S.B"};
    private static String[] belgianTerms = {"belgian","saison","dubbel","tripel","quad","white"};
    private static String[] amberTerms = {"amber","ruby"};
    private static String[] brownTerms = {"brown","altbier","classic"};
    private static String[] porterTerms = {"porter","stout"};

    public int totalBreweries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Extract extras from intent sent from splash loading activity
        Bundle extras = getIntent().getExtras();

        // Set total amount of breweries loaded
        totalBreweries = extras.getInt("total");

        // Initialize markers for map object
        markers = new Marker[totalBreweries];

        // Extract the brewery information from the extras into the string arrays
        breweries = extras.getStringArrayList("nameArr").toArray(new String[totalBreweries]);
        //addresses = extras.getStringArrayList("addressArr").toArray(new String[totalBreweries]);
        //urls = extras.getStringArrayList("urlArr").toArray(new String[totalBreweries]);
        iconAssets = extras.getStringArrayList("iconArr").toArray(new String[totalBreweries]);
        lats = extras.getDoubleArray("latArr");
        longs = extras.getDoubleArray("longArr");
        beers = extras.getStringArrayList("beerArr").toArray(new String[totalBreweries]);

        // Initialize beer style filter drop down spinner
        Spinner s = (Spinner) findViewById(R.id.style_filter);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, arraySpinner);
        s.setAdapter(adapter);
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            /** Spinner listener updates which markers are visible when a beer style is selected. */
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                switch (position) {
                    case 0: for (Marker marker: markers) { marker.setVisible(true); }
                            break;
                    case 1: showOnlyStyle(wheatTerms);
                            break;
                    case 2: showOnlyStyle(lagerTerms);
                            break;
                    case 3: showOnlyStyle(paleTerms);
                            break;
                    case 4: showOnlyStyle(bitterTerms);
                            break;
                    case 5: showOnlyStyle(belgianTerms);
                            break;
                    case 6: showOnlyStyle(amberTerms);
                            break;
                    case 7: showOnlyStyle(brownTerms);
                            break;
                    case 8: showOnlyStyle(porterTerms);
                }
            }

            // Default to showing all markers
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                for (Marker marker: markers) {
                    marker.setVisible(true);
                }
            }
        });

        // Run map setup process
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();

        setUpMapIfNeeded();
    }

    /** Listener method for the my location button, resets the camera to default. */
    public void onResetClick(View v) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latAvg, longAvg), zoom));
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

    /** Method that handles the actual process of setting up all the map options. */
    private void setUpMap() {

        // Set custom info window adapter definition
        mMap.setInfoWindowAdapter(new MyInfoWindowAdapter());

        // Initialize average coordinates
        latAvg = 0.0;
        longAvg = 0.0;

        // Create a marker for each brewery, add beer lists, set coordinates, and set icon
        for (int i = 0; i < totalBreweries; i++) {
            markers[i] = mMap.addMarker(new MarkerOptions()
                    .title(breweries[i])
                    .snippet(beers[i])
                    .position(new LatLng(lats[i], longs[i]))
                    .icon(BitmapDescriptorFactory.fromAsset(iconAssets[i])));

            // Update average coordinate total
            latAvg += lats[i];
            longAvg += longs[i];
        }

        // Divide average total by amount of breweries to get average coordinate
        latAvg = latAvg / totalBreweries;
        longAvg = longAvg / totalBreweries;

        // Set up camera to use the average coordinate as a center and the defined zoom
        mMap.moveCamera( CameraUpdateFactory.newLatLngZoom(new LatLng(latAvg, longAvg), zoom) );

        // Disable two finger rotate gesture, unnecessary for app purposes
        mMap.getUiSettings().setRotateGesturesEnabled(false);
    }

    /** Method that handles showing/hiding the appropriate markers when a style is selected */
    private void showOnlyStyle(String[] termArr) {
        for (Marker marker: markers) {
            // Extract the beer list from the marker
            String snippet = marker.getSnippet();

            // Initialize flag for if a term is found
            boolean containsTerm = false;

            // Check if the beer list contains any of the different terms for the style
            for (String term : termArr) {
                if (snippet.toLowerCase().contains(term.toLowerCase())) {
                    containsTerm = true;
                }
            }

            if (containsTerm) {
                marker.setVisible(true);
            } else {
                marker.setVisible(false);
            }
        }
    }

    // Custom google maps marker info window sub class definition
    class MyInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private final View myContentsView;

        MyInfoWindowAdapter(){
            myContentsView = getLayoutInflater().inflate(R.layout.windowlayout, null);
        }

        @Override
        public View getInfoContents(Marker marker) {

            // Info windows consists of a title and a snippet that wrap content
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
}
