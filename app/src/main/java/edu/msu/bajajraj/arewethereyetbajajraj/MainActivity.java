package edu.msu.bajajraj.arewethereyetbajajraj;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActiveListener activeListener = new ActiveListener();

    private LocationManager locationManager = null;
    private SharedPreferences settings = null;

    private final static String TO = "to";
    private final static String TOLAT = "tolat";
    private final static String TOLONG = "tolong";
    private final static String RAD = "torad";

    private double latitude = 0;
    private double longitude = 0;
    private boolean valid = false;

    private double toLatitude = 0;
    private double toLongitude = 0;
    private String to = "";

    String radio = "1";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Force the screen to say on and bright
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // Also, dont forget to add overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //   int[] grantResults)
                // to handle the case where the user grants the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        to = settings.getString(TO, "2250 Engineering");

        toLatitude = Double.parseDouble(settings.getString(TOLAT, "42.724303"));
        toLongitude = Double.parseDouble(settings.getString(TOLONG, "-84.480507"));

        // Get the location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        radio = settings.getString(RAD, "1");
    }

    /**
     * Set all user interface components to the current state
     */
    private void setUI() {
        TextView lat = (TextView) findViewById(R.id.textLatitude);
        TextView log = (TextView) findViewById(R.id.textLongitude);
        TextView edit = (TextView) findViewById(R.id.textTo);
        TextView EditDistance = (TextView) findViewById(R.id.textDistance);

        if (valid) {

            lat.setText(Double.toString(latitude));
            log.setText(Double.toString(longitude));
            edit.setText(to);

            float[] distance = new float[1];
            Location.distanceBetween(latitude, longitude, toLatitude, toLongitude, distance);
            EditDistance.setText(String.format("%1$6.1fm", distance[0]));

            RadioButton walkingButton = (RadioButton) findViewById(R.id.walking_button);
            RadioButton cyclingButton = (RadioButton) findViewById(R.id.cycling_button);
            RadioButton drivingButton = (RadioButton) findViewById(R.id.driving_button);

            if(radio.equals("1")){
                walkingButton.toggle();
            } else if (radio.equals("2")){
                cyclingButton.toggle();

            } else if (radio.equals("3")){
                drivingButton.toggle();
            }
        } else {
            lat.setText("");
            log.setText("");
            edit.setText(to);
            EditDistance.setText("");
            TextView EditProvider = (TextView) findViewById(R.id.textProvider);
            EditProvider.setText("");
        }
    }

    /**
     * Called when this application becomes foreground again.
     */
    @Override
    protected void onResume() {
        super.onResume();

        TextView viewProvider = (TextView) findViewById(R.id.textProvider);
        viewProvider.setText("");

        setUI();
        registerListeners();
    }

    /**
     * Called when this application is no longer the foreground application.
     */
    @Override
    protected void onPause() {
        unregisterListeners();
        super.onPause();
    }

    private void registerListeners() {
        unregisterListeners();

        // Create a Criteria object
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        criteria.setAltitudeRequired(true);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(false);

        String bestAvailable = locationManager.getBestProvider(criteria, true);

        if (bestAvailable != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(bestAvailable, 500, 1, activeListener);
            TextView viewProvider = (TextView)findViewById(R.id.textProvider);
            viewProvider.setText(bestAvailable);
            Location location = locationManager.getLastKnownLocation(bestAvailable);
            onLocation(location);
        }
    }

    private void unregisterListeners() {
        locationManager.removeUpdates(activeListener);
    }

    private void onLocation(Location location) {
        if(location == null) {
            return;
        }

        latitude = location.getLatitude();
        longitude = location.getLongitude();
        valid = true;

        setUI();
    }

    /**
     * Handle an options menu selection
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.itemSparty:
                newTo("Sparty", 42.731138, -84.487508);
                return true;

            case R.id.itemHome:
                newTo("McDonel Hall - Home", 42.7267, -84.4675);
                return true;

            case R.id.item2250:
                newTo("2250 Engineering", 42.724303, -84.480507);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handle setting a new "to" location.
     * @param address Address to display
     * @param lat latitude
     * @param lon longitude
     */
    private void newTo(String address, double lat, double lon) {
        to = address;
        toLatitude = lat;
        toLongitude = lon;

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(TOLAT, Double.toString(toLatitude));
        editor.putString(TOLONG, Double.toString(toLongitude));
        editor.putString(TO, address);

        editor.commit();

        setUI();
    }

    public void onNew(View view) {
        EditText location = (EditText)findViewById(R.id.editLocation);
        final String address = location.getText().toString().trim();
        newAddress(address);
    }

    private void newAddress(final String address) {

        if(address.equals("")) {
            // Don't do anything if the address is blank
            return;
        }

        new Thread(new Runnable() {

            @Override
            public void run() {
                lookupAddress(address);

            }

        }).start();

    }

    /**
     * Look up the provided address. This works in a thread!
     * @param address Address we are looking up
     */
    private void lookupAddress(String address) {
        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.US);
        boolean exception = false;
        List<Address> locations;
        try {
            locations = geocoder.getFromLocationName(address, 1);
        } catch(IOException ex) {
            // Failed due to I/O exception
            locations = null;
            exception = true;
        }

        final String add = address;
        final boolean ex = exception;
        final List<Address> loc = locations;

        this.runOnUiThread(new Runnable() {
            public void run() {
                newLocation(add,ex,loc);
            }
        });
    }

    private void newLocation(String address, boolean exception, List<Address> locations) {

        if(exception) {
            Toast.makeText(MainActivity.this, R.string.exception, Toast.LENGTH_SHORT).show();
        } else {
            if(locations == null || locations.size() == 0) {
                Toast.makeText(this, R.string.couldnotfind, Toast.LENGTH_SHORT).show();
                return;
            }

            EditText location = (EditText)findViewById(R.id.editLocation);
            location.setText("");

            // We have a valid new location
            Address a = locations.get(0);
            newTo(address, a.getLatitude(), a.getLongitude());

            to = address;

        }
    }

    public void openMap(View view) {
        RadioButton walkingButton = (RadioButton) findViewById(R.id.walking_button);
        RadioButton cyclingButton = (RadioButton) findViewById(R.id.cycling_button);
        RadioButton drivingButton = (RadioButton) findViewById(R.id.driving_button);

        if (walkingButton.isChecked()){
            displayGoogleMap("&mode=w");
        }
        else if(cyclingButton.isChecked()){
            displayGoogleMap("&mode=b");

        }
        else if(drivingButton.isChecked()){
            displayGoogleMap("&mode=d");
        }
    }

    private void displayGoogleMap(String way){
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + Double.toString(toLatitude)
                + "," + Double.toString(toLongitude) + way);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    public void onMode(View view){
        boolean checked = ((RadioButton) view).isChecked();
        SharedPreferences.Editor editor = settings.edit();

        if (view.getId() == R.id.walking_button && checked){
            radio = "1";
        } else if (view.getId() == R.id.cycling_button && checked){
            radio = "2";
        } else if (view.getId() == R.id.driving_button && checked){
            radio = "3";
        }
        editor.putString( RAD , radio);
        editor.commit();
    }

    private class ActiveListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            onLocation(location);
        }

        @Override
        public void onStatusChanged(String s, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {
            registerListeners();
        }
    };
}