package ru.pekcherkin.androiduberremake.ui.home;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ru.pekcherkin.androiduberremake.Common;
import ru.pekcherkin.androiduberremake.Model.EventBus.DriverRequestReceived;
import ru.pekcherkin.androiduberremake.R;
import ru.pekcherkin.androiduberremake.Remote.IGoogleAPI;
import ru.pekcherkin.androiduberremake.Remote.RetrofitClient;
import ru.pekcherkin.androiduberremake.Utils.UserUtils;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    @BindView(R.id.chip_decline)
    Chip chip_decline;
    @BindView(R.id.layout_accept)
    CardView layout_accept;
    @BindView(R.id.circular_progress_bar)
    CircularProgressBar circularProgressBar;
    @BindView(R.id.txt_estimate_time)
    TextView txt_estimate_time;
    @BindView(R.id.txt_estimate_distance)
    TextView txt_estimate_distance;
    @BindView(R.id.root_layout)
    FrameLayout root_layout;

    @OnClick(R.id.chip_decline)
    void onDeclineClick() {
        if (driverRequestReceived != null) {
            if (countDownEvent != null)
                countDownEvent.dispose();
            chip_decline.setVisibility(View.GONE);
            layout_accept.setVisibility(View.GONE);
            mMap.clear();
            UserUtils.sendDeclineRequest(root_layout, getContext(), driverRequestReceived.getKey());
            driverRequestReceived = null;
        }
    }


    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private IGoogleAPI iGoogleAPI;
    private Polyline blackPolyline, greenPolyline;
    private PolylineOptions polylineOptions, blackPolyLineOptions;
    private List<LatLng> polylineList;

    private DriverRequestReceived driverRequestReceived;
    private Disposable countDownEvent;

    private GoogleMap mMap;

    private HomeViewModel homeViewModel;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    SupportMapFragment mapFragment;

    private boolean isFirstTime = true;

    //Online system
    DatabaseReference onLineRef, currentUserRef, driversLocationRef;
    GeoFire geoFire;
    ValueEventListener onlineValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            if (dataSnapshot.exists() && currentUserRef != null) {
                currentUserRef.onDisconnect().removeValue();
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            Snackbar.make(mapFragment.getView(), databaseError.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    };


    @Override
    public void onDestroy() {
        Toast.makeText(getContext(), "Дестрой", Toast.LENGTH_LONG).show();

        fusedLocationProviderClient.removeLocationUpdates(locationCallback);

        if (FirebaseAuth.getInstance().getCurrentUser() != null)
            geoFire.removeLocation(FirebaseAuth.getInstance().getCurrentUser().getUid());

        onLineRef.removeEventListener(onlineValueEventListener);

        if (EventBus.getDefault().hasSubscriberForEvent(DriverRequestReceived.class))
            EventBus.getDefault().removeStickyEvent(DriverRequestReceived.class);
        EventBus.getDefault().unregister(this);

        compositeDisposable.clear();

        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerOnlineSystem();
    }

    private void registerOnlineSystem() {
        onLineRef.addValueEventListener(onlineValueEventListener);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(root);
        init();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        return root;
    }

    private void initViews(View root) {
        ButterKnife.bind(this, root);
    }

    private void init() {

        iGoogleAPI = RetrofitClient.getInstance().create(IGoogleAPI.class);

        onLineRef = FirebaseDatabase.getInstance().getReference().child(".info/connected");

        buildLocationRequest();
        buildLocationCallback();
        updateLocation();

    }

    private void updateLocation() {
        if (fusedLocationProviderClient == null) {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }
    }

    private void buildLocationCallback() {
        if (locationCallback == null) {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    LatLng newPosition = new LatLng(locationResult.getLastLocation().getLatitude(),
                            locationResult.getLastLocation().getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPosition, 18f));

                    //Here, after get location, we will get adress name
                    Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                    List<Address> addressList;

                    try {
                        addressList = geocoder.getFromLocation(locationResult.getLastLocation().getLatitude()
                                , locationResult.getLastLocation().getLongitude(), 1);
                        String cityName = addressList.get(0).getLocality();

                        driversLocationRef = FirebaseDatabase.getInstance().getReference(Common.DRIVERS_LOCATION_REFERENCE)
                                .child(cityName);
                        currentUserRef = driversLocationRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid());

                        geoFire = new GeoFire(driversLocationRef);

                        //Update location
                        geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(),
                                new GeoLocation(locationResult.getLastLocation().getLatitude(),
                                        locationResult.getLastLocation().getLongitude()),
                                (key, error) -> {
                                    if (error != null)
                                        Snackbar.make(mapFragment.getView(), "ТУТ СУКА" + error.getMessage(), Snackbar.LENGTH_LONG).show();

                                });

                        registerOnlineSystem();

                    } catch (IOException e) {
                        Snackbar.make(getView(), "Тут ошибка" + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                    }

                }
            };

        }
    }

    private void buildLocationRequest() {
        if (locationRequest == null) {
            locationRequest = new LocationRequest();
            locationRequest.setSmallestDisplacement(50f); //50m
            locationRequest.setInterval(15000); //15sec
            locationRequest.setFastestInterval(10000); //10 sec
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //check permision

        Dexter.withContext(getContext())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);
                        mMap.setOnMyLocationButtonClickListener(() -> {
                            fusedLocationProviderClient.getLastLocation()
                                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Тут" + e.getMessage(), Toast.LENGTH_SHORT).show())
                                    .addOnSuccessListener(location -> {
                                        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f));
                                    });
                            return true;
                        });

                        //setLayout Button
                        View locationButton = ((View) mapFragment.getView().findViewById(Integer.parseInt("1"))
                                .getParent()).findViewById(Integer.parseInt("2"));
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();

                        //right bottom
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                        params.setMargins(0, 0, 0, 250);

                        //Move location
                        buildLocationRequest();
                        buildLocationCallback();
                        updateLocation();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Toast.makeText(getContext(), "В разрешении" + permissionDeniedResponse.getPermissionName() +
                                "было отказано", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                    }
                }).check();
        mMap.getUiSettings().setZoomControlsEnabled(true);

        try {
            boolean succsess = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.uber_maps_style));
            if (succsess) {
                Log.e("EDMT_ERROR", "Style parsing Error");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("EDMT_ERROR", e.getMessage());
        }

        Snackbar.make(mapFragment.getView(), "Вы в онлайне", Snackbar.LENGTH_LONG).show();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onDriverRequestReceive(DriverRequestReceived event) {
        driverRequestReceived = event;

        fusedLocationProviderClient.getLastLocation()
                .addOnFailureListener(e -> Snackbar.make(requireView(), e.getMessage(), Snackbar.LENGTH_LONG).show())
                .addOnSuccessListener(location -> {

                    compositeDisposable.add(iGoogleAPI.getDirections("driving",
                            "less_driving",
                            new StringBuilder()
                                    .append(location.getLatitude())
                                    .append(",")
                                    .append(location.getLongitude())
                                    .toString(),
                            event.getPickupLocation(),
                            getString(R.string.google_api_key))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(returnResult -> {

                                try {
                                    JSONObject jsonObject = new JSONObject(returnResult);
                                    JSONArray jsonArray = jsonObject.getJSONArray("routes");
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        JSONObject route = jsonArray.getJSONObject(i);
                                        JSONObject poly = route.getJSONObject("overview_polyline");
                                        String polyline = poly.getString("points");
                                        polylineList = Common.decodePoly(polyline);
                                    }
                                    polylineOptions = new PolylineOptions();
                                    polylineOptions.color(Color.GRAY);
                                    polylineOptions.width(12);
                                    polylineOptions.startCap(new SquareCap());
                                    polylineOptions.jointType(JointType.ROUND);
                                    polylineOptions.addAll(polylineList);
                                    greenPolyline = mMap.addPolyline(polylineOptions);

                                    blackPolyLineOptions = new PolylineOptions();
                                    blackPolyLineOptions.color(Color.BLACK);
                                    blackPolyLineOptions.width(12);
                                    blackPolyLineOptions.startCap(new SquareCap());
                                    blackPolyLineOptions.jointType(JointType.ROUND);
                                    blackPolyLineOptions.addAll(polylineList);
                                    blackPolyline = mMap.addPolyline(blackPolyLineOptions);

                                    ValueAnimator valueAnimator = ValueAnimator.ofInt(0, 1);
                                    valueAnimator.setDuration(1100);
                                    valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
                                    valueAnimator.setInterpolator(new LinearInterpolator());
                                    valueAnimator.addUpdateListener(value -> {
                                        List<LatLng> points = greenPolyline.getPoints();
                                        int percentValue = (int) valueAnimator.getAnimatedValue();
                                        int size = points.size();
                                        int newPoints = (int) (size * (percentValue / 100.0f));
                                        List<LatLng> p = points.subList(0, newPoints);
                                        blackPolyline.setPoints(p);
                                    });

                                    valueAnimator.start();

                                    LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());
                                    LatLng destination = new LatLng(Double.parseDouble(event.getPickupLocation().split(",")[0]),
                                            Double.parseDouble(event.getPickupLocation().split(",")[1]));

                                    LatLngBounds latLngBounds = new LatLngBounds.Builder()
                                            .include(origin)
                                            .include(destination)
                                            .build();

                                    JSONObject object = jsonArray.getJSONObject(0);
                                    JSONArray legs = object.getJSONArray("legs");
                                    JSONObject legObjects = legs.getJSONObject(0);

                                    JSONObject time = legObjects.getJSONObject("duration");
                                    String duration = time.getString("text");

                                    JSONObject distanceEstimate = legObjects.getJSONObject("distance");
                                    String distance = distanceEstimate.getString("text");

                                    txt_estimate_time.setText(duration);
                                    txt_estimate_distance.setText(distance);

                                    mMap.addMarker(new MarkerOptions()
                                            .position(destination)
                                            .icon(BitmapDescriptorFactory.defaultMarker())
                                            .title("PickupLocation"));

                                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 160));
                                    mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.getCameraPosition().zoom - 1));

                                    chip_decline.setVisibility(View.VISIBLE);
                                    layout_accept.setVisibility(View.VISIBLE);

                                    // count down
                                    countDownEvent = Observable.interval(100, TimeUnit.MILLISECONDS)
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .doOnNext(x -> {
                                                circularProgressBar.setProgress(circularProgressBar.getProgress() + 1f);
                                            })
                                            .takeUntil(aLong -> aLong == 100)
                                            .doOnComplete(() -> {
                                                circularProgressBar.setProgress(0);
                                                Toast.makeText(getContext(), "Fake accept action", Toast.LENGTH_LONG).show();
                                            }).subscribe();


                                } catch (Exception e) {
                                    //Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_LONG).show();
                                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            })
                    );

                });
    }
}
