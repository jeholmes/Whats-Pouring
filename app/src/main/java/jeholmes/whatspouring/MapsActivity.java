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

    private static String[] wheatTerms = {"wheat","hefeweizen","wit","blonde"};
    private static String[] lagerTerms = {"lager","pilsner","kolsch","marzen","helles"};
    private static String[] paleTerms = {"pale", "IPA", "india", "I.P.A"};
    private static String[] bitterTerms = {"bitter", "ESB", "E.S.B"};
    private static String[] belgianTerms = {"belgian", "saison", "dubbel", "tripel", "quad", "white"};
    private static String[] amberTerms = {"amber", "ruby"};
    private static String[] brownTerms = {"brown", "altbier", "classic"};
    private static String[] porterTerms = {"porter", "stout"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Bundle extras = getIntent().getExtras();

        totalBreweries = extras.getInt("total");

        markers = new Marker[totalBreweries];

        breweries = extras.getStringArrayList("nameArr").toArray(new String[totalBreweries]);
        //addresses = extras.getStringArrayList("addrArr").toArray(new String[totalBreweries]);
        //urls = extras.getStringArrayList("urlArr").toArray(new String[totalBreweries]);
        //listDOMid = extras.getStringArrayList("listArr").toArray(new String[totalBreweries]);
        //listContainer = extras.getStringArrayList("contArr").toArray(new String[totalBreweries]);
        //listSecondary = extras.getStringArrayList("secArr").toArray(new String[totalBreweries]);
        iconAssets = extras.getStringArrayList("iconArr").toArray(new String[totalBreweries]);
        lats = extras.getDoubleArray("latArr");
        longs = extras.getDoubleArray("longArr");
        beers = extras.getStringArrayList("beerArr").toArray(new String[totalBreweries]);

        String[] arraySpinner = new String[] {
                "All Beer Styles",
                "Wheat Ale",
                "Lager/Pilsner",
                "Pale Ale/IPA",
                "Best Bitter/ESB",
                "Belgian Ale",
                "Amber Ale",
                "Brown Ale",
                "Porter/Stout"
        };

        Spinner s = (Spinner) findViewById(R.id.style_filter);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, arraySpinner);
        s.setAdapter(adapter);
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                switch (position) {
                    case 0:
                        for (Marker marker: markers) {
                            marker.setVisible(true);
                        }
                        break;
                    case 1:
                        showOnlyStyle(wheatTerms);
                        break;
                    case 2:
                        showOnlyStyle(lagerTerms);
                        break;
                    case 3:
                        showOnlyStyle(paleTerms);
                        break;
                    case 4:
                        showOnlyStyle(bitterTerms);
                        break;
                    case 5:
                        showOnlyStyle(belgianTerms);
                        break;
                    case 6:
                        showOnlyStyle(amberTerms);
                        break;
                    case 7:
                        showOnlyStyle(brownTerms);
                        break;
                    case 8:
                        showOnlyStyle(porterTerms);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                for (Marker marker: markers) {
                    marker.setVisible(true);
                }
            }
        });

        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();

        setUpMapIfNeeded();
    }

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

    private Marker[] markers;

    private String[] breweries;
    //private String[] addresses;
    //private String[] urls;
    //private String[] listDOMid;
    //private String[] listContainer;
    //private String[] listSecondary;
    private String[] iconAssets;
    private double[] lats;
    private double[] longs;
    private String[] beers;

    public int totalBreweries;

    double latAvg;
    double longAvg;
    float zoom = 14.5f;

    private void setUpMap() {

        mMap.setInfoWindowAdapter(new MyInfoWindowAdapter());

        latAvg = 0.0;
        longAvg = 0.0;

        for (int i = 0; i < totalBreweries; i++) {
            markers[i] = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(lats[i], longs[i]))
                    .title(breweries[i])
                    .snippet(beers[i])
                    .icon(BitmapDescriptorFactory.fromAsset(iconAssets[i])));
            latAvg += lats[i];
            longAvg += longs[i];
        }
        latAvg = latAvg / totalBreweries;
        longAvg = longAvg / totalBreweries;

        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.moveCamera( CameraUpdateFactory.newLatLngZoom(new LatLng(latAvg, longAvg), zoom) );
    }

    private void showOnlyStyle(String[] termArr) {
        for (Marker marker: markers) {
            String snippet = marker.getSnippet();
            boolean contains = false;
            for (String term : termArr) {
                if (snippet.toLowerCase().contains(term.toLowerCase())) {
                    contains = true;
                }
            }
            if (contains) {
                marker.setVisible(true);
            } else {
                marker.setVisible(false);
            }
        }
    }
}
