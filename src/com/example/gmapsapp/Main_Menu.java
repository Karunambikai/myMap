package com.example.gmapsapp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
public class Main_Menu extends Activity implements OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.menu);
		findViewById(R.id.btnCheckIn).setOnClickListener(this);
		findViewById(R.id.btnMenu).setOnClickListener(this);
		findViewById(R.id.btnHow).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnCheckIn:
			startActivity(new Intent(getApplicationContext(),
					MainActivity.class));
			break;
		case R.id.btnMenu:
			startActivity(new Intent(getApplicationContext(),
					MapMenuActivity.class));
			break;
		case R.id.btnHow:
			startActivity(new Intent(getApplicationContext(),
					How_To_Use.class));
			break;
		}
	}
}
