package com.psarmmiey.placesViewer;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;


public class MainActivity extends AppCompatActivity
	implements GoogleApiClient.ConnectionCallbacks,
	           GoogleApiClient.OnConnectionFailedListener {

	private final int PERMISSION_ACCESS_COARSE_LOCATION = 0;

	// List of weather objects representing the forecast
	private final List<Weather> weatherList = new ArrayList<>();
	private GoogleApiClient mGoogleApiClient;

	private double mLong;
	private double mLat;
	private double finalLat;
	private double finalLong;


	// ArrayAdapter for binding weather objects to a ListView
	private WeatherArrayAdapter weatherArrayAdapter;
	private ListView weatherListView; // displays weather info

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// autoGenerated code to inflate layout and configure Toolbar
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);


		// Create an instance of GoogleAPIClient.
		checkNetworkConnection();
		if(mGoogleApiClient == null) {
			mGoogleApiClient = new GoogleApiClient.Builder(this)
				                   .addConnectionCallbacks(this)
				                   .addOnConnectionFailedListener(this)
				                   .addApi(LocationServices.API)
				                   .build();
		}

		if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
			   != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
				new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
				PERMISSION_ACCESS_COARSE_LOCATION);
		}


		// create ArrayAdapter to bind weatherList to the weatherListView
		weatherListView = (ListView) findViewById(R.id.weatherListView);
		weatherArrayAdapter = new WeatherArrayAdapter(this, weatherList);
		weatherListView.setAdapter(weatherArrayAdapter);


		final ProgressBar loadingSpin = (ProgressBar) findViewById(R.id.loadingBar);

		EditText locationEditText = (EditText) findViewById(R.id.locationEditText);
		locationEditText.setOnClickListener(
			new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
					CardView quickCard = (CardView) findViewById(R.id.quickSearchCard);
					fab.setVisibility(View.VISIBLE);

					quickCard.setVisibility(View.VISIBLE);
				}
			}
		);

		locationEditText.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View view, int i, KeyEvent keyEvent) {
				EditText locationEditText =
					(EditText) findViewById(R.id.locationEditText);
				if(keyEvent.getAction() == android.view.KeyEvent.KEYCODE_ENTER) {
					URL url = createURL(locationEditText.getText().toString());
					if(url != null) {
						dismissKeyboard(locationEditText);
						GetPlaceTask getLocalWeatherTask = new GetPlaceTask();
						getLocalWeatherTask.execute(url);
					} else {
						Snackbar.make(findViewById(R.id.coordinatorLayout),
							R.string.invalid_url, Snackbar.LENGTH_LONG).show();
					}
				}
				return false;
			}
		});

		FloatingActionButton churchFab = (FloatingActionButton) findViewById(R.id.church_fab);
		FloatingActionButton mosque = (FloatingActionButton) findViewById(R.id.mosque_fab);
		FloatingActionButton postOffice = (FloatingActionButton) findViewById(R.id.post_office_fab);
		FloatingActionButton petrolStation = (FloatingActionButton) findViewById(R.id.petrol_station_fab);
		FloatingActionButton supermarket = (FloatingActionButton) findViewById(R.id.supermarkets_fab);
		FloatingActionButton hotel = (FloatingActionButton) findViewById(R.id.hotel_fab);
		FloatingActionButton pharmacy = (FloatingActionButton) findViewById(R.id.pharmacy_fab);

		//FloatingActionButton hospital = (FloatingActionButton) findViewById(R.id.hospitalFab);
		TextView churchText = (TextView) findViewById(R.id.church_textView);
		TextView mosqueText = (TextView) findViewById(R.id.mosque_textView);
		TextView postOfficeText = (TextView) findViewById(R.id.post_office_textView);
		TextView petrolStationText = (TextView) findViewById(R.id.petrol_stations_textView);
		TextView supermarketText = (TextView) findViewById(R.id.supermarket_textView);
		TextView hotelText = (TextView) findViewById(R.id.hotel_textView);
		TextView pharmacyText = (TextView) findViewById(R.id.chemst_textView);
		TextView moreLess = (TextView) findViewById(R.id.more_Less_textView);


		pharmacy.setVisibility(View.GONE);
		pharmacyText.setVisibility(GONE);
		churchFab.setVisibility(View.GONE);
		churchText.setVisibility(View.GONE);
		supermarket.setVisibility(View.GONE);
		supermarketText.setVisibility(View.GONE);
		hotel.setVisibility(View.GONE);
		hotelText.setVisibility(View.GONE);
		petrolStation.setVisibility(View.GONE);
		petrolStationText.setVisibility(View.GONE);
		mosque.setVisibility(View.GONE);
		mosqueText.setVisibility(View.GONE);
		postOffice.setVisibility(View.GONE);
		postOfficeText.setVisibility(View.GONE);
		moreLess.setVisibility(View.GONE);

		// CardView quickCard = (CardView) findViewById(R.id.quickSearchCard);
		FloatingActionButton lessFab = (FloatingActionButton) findViewById(R.id.more_less_fab);
		lessFab.setVisibility(GONE);


		// Search Fab
		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// get text from locationEditText and create web service URL

				//	FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
				CardView quickCard = (CardView) findViewById(R.id.quickSearchCard);
				// fab.setVisibility(View.GONE);
				quickCard.setVisibility(GONE);
				quickCard.removeView(quickCard);
				EditText locationEditText =
					(EditText) findViewById(R.id.locationEditText);
				String trim = locationEditText.getText().toString().replaceAll(" ", "_").trim();
				locationEditText.setText(trim);
				URL url = createURL(trim);


				// hide keyboard and initiate a GetPlaceTask to download
				// weather data from OpenWeatherMap.org in a separate thread
				if(url != null) {
					dismissKeyboard(locationEditText);

					loadingSpin.setVisibility(View.VISIBLE);

					GetPlaceTask getLocalWeatherTask = new GetPlaceTask();
					getLocalWeatherTask.execute(url);
				} else {
					Snackbar.make(findViewById(R.id.coordinatorLayout),
						R.string.invalid_url, Snackbar.LENGTH_LONG).show();
				}
			}
		});

		// Restaurant Fab
		FloatingActionButton restaurantFab = (FloatingActionButton) findViewById(R.id.restaurantFab);
		restaurantFab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// get text from locationEditText and create web service URL
				CardView quickCard = (CardView) findViewById(R.id.quickSearchCard);
				quickCard.setVisibility(GONE);
				quickCard.removeView(quickCard);
				EditText locationEditText =
					(EditText) findViewById(R.id.locationEditText);
				locationEditText.setText(R.string.restaurant);

				URL url = createURL(locationEditText.getText().toString().trim());

				// hide keyboard and initiate a GetPlaceTask to download
				// weather data from OpenWeatherMap.org in a separate thread
				if(url != null) {
					dismissKeyboard(locationEditText);

					loadingSpin.setVisibility(View.VISIBLE);

					GetPlaceTask getLocalWeatherTask = new GetPlaceTask();
					getLocalWeatherTask.execute(url);
				} else {
					Snackbar.make(findViewById(R.id.coordinatorLayout),
						R.string.invalid_url, Snackbar.LENGTH_LONG).show();
				}
			}
		});

		// Bank Fab
		FloatingActionButton bankFab = (FloatingActionButton) findViewById(R.id.bankFab);
		bankFab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// get text from locationEditText and create web service URL
				CardView quickCard = (CardView) findViewById(R.id.quickSearchCard);
				quickCard.setVisibility(GONE);
				quickCard.removeView(quickCard);
				EditText locationEditText =
					(EditText) findViewById(R.id.locationEditText);
				locationEditText.setText(R.string.bank);
				URL url = createURL(locationEditText.getText().toString());

				// hide keyboard and initiate a GetPlaceTask to download
				// weather data from OpenWeatherMap.org in a separate thread
				if(url != null) {
					dismissKeyboard(locationEditText);

					loadingSpin.setVisibility(View.VISIBLE);

					GetPlaceTask getLocalWeatherTask = new GetPlaceTask();
					getLocalWeatherTask.execute(url);
				} else {
					Snackbar.make(findViewById(R.id.coordinatorLayout),
						R.string.invalid_url, Snackbar.LENGTH_LONG).show();
				}
			}
		});

		// hospital Fab
		final FloatingActionButton hospitalFab = (FloatingActionButton) findViewById(R.id.hospitalFab);
		hospitalFab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// get text from locationEditText and create web service URL
				CardView quickCard = (CardView) findViewById(R.id.quickSearchCard);
				quickCard.setVisibility(GONE);
				quickCard.removeView(quickCard);
				EditText locationEditText =
					(EditText) findViewById(R.id.locationEditText);
				locationEditText.setText(R.string.hospital);
				URL url = createURL(locationEditText.getText().toString());

				// hide keyboard and initiate a GetPlaceTask to download
				// weather data from OpenWeatherMap.org in a separate thread
				if(url != null) {
					dismissKeyboard(locationEditText);

					loadingSpin.setVisibility(View.VISIBLE);

					GetPlaceTask getLocalWeatherTask = new GetPlaceTask();
					getLocalWeatherTask.execute(url);
				} else {
					Snackbar.make(findViewById(R.id.coordinatorLayout),
						R.string.invalid_url, Snackbar.LENGTH_LONG).show();
				}
			}
		});

		// School Fab
		FloatingActionButton schoolFab = (FloatingActionButton) findViewById(R.id.schoolFab);
		schoolFab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// get text from locationEditText and create web service URL
				CardView quickCard = (CardView) findViewById(R.id.quickSearchCard);
				quickCard.setVisibility(GONE);
				quickCard.removeView(quickCard);
				EditText locationEditText =
					(EditText) findViewById(R.id.locationEditText);
				locationEditText.setText(R.string.school);
				URL url = createURL(locationEditText.getText().toString());

				// hide keyboard and initiate a GetPlaceTask to download
				// weather data from OpenWeatherMap.org in a separate thread
				if(url != null) {
					dismissKeyboard(locationEditText);
					loadingSpin.setVisibility(View.VISIBLE);
					GetPlaceTask getLocalWeatherTask = new GetPlaceTask();
					getLocalWeatherTask.execute(url);
				} else {
					Snackbar.make(findViewById(R.id.coordinatorLayout),
						R.string.invalid_url, Snackbar.LENGTH_LONG).show();
				}
			}
		});

		churchFab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// get text from locationEditText and create web service URL
				CardView quickCard = (CardView) findViewById(R.id.quickSearchCard);
				quickCard.setVisibility(GONE);
				quickCard.removeView(quickCard);
				EditText locationEditText =
					(EditText) findViewById(R.id.locationEditText);
				locationEditText.setText(R.string.church);
				URL url = createURL(locationEditText.getText().toString());

				// hide keyboard and initiate a GetPlaceTask to download
				// weather data from OpenWeatherMap.org in a separate thread
				if(url != null) {
					dismissKeyboard(locationEditText);
					loadingSpin.setVisibility(View.VISIBLE);
					GetPlaceTask getLocalWeatherTask = new GetPlaceTask();
					getLocalWeatherTask.execute(url);
				} else {
					Snackbar.make(findViewById(R.id.coordinatorLayout),
						R.string.invalid_url, Snackbar.LENGTH_LONG).show();
				}
			}
		});

		mosque.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// get text from locationEditText and create web service URL
				CardView quickCard = (CardView) findViewById(R.id.quickSearchCard);
				quickCard.setVisibility(GONE);
				quickCard.removeView(quickCard);
				EditText locationEditText =
					(EditText) findViewById(R.id.locationEditText);
				locationEditText.setText(R.string.mosque);
				URL url = createURL(locationEditText.getText().toString());

				// hide keyboard and initiate a GetPlaceTask to download
				// weather data from OpenWeatherMap.org in a separate thread
				if(url != null) {
					dismissKeyboard(locationEditText);
					loadingSpin.setVisibility(View.VISIBLE);
					GetPlaceTask getLocalWeatherTask = new GetPlaceTask();
					getLocalWeatherTask.execute(url);
				} else {
					Snackbar.make(findViewById(R.id.coordinatorLayout),
						R.string.invalid_url, Snackbar.LENGTH_LONG).show();
				}
			}
		});

		postOffice.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// get text from locationEditText and create web service URL
				CardView quickCard = (CardView) findViewById(R.id.quickSearchCard);
				quickCard.setVisibility(GONE);
				quickCard.removeView(quickCard);
				EditText locationEditText =
					(EditText) findViewById(R.id.locationEditText);
				locationEditText.setText(R.string.post_office);
				URL url = createURL(locationEditText.getText().toString());

				// hide keyboard and initiate a GetPlaceTask to download
				// weather data from OpenWeatherMap.org in a separate thread
				if(url != null) {
					dismissKeyboard(locationEditText);
					loadingSpin.setVisibility(View.VISIBLE);
					GetPlaceTask getLocalWeatherTask = new GetPlaceTask();
					getLocalWeatherTask.execute(url);
				} else {
					Snackbar.make(findViewById(R.id.coordinatorLayout),
						R.string.invalid_url, Snackbar.LENGTH_LONG).show();
				}
			}
		});

		petrolStation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// get text from locationEditText and create web service URL
				CardView quickCard = (CardView) findViewById(R.id.quickSearchCard);
				quickCard.setVisibility(GONE);
				quickCard.removeView(quickCard);
				EditText locationEditText =
					(EditText) findViewById(R.id.locationEditText);
				locationEditText.setText(R.string.petrol_station);
				URL url = createURL(locationEditText.getText().toString());

				// hide keyboard and initiate a GetPlaceTask to download
				// weather data from OpenWeatherMap.org in a separate thread
				if(url != null) {
					dismissKeyboard(locationEditText);
					loadingSpin.setVisibility(View.VISIBLE);
					GetPlaceTask getLocalWeatherTask = new GetPlaceTask();
					getLocalWeatherTask.execute(url);
				} else {
					Snackbar.make(findViewById(R.id.coordinatorLayout),
						R.string.invalid_url, Snackbar.LENGTH_LONG).show();
				}
			}
		});
		supermarket.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// get text from locationEditText and create web service URL
				CardView quickCard = (CardView) findViewById(R.id.quickSearchCard);
				quickCard.setVisibility(GONE);
				quickCard.removeView(quickCard);
				EditText locationEditText =
					(EditText) findViewById(R.id.locationEditText);
				locationEditText.setText(R.string.supermarket);
				URL url = createURL(locationEditText.getText().toString());

				// hide keyboard and initiate a GetPlaceTask to download
				// weather data from OpenWeatherMap.org in a separate thread
				if(url != null) {
					dismissKeyboard(locationEditText);
					loadingSpin.setVisibility(View.VISIBLE);
					GetPlaceTask getLocalWeatherTask = new GetPlaceTask();
					getLocalWeatherTask.execute(url);
				} else {
					Snackbar.make(findViewById(R.id.coordinatorLayout),
						R.string.invalid_url, Snackbar.LENGTH_LONG).show();
				}
			}
		});

		hotel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// get text from locationEditText and create web service URL
				CardView quickCard = (CardView) findViewById(R.id.quickSearchCard);
				quickCard.setVisibility(GONE);
				quickCard.removeView(quickCard);
				EditText locationEditText =
					(EditText) findViewById(R.id.locationEditText);
				locationEditText.setText(R.string.hotels);
				URL url = createURL(locationEditText.getText().toString());

				// hide keyboard and initiate a GetPlaceTask to download
				// weather data from OpenWeatherMap.org in a separate thread
				if(url != null) {
					dismissKeyboard(locationEditText);
					loadingSpin.setVisibility(View.VISIBLE);
					GetPlaceTask getLocalWeatherTask = new GetPlaceTask();
					getLocalWeatherTask.execute(url);
				} else {
					Snackbar.make(findViewById(R.id.coordinatorLayout),
						R.string.invalid_url, Snackbar.LENGTH_LONG).show();
				}
			}
		});

		pharmacy.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// get text from locationEditText and create web service URL
				CardView quickCard = (CardView) findViewById(R.id.quickSearchCard);
				quickCard.setVisibility(GONE);
				quickCard.removeView(quickCard);
				EditText locationEditText =
					(EditText) findViewById(R.id.locationEditText);
				locationEditText.setText(R.string.pharmacy);
				URL url = createURL(locationEditText.getText().toString());

				// hide keyboard and initiate a GetPlaceTask to download
				// weather data from OpenWeatherMap.org in a separate thread
				if(url != null) {
					dismissKeyboard(locationEditText);
					loadingSpin.setVisibility(View.VISIBLE);
					GetPlaceTask getLocalWeatherTask = new GetPlaceTask();
					getLocalWeatherTask.execute(url);
				} else {
					Snackbar.make(findViewById(R.id.coordinatorLayout),
						R.string.invalid_url, Snackbar.LENGTH_LONG).show();
				}
			}
		});


		FloatingActionButton less_Fab = (FloatingActionButton) findViewById(R.id.less_fab);
		less_Fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				FloatingActionButton churchFab = (FloatingActionButton) findViewById(R.id.church_fab);
				FloatingActionButton mosque = (FloatingActionButton) findViewById(R.id.mosque_fab);
				FloatingActionButton postOffice = (FloatingActionButton) findViewById(R.id.post_office_fab);
				FloatingActionButton petrolStation = (FloatingActionButton) findViewById(R.id.petrol_station_fab);
				FloatingActionButton supermarket = (FloatingActionButton) findViewById(R.id.supermarkets_fab);
				FloatingActionButton hotel = (FloatingActionButton) findViewById(R.id.hotel_fab);
				FloatingActionButton pharmacy = (FloatingActionButton) findViewById(R.id.pharmacy_fab);
				FloatingActionButton hospitalFab = (FloatingActionButton) findViewById(R.id.hospitalFab);
				FloatingActionButton less_Fab = (FloatingActionButton) findViewById(R.id.less_fab);

				//FloatingActionButton hospital = (FloatingActionButton) findViewById(R.id.hospitalFab);
				TextView churchText = (TextView) findViewById(R.id.church_textView);
				TextView mosqueText = (TextView) findViewById(R.id.mosque_textView);
				TextView postOfficeText = (TextView) findViewById(R.id.post_office_textView);
				TextView petrolStationText = (TextView) findViewById(R.id.petrol_stations_textView);
				TextView supermarketText = (TextView) findViewById(R.id.supermarket_textView);
				TextView hotelText = (TextView) findViewById(R.id.hotel_textView);
				TextView pharmacyText = (TextView) findViewById(R.id.chemst_textView);
				TextView moreLess = (TextView) findViewById(R.id.more_Less_textView);
				TextView hospitalText = (TextView) findViewById(R.id.hospital_textView);
				TextView lessText = (TextView) findViewById(R.id.less_textView);


				less_Fab.setVisibility(GONE);
				lessText.setVisibility(GONE);
				hospitalFab.setVisibility(View.VISIBLE);
				hospitalText.setVisibility(View.VISIBLE);
				pharmacy.setVisibility(View.VISIBLE);
				pharmacyText.setVisibility(View.VISIBLE);
				churchFab.setVisibility(View.VISIBLE);
				churchText.setVisibility(View.VISIBLE);
				supermarket.setVisibility(View.VISIBLE);
				supermarketText.setVisibility(View.VISIBLE);
				hotel.setVisibility(View.VISIBLE);
				hotelText.setVisibility(View.VISIBLE);
				petrolStation.setVisibility(View.VISIBLE);
				petrolStationText.setVisibility(View.VISIBLE);
				mosque.setVisibility(View.VISIBLE);
				mosqueText.setVisibility(View.VISIBLE);
				postOffice.setVisibility(View.VISIBLE);
				postOfficeText.setVisibility(View.VISIBLE);
				moreLess.setVisibility(View.VISIBLE);

				// CardView quickCard = (CardView) findViewById(R.id.quickSearchCard);
				FloatingActionButton lessFab = (FloatingActionButton) findViewById(R.id.more_less_fab);
				lessFab.setVisibility(View.VISIBLE);
				android.widget.GridLayout
					gridLayout = (android.widget.GridLayout) findViewById(R.id.quickGrid);
				gridLayout.setUseDefaultMargins(false);
			}
		});

		//FloatingActionButton lesserFab = (FloatingActionButton) findViewById(R.id.more_less_fab);
		lessFab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// get text from locationEditText and create web service URL

				FloatingActionButton churchFab = (FloatingActionButton) findViewById(R.id.church_fab);
				FloatingActionButton mosque = (FloatingActionButton) findViewById(R.id.mosque_fab);
				FloatingActionButton postOffice = (FloatingActionButton) findViewById(R.id.post_office_fab);
				FloatingActionButton petrolStation = (FloatingActionButton) findViewById(R.id.petrol_station_fab);
				FloatingActionButton supermarket = (FloatingActionButton) findViewById(R.id.supermarkets_fab);
				FloatingActionButton hotel = (FloatingActionButton) findViewById(R.id.hotel_fab);
				FloatingActionButton pharmacy = (FloatingActionButton) findViewById(R.id.pharmacy_fab);
				FloatingActionButton hospitalFab = (FloatingActionButton) findViewById(R.id.hospitalFab);
				FloatingActionButton less_Fab = (FloatingActionButton) findViewById(R.id.less_fab);

				//FloatingActionButton hospital = (FloatingActionButton) findViewById(R.id.hospitalFab);
				TextView churchText = (TextView) findViewById(R.id.church_textView);
				TextView mosqueText = (TextView) findViewById(R.id.mosque_textView);
				TextView postOfficeText = (TextView) findViewById(R.id.post_office_textView);
				TextView petrolStationText = (TextView) findViewById(R.id.petrol_stations_textView);
				TextView supermarketText = (TextView) findViewById(R.id.supermarket_textView);
				TextView hotelText = (TextView) findViewById(R.id.hotel_textView);
				TextView pharmacyText = (TextView) findViewById(R.id.chemst_textView);
				TextView moreLess = (TextView) findViewById(R.id.more_Less_textView);
				TextView hospitalText = (TextView) findViewById(R.id.hospital_textView);
				TextView lessText = (TextView) findViewById(R.id.less_textView);

				less_Fab.setVisibility(View.VISIBLE);
				lessText.setVisibility(View.VISIBLE);
				hospitalFab.setVisibility(View.GONE);
				hospitalText.setVisibility(View.GONE);
				pharmacy.setVisibility(View.GONE);
				pharmacyText.setVisibility(GONE);
				churchFab.setVisibility(View.GONE);
				churchText.setVisibility(View.GONE);
				supermarket.setVisibility(View.GONE);
				supermarketText.setVisibility(View.GONE);
				hotel.setVisibility(View.GONE);
				hotelText.setVisibility(View.GONE);
				petrolStation.setVisibility(View.GONE);
				petrolStationText.setVisibility(View.GONE);
				mosque.setVisibility(View.GONE);
				mosqueText.setVisibility(View.GONE);
				postOffice.setVisibility(View.GONE);
				postOfficeText.setVisibility(View.GONE);
				moreLess.setVisibility(View.GONE);

				// CardView quickCard = (CardView) findViewById(R.id.quickSearchCard);
				FloatingActionButton lessFab = (FloatingActionButton) findViewById(R.id.more_less_fab);
				lessFab.setVisibility(GONE);
				android.widget.GridLayout
					gridLayout = (android.widget.GridLayout) findViewById(R.id.quickGrid);
				gridLayout.setUseDefaultMargins(true);


			}
		});


	}


	// programmatically dismiss keyboard when user touches FAB
	private void dismissKeyboard(View view) {
		InputMethodManager imm = (InputMethodManager) getSystemService(
			Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

	}

	protected void onStart() {
		mGoogleApiClient.connect();
		super.onStart();
	}

	public void onConnected(Bundle connectionHint) {
		try {
			Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
				mGoogleApiClient);
			if(mLastLocation != null) {
				setMLat(mLastLocation.getLatitude());
				setMLong(mLastLocation.getLongitude());
			}
		} catch(SecurityException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch(requestCode) {
			case PERMISSION_ACCESS_COARSE_LOCATION:
				if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					// All good!
					Snackbar.make(findViewById(R.id.coordinatorLayout),
						"Location Access Granted", Snackbar.LENGTH_LONG).show();
				} else {
					Toast.makeText(this, "Need your location!", Toast.LENGTH_SHORT).show();
				}

				break;
		}
	}

	private double getmLong() {
		return mLong;
	}

	private void setMLong(double mLong) {
		this.mLong = mLong;
	}

	private double getMLat() {
		return mLat;
	}

	private void setMLat(double mLat) {
		this.mLat = mLat;
	}

	private double getFinalLong() {
		return finalLong;
	}

	private void setFinalLong(double finalLong) {
		this.finalLong = finalLong;
	}

	private double getFinalLat() {
		return finalLat;
	}

	private void setFinalLat(double finalLat) {
		this.finalLat = finalLat;
	}

	@Override
	public void onConnectionSuspended(int i) {
		Snackbar.make(findViewById(R.id.coordinatorLayout),
			R.string.invalid_url, Snackbar.LENGTH_LONG).show();
	}

	// create google places web service URL using the service intended
	@Nullable
	private URL createURL(String places) {
		String apiKey = getString(R.string.api_key);
		String baseUrl = getString(R.string.web_service_url);
		String order = "&rankby=distance&";
		String s = places.replaceAll(" ", "_");
		String trim = s.trim();
		try {
			String urlString;
			urlString =
				String.format("%s%s,%s%s&name=%s&key=%s", baseUrl, getMLat(), getmLong(),
					order, trim, apiKey);

			return new URL(urlString);
		} catch(Exception e) {
			e.printStackTrace();
		}

		return null; // URL was malformed
	}

