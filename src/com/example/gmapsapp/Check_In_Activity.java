package com.example.gmapsapp;

import java.io.File;
import java.io.IOException;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gmapsapp.db.DataSource;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
public class Check_In_Activity extends FragmentActivity {

	String title, recording;
	DataSource ds;
	ListView listView;
	double lat, lng;
	MarkerOptions markerOptions;
	int height, width;
	GoogleMap mMap;
	TextView ci_title, ci_location, ci_type, ci_description, ci_date;
	LinearLayout ci_images;
	MediaPlayer myPlayer;
	private static final float DEFAULTZOOM = 15;
	private static final int GPS_ERRORDIALOG_REQUEST = 9001;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (servicesOK()) {
			setContentView(R.layout.activity_check_in_detail);
			if (initMap()) {
				ds = new DataSource(this);
				ds.open();
				Bundle b = getIntent().getExtras();
				title = b.getString("title");
				final Check_In_Item item = ds.getItem(title);
				ci_title = (TextView) findViewById(R.id.ci_title);
				ci_location = (TextView) findViewById(R.id.ci_location);
				ci_description = (TextView) findViewById(R.id.ci_description);
				ci_type = (TextView) findViewById(R.id.ci_type);
				ci_date = (TextView) findViewById(R.id.ci_date);
				ci_images = (LinearLayout) findViewById(R.id.ci_images);

				ci_title.setText(item.getTitle());
				ci_location.setText(item.getLocation());
				ci_description.setText(item.getDescription());
				ci_type.setText(item.getType());
				ci_date.setText(item.getDate());
				String fontPath = "fonts/gooddog.otf";
				Typeface tf = Typeface.createFromAsset(getAssets(), fontPath);
				ci_description.setTypeface(tf);

				Display display = getWindowManager().getDefaultDisplay();
				Point size = new Point();
				display.getSize(size);
				width = size.x;
				height = size.y;
				List<String> img_Path = ds.List_Images(title);
				for (int i = 0; i < img_Path.size(); i++) {
					File imgFile = new File(img_Path.get(i));
					if (imgFile.exists()) {
						Bitmap original = BitmapFactory.decodeFile(imgFile
								.getAbsolutePath());
						ImageView iv = new ImageView(getApplicationContext());
						iv.setImageBitmap(Bitmap.createScaledBitmap(original,
								height / 2, height / 2, true));
						ci_images.addView(iv);
					}
				}

				findViewById(R.id.ci_play).setOnClickListener(
						new OnClickListener() {
							@Override
							public void onClick(View v) {

								if (item.getRecord().length() > 0) {
									File recFile = new File(item.getRecord());
									Toast.makeText(getApplicationContext(),
											recFile.getAbsolutePath(),
											Toast.LENGTH_LONG).show();
									myPlayer = new MediaPlayer();
									try {
										myPlayer.setDataSource(item.getRecord());
										myPlayer.prepare();
										myPlayer.start();
									} catch (IllegalArgumentException e) {
										e.printStackTrace();
									} catch (SecurityException e) {
										e.printStackTrace();
									} catch (IllegalStateException e) {
										e.printStackTrace();
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							}
						});
				ci_images.getLayoutParams().height = height / 2;

				mMap.setMyLocationEnabled(true);
				gotoLocation(Double.parseDouble(item.getLat()),
						Double.parseDouble(item.getLng()), DEFAULTZOOM,
						item.getLocation());
				findViewById(R.id.map_frag).getLayoutParams().height = (int) (height / 2.5);
			} else {
				Toast.makeText(this, "Map not available!", Toast.LENGTH_SHORT)
						.show();
			}
		} else {
			setContentView(R.layout.activity_main);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public boolean servicesOK() {
		int isAvailable = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (isAvailable == ConnectionResult.SUCCESS) {
			return true;
		} else if (GooglePlayServicesUtil.isUserRecoverableError(isAvailable)) {
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(isAvailable,
					this, GPS_ERRORDIALOG_REQUEST);
			dialog.show();
		} else {
			Toast.makeText(this, "Can't connect to Google Play services",
					Toast.LENGTH_SHORT).show();
		}
		return false;
	}

	private boolean initMap() {
		if (mMap == null) {
			SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.ci_map);
			mMap = mapFrag.getMap();
		}
		return (mMap != null);
	}

	private void gotoLocation(double lat, double lng, float zoom,
			String gotoLocation) {
		if (mMap != null)
			mMap.clear();
		LatLng ll = new LatLng(lat, lng);
		CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, zoom);
		mMap.moveCamera(update);
		markerOptions = new MarkerOptions().position(new LatLng(lat, lng))
				.title(gotoLocation);
		markerOptions.icon(BitmapDescriptorFactory
				.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
		mMap.addMarker(markerOptions);
		CameraPosition cameraPosition = new CameraPosition.Builder()
				.target(new LatLng(lat, lng)).zoom(zoom).build();
		mMap.animateCamera(CameraUpdateFactory
				.newCameraPosition(cameraPosition));
	}

	private void hideSoftKeyboard(View v) {
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.mapTypeNone:
			mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
			break;
		case R.id.mapTypeNormal:
			mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
			break;
		case R.id.mapTypeSatellite:
			mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
			break;
		case R.id.mapTypeTerrain:
			mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
			break;
		case R.id.mapTypeHybrid:
			mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
			break;
		case R.id.gotoCurrentLocation:
			// gotoCurrentLocation();
			break;
		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStop() {
		super.onStop();
		MapStateManager mgr = new MapStateManager(this);
		mgr.saveMapState(mMap);
	}

	@Override
	protected void onResume() {
		super.onResume();
		MapStateManager mgr = new MapStateManager(this);
		CameraPosition position = mgr.getSavedCameraPosition();
		if (position != null) {
			CameraUpdate update = CameraUpdateFactory
					.newCameraPosition(position);
			mMap.moveCamera(update);
			mMap.setMapType(mgr.getSavedMapType());
		}
	}

	protected void gotoCurrentLocation() {
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Location location = lm
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10,
				locationListener);
		lng = location.getLongitude();
		lat = location.getLatitude();
		Geocoder gc = new Geocoder(this);
		List<Address> list = null;
		try {
			list = gc.getFromLocation(lat, lng, 3);
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < list.size(); i++) {
			Address address = (Address) list.get(i);
			String addressText = String.format("%s, %s", address
					.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0)
					: "", address.getCountryName());
			if (i == 0)
				gotoLocation(lat, lng, DEFAULTZOOM, addressText);
		}
	}

	private final LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			lng = location.getLongitude();
			lat = location.getLatitude();
		}

		@Override
		public void onProviderDisabled(String arg0) {
		}

		@Override
		public void onProviderEnabled(String arg0) {
		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		}
	};

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		if (myPlayer != null)
			myPlayer.stop();
	}

}
