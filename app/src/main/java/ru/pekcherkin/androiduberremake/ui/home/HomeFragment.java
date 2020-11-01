package ru.pekcherkin.androiduberremake.ui.home;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
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
import com.google.android.material.snackbar.BaseTransientBottomBar;
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
import com.kusu.library.LoadingButton;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
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
import ru.pekcherkin.androiduberremake.Model.EventBus.NotifyToRiderEvent;
import ru.pekcherkin.androiduberremake.Model.RiderModel;
import ru.pekcherkin.androiduberremake.Model.TripPlanModel;
import ru.pekcherkin.androiduberremake.R;
import ru.pekcherkin.androiduberremake.Remote.IGoogleAPI;
import ru.pekcherkin.androiduberremake.Remote.RetrofitClient;
import ru.pekcherkin.androiduberremake.Utils.LocationUtils;
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

    @BindView(R.id.txt_rating)
    TextView txt_rating;
    @BindView(R.id.txt_type_uber)
    TextView txt_type_uber;
    @BindView(R.id.img_round)
    ImageView img_round;
    @BindView(R.id.layout_start_uber)
    CardView layout_start_uber;
    @BindView(R.id.txt_rider_name)
    TextView txt_rider_name;
    @BindView(R.id.txt_start_uber_estimate_distance)
    TextView txt_start_uber_estimate_distance;
    @BindView(R.id.txt_start_uber_estimate_time)
    TextView txt_start_uber_estimate_time;
    @BindView(R.id.img_phone_call)
    ImageView img_phone_call;
    @BindView(R.id.btn_start_uber)
    LoadingButton btn_start_uber;
    @BindView(R.id.btn_complete_trip)
    LoadingButton btn_complete_trip;

    @BindView(R.id.layout_notify_rider)
    LinearLayout layout_notify_rider;
    @BindView(R.id.txt_notify_rider)
    TextView txt_notify_rider;
    @BindView(R.id.progress_notify)
    ProgressBar progress_notify;

    private String tripNumberId = "";
    private boolean isTripStart = false, onlineSystemAlreadyRegister = false;

    private GeoFire pickupGeoFire, destinationGeoFire;
    private GeoQuery pickupGeoQuery, destinationGeoQuery;

    private GeoQueryEventListener pickupGeoQueryListener = new GeoQueryEventListener() {
        @Override
        public void onKeyEntered(String key, GeoLocation location) {
            btn_start_uber.setEnabled(true);
            UserUtils.sendNotifyToRider(getContext(), root_layout, key);
            if (pickupGeoQuery != null) {
                pickupGeoFire.removeLocation(key);
                pickupGeoFire = null;
                pickupGeoQuery.removeAllListeners();
            }
        }

        @Override
        public void onKeyExited(String key) {
            btn_start_uber.setEnabled(false);

        }

        @Override
        public void onKeyMoved(String key, GeoLocation location) {

        }

        @Override
        public void onGeoQueryReady() {

        }

        @Override
        public void onGeoQueryError(DatabaseError error) {

        }
    };
    private GeoQueryEventListener destinationGeoQueryListener = new GeoQueryEventListener() {
        @Override
        public void onKeyEntered(String key, GeoLocation location) {
            btn_complete_trip.setEnabled(true);
            if (destinationGeoQuery != null) {
                destinationGeoFire.removeLocation(key);
                destinationGeoFire = null;
                destinationGeoQuery.removeAllListeners();
            }
        }

        @Override
        public void onKeyExited(String key) {

        }

        @Override
        public void onKeyMoved(String key, GeoLocation location) {

        }

        @Override
        public void onGeoQueryReady() {

        }

        @Override
        public void onGeoQueryError(DatabaseError error) {

        }
    };

    private CountDownTimer waiting_timer;
    private String cityName = "";


    @OnClick(R.id.chip_decline)
    void onDeclineClick() {
        if (driverRequestReceived != null) {
            if (TextUtils.isEmpty(tripNumberId)) {
                if (countDownEvent != null)
                    countDownEvent.dispose();
                chip_decline.setVisibility(View.GONE);
                layout_accept.setVisibility(View.GONE);
                mMap.clear();
                UserUtils.sendDeclineRequest(root_layout, getContext(), driverRequestReceived.getKey());
                driverRequestReceived = null;
            } else {
                fusedLocationProviderClient.getLastLocation()
                        .addOnFailureListener(e -> {
                            Snackbar.make(mapFragment.getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
                        })
                        .addOnSuccessListener(location -> {
                            chip_decline.setVisibility(View.GONE);
                            layout_start_uber.setVisibility(View.GONE);
                            mMap.clear();
                            UserUtils.sendDeclineAAndRemoveTripRequest(root_layout, getContext(),
                                    driverRequestReceived.getKey(), tripNumberId);
                            tripNumberId = "";
                            driverRequestReceived = null;
                            makeDriverOnline(location);
                        });
            }

        }
    }

    @OnClick(R.id.btn_start_uber)
    void onStartUberClick() {
        if (blackPolyline != null) blackPolyline.remove();
        if (greenPolyline != null) greenPolyline.remove();

        if (waiting_timer != null) waiting_timer.cancel();
        layout_notify_rider.setVisibility(View.GONE);

        if (driverRequestReceived != null) {
            LatLng destinationLatLng = new LatLng(
                    Double.parseDouble(driverRequestReceived.getDestinationLocation().split(".")[0]),
                    Double.parseDouble(driverRequestReceived.getDestinationLocation().split(".")[1])
            );
            mMap.addMarker(new MarkerOptions()
                    .position(destinationLatLng)
                    .title(driverRequestReceived.getDestinationLocationString())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

            drawPathFromCurrentLocation(driverRequestReceived.getDestinationLocation());
        }
        btn_start_uber.setVisibility(View.GONE);
        chip_decline.setVisibility(View.GONE);
        btn_complete_trip.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.btn_complete_trip)
    void onCompleteTripClick() {
        Map<String, Object> update_trip = new HashMap<>();
        update_trip.put("done", true);
        FirebaseDatabase.getInstance()
                .getReference(Common.Trip)
                .child(tripNumberId)
                .updateChildren(update_trip)
                .addOnFailureListener(e -> Snackbar.make(mapFragment.requireView(), e.getMessage(),
                        Snackbar.LENGTH_SHORT).show())
                .addOnSuccessListener(aVoid -> {
                    fusedLocationProviderClient.getLastLocation()
                            .addOnFailureListener(e -> {
                                Snackbar.make(mapFragment.requireView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
                            })
                            .addOnSuccessListener(location -> {

                                UserUtils.sendCompleteTripToRider(mapFragment.requireView(), getContext(),
                                        driverRequestReceived.getKey(), tripNumberId);

                                mMap.clear();
                                tripNumberId = "";
                                isTripStart = false;
                                chip_decline.setVisibility(View.GONE);
                                layout_accept.setVisibility(View.GONE);
                                circularProgressBar.setProgress(0);
                                layout_start_uber.setVisibility(View.GONE);
                                layout_notify_rider.setVisibility(View.GONE);
                                progress_notify.setProgress(0);

                                btn_complete_trip.setEnabled(false);
                                btn_complete_trip.setVisibility(View.GONE);

                                btn_start_uber.setEnabled(false);
                                btn_start_uber.setVisibility(View.VISIBLE);

                                destinationGeoFire = null;
                                pickupGeoFire = null;

                                driverRequestReceived = null;
                                makeDriverOnline(location);

                            });
                });
    }

    private void drawPathFromCurrentLocation(String destinationLocation) {
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
                            destinationLocation,
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


                                    LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());
                                    LatLng destination = new LatLng(Double.parseDouble(destinationLocation.split(",")[0]),
                                            Double.parseDouble(destinationLocation.split(",")[1]));

                                    LatLngBounds latLngBounds = new LatLngBounds.Builder()
                                            .include(origin)
                                            .include(destination)
                                            .build();

                                    createGeoFireDestinationLocation(driverRequestReceived.getKey(), destination);


                                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 160));
                                    mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.getCameraPosition().zoom - 1));

                                } catch (Exception e) {
                                    //Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_LONG).show();
                                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            })
                    );

                });
    }

    private void createGeoFireDestinationLocation(String key, LatLng destination) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Common.TRIP_DESTINATION_LOCATION_REF);
        destinationGeoFire = new GeoFire(ref);
        destinationGeoFire.setLocation(key, new GeoLocation(destination.latitude, destination.longitude),
                (key1, error) -> {

                });
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
        if (EventBus.getDefault().hasSubscriberForEvent(NotifyToRiderEvent.class))
            EventBus.getDefault().removeStickyEvent(NotifyToRiderEvent.class);
        EventBus.getDefault().unregister(this);

        compositeDisposable.clear();

        onlineSystemAlreadyRegister = false;

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
        if (!onlineSystemAlreadyRegister) {
            onLineRef.addValueEventListener(onlineValueEventListener);
            onlineSystemAlreadyRegister = true;
        }

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

                    if (pickupGeoFire != null) {
                        pickupGeoQuery =
                                pickupGeoFire.queryAtLocation(new GeoLocation(locationResult.getLastLocation().getLatitude(),
                                        locationResult.getLastLocation().getLongitude()), Common.MIN_RANGE_PICKUP_IN_KM);
                        pickupGeoQuery.addGeoQueryEventListener(pickupGeoQueryListener);
                    }

                    //destination

                    if (destinationGeoFire != null) {
                        destinationGeoQuery =
                                destinationGeoFire.queryAtLocation(new GeoLocation(locationResult.getLastLocation().getLatitude(),
                                        locationResult.getLastLocation().getLongitude()), Common.MIN_RANGE_PICKUP_IN_KM);
                        destinationGeoQuery.addGeoQueryEventListener(destinationGeoQueryListener);
                    }

                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPosition, 18f));


                    //Here, after get location, we will get adress name
                    if (!isTripStart) {
                        makeDriverOnline(locationResult.getLastLocation());
                    } else {
                        if (!TextUtils.isEmpty(tripNumberId)) {
                            Map<String, Object> update_data = new HashMap<>();
                            update_data.put("currentLat", locationResult.getLastLocation().getLatitude());
                            update_data.put("currentLng", locationResult.getLastLocation().getLongitude());

                            FirebaseDatabase.getInstance().getReference(Common.Trip)
                                    .child(tripNumberId)
                                    .updateChildren(update_data)
                                    .addOnFailureListener(e -> Snackbar.make(mapFragment.getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show())
                                    .addOnSuccessListener(aVoid -> {

                                    });

                        }
                    }

                }
            };

        }
    }

    private void makeDriverOnline(Location location) {

        String saveCityName = cityName;
        cityName = LocationUtils.getAddressFromLocation(getContext(), location);

        if (!cityName.equals(saveCityName)) {
            if (currentUserRef != null)
                currentUserRef.removeValue()
                        .addOnFailureListener(e -> {
                            Snackbar.make(mapFragment.getView(), e.getMessage(), Snackbar.LENGTH_LONG).show();
                        })
                        .addOnSuccessListener(aVoid -> {
                            updateDriverLocation(location);
                        });
        }
        else
            updateDriverLocation(location);


    }

    private void updateDriverLocation(Location location) {
        if (!TextUtils.isEmpty(cityName)) {

            driversLocationRef = FirebaseDatabase.getInstance().getReference(Common.DRIVERS_LOCATION_REFERENCE)
                    .child(cityName);
            currentUserRef = driversLocationRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            geoFire = new GeoFire(driversLocationRef);

            //Update location
            geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(),
                    new GeoLocation(location.getLatitude(),
                            location.getLongitude()),
                    (key, error) -> {
                        if (error != null)
                            Snackbar.make(mapFragment.getView(), "ТУТ СУКА" + error.getMessage(), Snackbar.LENGTH_LONG).show();

                    });

            registerOnlineSystem();
        }
        else
            Snackbar.make(mapFragment.getView(), "Сервис не доступен", Snackbar.LENGTH_LONG).show();

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
                                            .title("Pickup Location"));

                                    createGeoFirePickupLocation(event.getKey(), destination);

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

                                                createTripPlan(event, duration, distance);

                                            }).subscribe();


                                } catch (Exception e) {
                                    //Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_LONG).show();
                                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            })
                    );

                });
    }

    private void createGeoFirePickupLocation(String key, LatLng destination) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Common.TRIP_PICKUP_REF);
        pickupGeoFire = new GeoFire(ref);
        pickupGeoFire.setLocation(key, new GeoLocation(destination.latitude, destination.longitude),
                (key1, error) -> {
                    Log.d("EDMTDev", key1 + "was create success on GeoFire");
                    if (error != null)
                        Snackbar.make(root_layout, error.getMessage(), Snackbar.LENGTH_LONG)
                                .show();
                    else
                        Log.d("EDMTDev", key1 + "was create success on GeoFire");
                });

    }

    private void createTripPlan(DriverRequestReceived event, String duration, String distance) {
        setProcessLayout(true);
        //Sync server time with device
        FirebaseDatabase.getInstance().getReference("info/serverTimeOffset")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        long timeOffset = dataSnapshot.getValue(Long.class);
                        FirebaseDatabase.getInstance().getReference(Common.RIDER_INFO)
                                .child(event.getKey())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {

                                            RiderModel riderModel = dataSnapshot.getValue(RiderModel.class);
                                            fusedLocationProviderClient.getLastLocation()
                                                    .addOnFailureListener(e -> Snackbar.make(mapFragment.getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show())
                                                    .addOnSuccessListener(location -> {

                                                        TripPlanModel tripPlanModel = new TripPlanModel();
                                                        tripPlanModel.setDriver(FirebaseAuth.getInstance().getCurrentUser().getUid());
                                                        tripPlanModel.setRider(event.getKey());
                                                        tripPlanModel.setDriverInfoModel(Common.currentUser);
                                                        tripPlanModel.setRiderModel(riderModel);
                                                        tripPlanModel.setOrigin(event.getPickupLocation());
                                                        tripPlanModel.setOriginString(event.getPickupLocationString());
                                                        tripPlanModel.setDestination(event.getDestinationLocation());
                                                        tripPlanModel.getDestinationString(event.getDestinationLocationString());
                                                        tripPlanModel.setDistancePickup(distance);
                                                        tripPlanModel.setDurationPickup(duration);
                                                        tripPlanModel.setCurrentLat(location.getLatitude());
                                                        tripPlanModel.setCurrentLng(location.getLongitude());

                                                        tripNumberId = Common.createUniqueTripIdNumber(timeOffset);

                                                        FirebaseDatabase.getInstance().getReference(Common.Trip)
                                                                .child(tripNumberId)
                                                                .setValue(tripPlanModel)
                                                                .addOnFailureListener(e -> {
                                                                    Snackbar.make(mapFragment.getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
                                                                }).addOnSuccessListener(aVoid -> {

                                                            txt_rider_name.setText(riderModel.getFirstName());
                                                            txt_start_uber_estimate_time.setText(duration);
                                                            txt_start_uber_estimate_distance.setText(distance);

                                                            setOfflineModeForDriver(event, duration, distance);
                                                        });
                                                    });

                                        } else {
                                            Snackbar.make(mapFragment.getView(), "клиент не найден с таким ключом  " + event.getKey(), Snackbar.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        Snackbar.make(mapFragment.getView(), databaseError.getMessage(), Snackbar.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Snackbar.make(mapFragment.getView(), databaseError.getMessage(), Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    private void setOfflineModeForDriver(DriverRequestReceived event, String duration, String distance) {
        UserUtils.sendAcceptRequestToRider(mapFragment.getView(), getContext(), event.getKey(), tripNumberId);

        //Go to offline
        if (currentUserRef != null)
            currentUserRef.removeValue();

        setProcessLayout(false);
        layout_accept.setVisibility(View.GONE);
        layout_start_uber.setVisibility(View.VISIBLE);

        isTripStart = true;
    }


    private void setProcessLayout(boolean isProcess) {
        int color = -1;
        if (isProcess) {
            color = ContextCompat.getColor(getContext(), R.color.dark_gray);
            circularProgressBar.setIndeterminateMode(true);
            txt_rating.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_star_dark_gray_24dp, 0);

        } else {
            color = ContextCompat.getColor(getContext(), android.R.color.white);
            circularProgressBar.setIndeterminateMode(true);
            circularProgressBar.setProgress(0);
            txt_rating.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_star_black_24dp, 0);


        }
        txt_estimate_time.setTextColor(color);
        txt_estimate_distance.setTextColor(color);
        ImageViewCompat.setImageTintList(img_round, ColorStateList.valueOf(color));
        txt_rating.setTextColor(color);
        txt_type_uber.setTextColor(color);
    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onNotifyToRider(NotifyToRiderEvent event) {
        layout_notify_rider.setVisibility(View.VISIBLE);
        progress_notify.setMax(Common.WAIT_TIME_IN_MIN * 60);
        waiting_timer = new CountDownTimer(Common.WAIT_TIME_IN_MIN * 60 * 1000, 1000) {
            @Override
            public void onTick(long l) {
                progress_notify.setProgress(progress_notify.getProgress() + 1);

                txt_notify_rider.setText(String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(l) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(l)),
                        TimeUnit.MILLISECONDS.toSeconds(l) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(l))));

            }

            @Override
            public void onFinish() {
                Snackbar.make(root_layout, getString(R.string.time_over), Snackbar.LENGTH_LONG).show();
            }
        }.start();
    }
}