// --Commented out by Inspection START (12/30/16 4:34 PM):
//    @Nullable
//    private URL createURL2(String placeID) {
//
//        double lat = 1.0;
//        String base = getString(R.string.base);
//        try {
//            String urlString;
//            urlString = String.format("%s%s%s", base, placeID, getString(R.string.key2));
//            return new URL(urlString);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return null; // URL was malformed
//    }
// --Commented out by Inspection STOP (12/30/16 4:34 PM)

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
		Snackbar.make(findViewById(R.id.coordinatorLayout),
			R.string.internet_error, Snackbar.LENGTH_LONG).show();
	}

	// create Weather objects from JSONObject containing the forecast
	private void convertJSONtoArrayList(JSONObject forecast) {

		weatherList.clear(); // clear old weather data

		try {
			// get forecast's "list" JSONArray

			if(forecast.getString(getString(R.string.status_json)).equals("ZERO_RESULTS")) {
				Snackbar.make(findViewById(R.id.coordinatorLayout),
					R.string.read_error,
					Snackbar.LENGTH_LONG).show();
				ProgressBar loadingSpin = (ProgressBar) findViewById(R.id.loadingBar);
				loadingSpin.setVisibility(GONE);
				// Toast.makeText(this, R.string.read_error, Toast.LENGTH_LONG).show();
			} else {

				JSONArray list = forecast.getJSONArray(getString(R.string.results_json));

				for(int i = 0; i < list.length(); ++ i) {

					JSONObject place = list.getJSONObject(i); // get one day's data
					JSONObject north = place.getJSONObject(getString(R.string.geometry_json));
					JSONObject location = north.getJSONObject(getString(R.string.location_json));

					// set destination latitude and longitude
					setFinalLat(location.getDouble(getString(R.string.lat_json)));
					setFinalLong(location.getDouble(getString(R.string.lng_json)));

					ProgressBar loadingSpin = (ProgressBar) findViewById(R.id.loadingBar);
					loadingSpin.setVisibility(GONE);

					weatherList.add(new Weather(
						                           place.getString(getString(R.string.name_json)), // name of place

						                           getFinalLat(), // distance between current location and destination
						                           getFinalLong(),// maximum temperature

						                           // Calculate the distance
						                           calcDistance(getMLat(), getmLong(),
							                           location.getDouble(getString(R.string.lat_json)),
							                           location.getDouble(getString(R.string.lng_json))),
						                           place.getString(getString(R.string.vicinity_json)), // place description
						                           place.getString(getString(R.string.icon_json)), // icon name
						                           place.getString(getString(R.string.place_id_json))
						)
					);
				}
			}

		} catch(JSONException e) {
			e.printStackTrace();
		}
	}


	private double calcDistance(double latA, double longA, double latB, double longB) {
		Location locationA = new Location("Initial");
		locationA.setLatitude(latA);
		locationA.setLongitude(longA);
		Location locationB = new Location("Final");
		locationB.setLatitude(latB);
		locationB.setLongitude(longB);
		return (double) ((locationA.distanceTo(locationB)) / 1000);
	}


	private void checkNetworkConnection() {

		ConnectivityManager connMgr =
			(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();

		if(activeInfo != null && activeInfo.isConnected()) {
			boolean wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
			boolean mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;

			if(wifiConnected) Snackbar.make(findViewById(R.id.coordinatorLayout),
				R.string.wifi_connected, Snackbar.LENGTH_SHORT).show();
			else if(mobileConnected) Snackbar.make(findViewById(R.id.coordinatorLayout),
				R.string.mobile_data_connected, Snackbar.LENGTH_SHORT).show();
		} else Snackbar.make(findViewById(R.id.coordinatorLayout),
			R.string.connection_error, Snackbar.LENGTH_LONG).show();
	}

	// makes the REST web services call to get weather data and
	// saves the data to a local HTML file
	private class GetPlaceTask extends AsyncTask<URL, Void, JSONObject> {
		@Override
		protected JSONObject doInBackground(URL... params) {
			HttpURLConnection connection = null;

			try {
				connection = (HttpURLConnection) params[0].openConnection();
				int response = connection.getResponseCode();

				if(response == HttpURLConnection.HTTP_OK) {
					StringBuilder builder = new StringBuilder();

					try(BufferedReader reader = new BufferedReader(
						                                              new InputStreamReader(connection.getInputStream()))) {
						String line;

						while((line = reader.readLine()) != null) {
							builder.append(line);
						}
					} catch(IOException e) {
						Snackbar.make(findViewById(R.id.coordinatorLayout),
							R.string.read_error, Snackbar.LENGTH_LONG).show();
						e.printStackTrace();
					}
					return new JSONObject(builder.toString());
				} else {
					Snackbar.make(findViewById(R.id.coordinatorLayout),
						R.string.connect_error, Snackbar.LENGTH_LONG).show();
				}
			} catch(Exception e) {
				Snackbar.make(findViewById(R.id.coordinatorLayout),
					R.string.connect_error, Snackbar.LENGTH_LONG).show();
			} finally {
				assert connection != null;
				connection.disconnect(); // close the httpURLConnection
			}
			return null;
		}

		// process JSON response and update ListView
		//@Override
		protected void onPostExecute(JSONObject weather) {

			convertJSONtoArrayList(weather); // repopulate weatherList
			weatherArrayAdapter.notifyDataSetChanged(); // rebind to ListView
			weatherListView.smoothScrollToPosition(0); // scroll to top

		}
	}
}
