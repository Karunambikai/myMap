package com.example.gmapsapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.gmapsapp.db.DataSource;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MapMenuActivity extends FragmentActivity implements
		OnItemSelectedListener, OnClickListener, OnItemClickListener,
		OnMarkerClickListener {

	// DB
	DataSource ds;

	Spinner type;
	String check_In_Type = null;
	ListView listView;
	double lat, lng;
	String gotoLocation = null;
	StringBuilder loc;
	MarkerOptions markerOptions;
	private static final int GPS_ERRORDIALOG_REQUEST = 9001;
	@SuppressWarnings("unused")
	private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9002;
	GoogleMap mMap;
	List<Check_In_Item> list;
	@SuppressWarnings("unused")
	private static final float DEFAULTZOOM = 15;
	@SuppressWarnings("unused")
	private static final String LOGTAG = "Maps";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (servicesOK()) {
			setContentView(R.layout.activity_my_map);
			loc = new StringBuilder();

			if (initMap()) {
				boolean isNet = false;
				ConnectivityManager connectivity = (ConnectivityManager) getApplicationContext()
						.getSystemService(Context.CONNECTIVITY_SERVICE);
				if (connectivity != null) {
					NetworkInfo[] info = connectivity.getAllNetworkInfo();
					if (info != null)
						for (int i = 0; i < info.length; i++)
							if (info[i].getState() == NetworkInfo.State.CONNECTED) {
								isNet = true;
								break;
							}
				}
				if (!isNet) {
					AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
							this);
					alertDialogBuilder
							.setMessage(
									"No Internet Connection. Please connect internet and try again!!")
							.setCancelable(false)
							.setPositiveButton("Okay",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {

										}
									});
					AlertDialog alert = alertDialogBuilder.create();
					alert.show();
				}
				LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
				if (!locationManager
						.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					showGPSDisabledAlertToUser();
				}

				findViewById(R.id.my_buttonList).setOnClickListener(this);
				findViewById(R.id.my_buttonMap).setOnClickListener(this);
				findViewById(R.id.my_buttonMap).setEnabled(false);
				findViewById(R.id.my_buttonList).setEnabled(true);
				findViewById(R.id.my_buttonList).setAlpha(1f);
				findViewById(R.id.my_buttonMap).setAlpha(0.5f);

				listView = (ListView) findViewById(R.id.listView1);

				type = (Spinner) findViewById(R.id.SpinnerType);
				List<String> types = new ArrayList<String>();
				types.add("All");
				types.add("General");
				types.add("Business");
				types.add("Education");
				types.add("Health");
				types.add("Tour");
				ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(
						this, android.R.layout.simple_spinner_item, types);
				dataAdapter
						.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				type.setAdapter(dataAdapter);
				type.setOnItemSelectedListener(this);

				ds = new DataSource(this);
				ds.open();
				mMap.setMyLocationEnabled(true);
				list = ds.List_All_Check_Ins(check_In_Type);
				Load_Map_Data();
				mMap.setOnMarkerClickListener(this);
			} else {
				Toast.makeText(this, "Map not available!", Toast.LENGTH_SHORT)
						.show();
			}
			listView.setOnItemClickListener(this);
		} else {
			setContentView(R.layout.activity_main);
		}
	}

	public void Load_Map_Data() {
		for (int i = 0; i < list.size(); i++) {
			Check_In_Item item = list.get(i);
			MarkerOptions marker = new MarkerOptions().position(
					new LatLng(Double.parseDouble(item.getLat()), Double
							.parseDouble(item.getLng())))
					.title(item.getTitle());
			marker.icon(BitmapDescriptorFactory
					.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
			mMap.addMarker(marker);
			CameraPosition cameraPosition = new CameraPosition.Builder()
					.target(new LatLng(Double.parseDouble(item.getLat()),
							Double.parseDouble(item.getLng()))).zoom(5).build();
			mMap.animateCamera(CameraUpdateFactory
					.newCameraPosition(cameraPosition));
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
					.findFragmentById(R.id.my_map);
			mMap = mapFrag.getMap();
		}
		return (mMap != null);
	}

	@SuppressWarnings("unused")
	private void gotoLocation(double lat, double lng) {
		LatLng ll = new LatLng(lat, lng);
		CameraUpdate update = CameraUpdateFactory.newLatLng(ll);
		mMap.moveCamera(update);
	}

	private void gotoLocation(double lat, double lng, float zoom,
			String gotoLocation) {
		this.gotoLocation = gotoLocation;
		LatLng ll = new LatLng(lat, lng);
		CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, zoom);
		mMap.moveCamera(update);
		CameraPosition cameraPosition = new CameraPosition.Builder()
				.target(new LatLng(lat, lng)).zoom(15).build();
		mMap.animateCamera(CameraUpdateFactory
				.newCameraPosition(cameraPosition));
	}

	public void geoLocate(View v) throws IOException {
		hideSoftKeyboard(v);

		EditText et = (EditText) findViewById(R.id.editText1);
		String location = et.getText().toString();

		Geocoder gc = new Geocoder(this);
		List<Address> list = gc.getFromLocationName(location, 3);
		loc = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			Address address = (Address) list.get(i);
			loc.append(
					String.format(
							"%s, %s",
							address.getMaxAddressLineIndex() > 0 ? address
									.getAddressLine(0) : "", address
									.getCountryName())).append("\n");
		}
		for (int i = 0; i < list.size(); i++) {
			Address address = (Address) list.get(i);
			String addressText = String.format("%s, %s", address
					.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0)
					: "", address.getCountryName());
			lat = address.getLatitude();
			lng = address.getLongitude();
			if (i == 0)
				gotoLocation(lat, lng, DEFAULTZOOM, addressText);
		}
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
			gotoCurrentLocation();
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
		loc = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			Address address = (Address) list.get(i);
			loc.append(
					String.format(
							"%s, %s",
							address.getMaxAddressLineIndex() > 0 ? address
									.getAddressLine(0) : "", address
									.getCountryName())).append("\n");
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
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		check_In_Type = parent.getItemAtPosition(position).toString();
		list = ds.List_All_Check_Ins(check_In_Type);
		List<String> titles = new ArrayList<String>();
		for (int i = 0; i < list.size(); i++) {
			Check_In_Item item = list.get(i);
			titles.add(item.getTitle());
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, android.R.id.text1, titles);
		listView.setAdapter(adapter);
		mMap.clear();
		Load_Map_Data();
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		check_In_Type = parent.getItemAtPosition(0).toString();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.my_buttonList:
			listView.setVisibility(View.VISIBLE);
			findViewById(R.id.listViewLay).setVisibility(View.VISIBLE);

			findViewById(R.id.my_searchLay).setVisibility(View.GONE);
			findViewById(R.id.my_map).setVisibility(View.GONE);

			findViewById(R.id.my_buttonMap).setEnabled(true);
			findViewById(R.id.my_buttonList).setEnabled(false);
			findViewById(R.id.my_buttonList).setAlpha(0.5f);
			findViewById(R.id.my_buttonMap).setAlpha(1f);

			List<String> titles = new ArrayList<String>();
			for (int i = 0; i < list.size(); i++) {
				Check_In_Item item = list.get(i);
				titles.add(item.getTitle());
			}
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1, android.R.id.text1,
					titles);
			listView.setAdapter(adapter);
			break;
		case R.id.my_buttonMap:
			findViewById(R.id.my_searchLay).setVisibility(View.VISIBLE);
			findViewById(R.id.my_map).setVisibility(View.VISIBLE);
			findViewById(R.id.my_buttonMap).setEnabled(false);
			findViewById(R.id.my_buttonMap).setAlpha(0.5f);

			findViewById(R.id.my_buttonList).setEnabled(true);
			findViewById(R.id.my_buttonList).setAlpha(1f);
			listView.setVisibility(View.GONE);
			findViewById(R.id.listViewLay).setVisibility(View.GONE);
			break;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Toast.makeText(getApplicationContext(),
				parent.getItemAtPosition(position).toString(),
				Toast.LENGTH_LONG).show();
		Intent i = new Intent(getApplicationContext(), Check_In_Activity.class);
		i.putExtra("title", parent.getItemAtPosition(position).toString());
		startActivity(i);
		finish();
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		Toast.makeText(getApplicationContext(), marker.getTitle().toString(),
				Toast.LENGTH_LONG).show();
		Intent i = new Intent(getApplicationContext(), Check_In_Activity.class);
		i.putExtra("title", marker.getTitle().toString());
		startActivity(i);
		return false;
	}

	private void showGPSDisabledAlertToUser() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder
				.setMessage(
						"GPS is disabled in your device. Would you like to enable it?")
				.setCancelable(false)
				.setPositiveButton("Goto Settings Page To Enable GPS",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Intent callGPSSettingIntent = new Intent(
										android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
								startActivity(callGPSSettingIntent);
							}
						});
		alertDialogBuilder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = alertDialogBuilder.create();
		alert.show();
	}

}
