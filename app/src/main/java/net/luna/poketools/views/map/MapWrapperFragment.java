package net.luna.poketools.views.map;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.omkarmoghe.pokemap.R;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;

import net.luna.common.debug.LunaLog;
import net.luna.common.util.StringUtils;
import net.luna.common.util.ToastUtils;
import net.luna.poketools.controllers.AnimationCollector;
import net.luna.poketools.controllers.MarkerRefreshController;
import net.luna.poketools.controllers.app_preferences.PokemapAppPreferences;
import net.luna.poketools.controllers.app_preferences.PokemapSharedPreferences;
import net.luna.poketools.controllers.map.LocationManager;
import net.luna.poketools.controllers.net.NianticManager;
import net.luna.poketools.helpers.MapHelper;
import net.luna.poketools.helpers.RemoteImageLoader;
import net.luna.poketools.models.events.CatchablePokemonEvent;
import net.luna.poketools.models.events.ClearMapEvent;
import net.luna.poketools.models.events.GymsEvent;
import net.luna.poketools.models.events.LurePokemonEvent;
import net.luna.poketools.models.events.MarkerExpired;
import net.luna.poketools.models.events.MarkerUpdate;
import net.luna.poketools.models.events.PokestopsEvent;
import net.luna.poketools.models.events.SearchInPosition;
import net.luna.poketools.models.map.ApiPokemon;
import net.luna.poketools.models.map.GymMarkerExtended;
import net.luna.poketools.models.map.PokemonMarkerExtended;
import net.luna.poketools.models.map.PokestopMarkerExtended;
import net.luna.poketools.util.PokemonIdUtils;
import net.luna.poketools.views.MainActivity;
import net.luna.poketools.views.adapter.CompleteAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import POGOProtos.Enums.PokemonIdOuterClass;
import POGOProtos.Map.Fort.FortDataOuterClass;

/**
 * A simple {@link Fragment} subclass.
 * <p>
 * Use the {@link MapWrapperFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapWrapperFragment extends Fragment implements OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, CompoundButton.OnCheckedChangeListener, GoogleMap.OnCameraChangeListener {

    private final long MS_UPDATE_POKE = 10000;

    private LocationManager locationManager;
    private NianticManager nianticManager;
    private AutoCompleteTextView searchEdit;

    private LatLng mCurPostion;

    private PokemapAppPreferences mPref;
    private View mView;
    private View mFilterLayout, mControlLayout;
    private SwitchCompat pokeTypeSwitch, pokeStopsSwitch, gymsSwitch;
    private SupportMapFragment mSupportMapFragment;
    private GoogleMap mGoogleMap;
    private static Location mLocation = null;
    private PokemonMarkerExtended mSelectedMarker;
    private Map<String, GymMarkerExtended> gymsList = new HashMap<>();

    private List<Circle> userSelectedPositionCircles = new ArrayList<>();
    private List<Marker> userSelectedPositionMarkers = new ArrayList<>();
    private Map<String, PokemonMarkerExtended> markerList = new HashMap<>();
    private Map<String, PokemonMarkerExtended> futureMarkerList = new HashMap<>();
    private Map<String, PokestopMarkerExtended> pokestopsList = new HashMap<>();

    private Set<PokemonIdOuterClass.PokemonId> showablePokemonIDs = new HashSet<>();

    private android.os.Handler scanHandler = new android.os.Handler();

    private void snackMe(String message, int duration) {
        ((MainActivity) getActivity()).snackMe(message, duration);
    }

    private void snackMe(String message) {
        snackMe(message, Snackbar.LENGTH_LONG);
    }

    AnimationCollector collector;

    public MapWrapperFragment() {

//        gymTeamImageUrls.put(TeamColorOuterClass.TeamColor.NEUTRAL_VALUE, "http://i.imgur.com/If3mHMM.png");
//        gymTeamImageUrls.put(TeamColorOuterClass.TeamColor.BLUE_VALUE, "http://i.imgur.com/ElM6sqb.png");
//        gymTeamImageUrls.put(TeamColorOuterClass.TeamColor.RED_VALUE, "http://i.imgur.com/wO13iJ0.png");
//        gymTeamImageUrls.put(TeamColorOuterClass.TeamColor.YELLOW_VALUE, "http://i.imgur.com/F8Jq1dc.png");
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MapWrapperFragment.
     */
    public static MapWrapperFragment newInstance() {
        MapWrapperFragment fragment = new MapWrapperFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mPref = new PokemapSharedPreferences(getContext());
        showablePokemonIDs = mPref.getShowablePokemonIDs();
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        nianticManager.setPokemonFound(markerList.size());
        updateMarkers();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        locationManager = LocationManager.getInstance(getContext());

        nianticManager = NianticManager.getInstance();

        collector = new AnimationCollector(getActivity());

        locationManager.register(new LocationManager.Listener() {
            @Override
            public void onLocationChanged(Location location) {
                if (mLocation == null) {
                    mLocation = location;
                    initMap(true, true);
                } else {
                    mLocation = location;
                }
            }

            @Override
            public void onLocationFetchFailed(@Nullable ConnectionResult connectionResult) {
                showLocationFetchFailed();
            }
        });


        // Inflate the layout for this fragment if the view is not null
        if (mView == null)
            mView = inflater.inflate(R.layout.fragment_map_wrapper, container, false);

        mControlLayout = mView.findViewById(R.id.layout_control);
        mFilterLayout = mView.findViewById(R.id.layout_filter);

        // build the map
        if (mSupportMapFragment == null) {
            mSupportMapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction().replace(R.id.map, mSupportMapFragment).commit();
            mSupportMapFragment.setRetainInstance(true);
        }

        if (mGoogleMap == null) {
            mSupportMapFragment.getMapAsync(this);
        }

        FloatingActionButton locationFab = (FloatingActionButton) mView.findViewById(R.id.location_fab);
        locationFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLocation != null && mGoogleMap != null) {
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(mLocation.getLatitude(), mLocation.getLongitude()), 15));
                    onMapLongClick(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));
                } else {
                    showLocationFetchFailed();
                }
            }
        });

        FloatingActionButton filterFab = (FloatingActionButton) mView.findViewById(R.id.filter_fab);
        filterFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mFilterLayout.getVisibility() == View.GONE) {
                    mFilterLayout.setVisibility(View.VISIBLE);
                    collector.filterLayoutVisitAnim(mControlLayout, mFilterLayout);
                } else {
                    collector.filterLayoutGoneAnim(mControlLayout, mFilterLayout);
                }
            }
        });


        RelativeLayout scanPoleFad = (RelativeLayout) mView.findViewById(R.id.find_poke_fab);
        scanPoleFad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCurPostion != null) {
                    onMapLongClick(new LatLng(mCurPostion.latitude, mCurPostion.longitude));
                } else {
                    onMapLongClick(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));
                }
                scanHandler.postDelayed(loadnew, MS_UPDATE_POKE);
            }
        });

        mView.findViewById(R.id.closeSuggestions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                hideMapSuggestion();
            }
        });

        if (!mPref.getShowMapSuggestion()) {
            hideMapSuggestion();
        }

        searchEdit = (AutoCompleteTextView) mView.findViewById(R.id.edit_search);
        initSearchEdit();


        final ImageButton tipsFab = (ImageButton) mView.findViewById(R.id.btn_tips);
        tipsFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mView.findViewById(R.id.layoutSuggestions).setVisibility(View.VISIBLE);
            }
        });

//        mView.findViewById(R.id.login_text).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                startActivity(new Intent(getActivity(), LoginActivity.class));
//            }
//        });

        pokeTypeSwitch = (SwitchCompat) mView.findViewById(R.id.switch_poke);
        pokeStopsSwitch = (SwitchCompat) mView.findViewById(R.id.switch_stops);
        gymsSwitch = (SwitchCompat) mView.findViewById(R.id.switch_gyms);
        pokeTypeSwitch.setChecked(mPref.getUseOwnApi());
        pokeStopsSwitch.setChecked(mPref.getShowPokestops());
        gymsSwitch.setChecked(mPref.getShowGyms());
        pokeTypeSwitch.setOnCheckedChangeListener(this);
        pokeStopsSwitch.setOnCheckedChangeListener(this);
        gymsSwitch.setOnCheckedChangeListener(this);


        return mView;
    }

    private void hideMapSuggestion() {

        mPref.setShowMapSuggestion(false);

        if (mView != null) {
            mView.findViewById(R.id.layoutSuggestions).setVisibility(View.GONE);
        }
    }

    private boolean mScanning = false;
    private Runnable loadnew = new Runnable() {
        @Override
        public void run() {
            mScanning = true;
            nianticManager.getCatchablePokemonFromThridApi(mCurPostion.latitude, mCurPostion.longitude);
            scanHandler.postDelayed(loadnew, MS_UPDATE_POKE);
            LunaLog.d("load new pokemon");
        }
    };

    private void initMap(boolean animateZoomIn, boolean searchInPlace) {

        if (getView() != null) {
            if (mLocation != null && mGoogleMap != null) {
                if (ContextCompat.checkSelfPermission(mView.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(mView.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    new AlertDialog.Builder(getActivity())
                            .setTitle(getString(R.string.enable_location_permission_title))
                            .setMessage(getString(R.string.enable_location_permission_message))
                            .setPositiveButton(getString(R.string.button_ok), null)
                            .show();
                    return;
                }
                mGoogleMap.setMyLocationEnabled(true);

                LatLng currentLatLngLocation = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());

                if (animateZoomIn) {
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLngLocation, 15));
                } else {
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLngLocation, 15));
                }

                if (searchInPlace) {
                    searchInPlace(currentLatLngLocation);
                }

            } else {
                showLocationFetchFailed();
            }
        }
    }

    private void searchInPlace(LatLng latLngLocation) {

        //Run the initial scan at the current location reusing the long click function
        SearchInPosition sip = new SearchInPosition();
        sip.setPosition(latLngLocation);
        sip.setSteps(mPref.getSteps());
        EventBus.getDefault().post(sip);
    }

    private void clearMarkers() {
        if (mGoogleMap != null) {
            if (markerList != null && !markerList.isEmpty()) {
                for (Iterator<Map.Entry<String, PokemonMarkerExtended>> pokemonIterator = markerList.entrySet().iterator(); pokemonIterator.hasNext(); ) {
                    Map.Entry<String, PokemonMarkerExtended> pokemonEntry = pokemonIterator.next();
                    pokemonEntry.getValue().getMarker().remove();
                    pokemonIterator.remove();
                }
            }
            if (pokestopsList != null && !pokestopsList.isEmpty()) {
                for (Iterator<Map.Entry<String, PokestopMarkerExtended>> pokestopIterator = pokestopsList.entrySet().iterator(); pokestopIterator.hasNext(); ) {
                    Map.Entry<String, PokestopMarkerExtended> pokestopEntry = pokestopIterator.next();
                    pokestopEntry.getValue().getMarker().remove();
                    pokestopIterator.remove();
                }
            }

            if (gymsList != null && !gymsList.isEmpty()) {
                for (Iterator<Map.Entry<String, GymMarkerExtended>> gymIterator = gymsList.entrySet().iterator(); gymIterator.hasNext(); ) {
                    Map.Entry<String, GymMarkerExtended> gymEntry = gymIterator.next();
                    gymEntry.getValue().getMarker().remove();
                    gymIterator.remove();
                }
            }

            clearPokemonCircles();
        }

    }

    private void updateMarkers() {
        if (mGoogleMap != null) {
            if (markerList != null && !markerList.isEmpty()) {
                for (Iterator<Map.Entry<String, PokemonMarkerExtended>> pokemonIterator = markerList.entrySet().iterator(); pokemonIterator.hasNext(); ) {
                    Map.Entry<String, PokemonMarkerExtended> pokemonEntry = pokemonIterator.next();
                    Marker marker = pokemonEntry.getValue().getMarker();
                    long exprieTime = 0;
                    String spawnPoint = null;
                    PokemonIdOuterClass.PokemonId pokemonid = null;
                    if (pokemonEntry.getValue().getCatchablePokemon() != null) {
                        exprieTime = pokemonEntry.getValue().getCatchablePokemon().getExpirationTimestampMs();
                        pokemonid = pokemonEntry.getValue().getCatchablePokemon().getPokemonId();
                        spawnPoint = pokemonEntry.getValue().getCatchablePokemon().getSpawnPointId();
                    } else if (pokemonEntry.getValue().getApiPokemon() != null) {
                        exprieTime = pokemonEntry.getValue().getApiPokemon().getExpire();
                        pokemonid = pokemonEntry.getValue().getApiPokemon().getPokemonId();
                        spawnPoint = pokemonEntry.getValue().getApiPokemon().getSpawnPointId();
                    }

                    if (!showablePokemonIDs.contains(pokemonid)) {
                        marker.remove();
                        pokemonIterator.remove();
                    } else {
                        if (exprieTime == -1) {
                            futureMarkerList.put(spawnPoint, pokemonEntry.getValue());
                            marker.setAlpha(0.6f);
                            marker.setSnippet(getString(R.string.pokemon_will_spawn));
                            continue;
                        }
                        long millisLeft = exprieTime - System.currentTimeMillis();
                        if (millisLeft < 0) {
                            marker.remove();
                            pokemonIterator.remove();
                        } else {
                            marker.setSnippet(getExpirationBreakdown(millisLeft));
                            if (marker.isInfoWindowShown()) {
                                marker.showInfoWindow();
                            }
                        }
                    }
                }
            }
            if (pokestopsList != null && !pokestopsList.isEmpty() && mPref.getShowPokestops()) {

                for (Iterator<Map.Entry<String, PokestopMarkerExtended>> pokestopIterator = pokestopsList.entrySet().iterator(); pokestopIterator.hasNext(); ) {
                    Map.Entry<String, PokestopMarkerExtended> pokestopEntry = pokestopIterator.next();
                    final Pokestop pokestop = pokestopEntry.getValue().getPokestop();
                    final Marker marker = pokestopEntry.getValue().getMarker();

                    int markerSize = getResources().getDimensionPixelSize(R.dimen.pokestop_marker);


                    BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.filter_ic_pokestop);
                    marker.setIcon(bitmapDescriptor);
                    marker.setZIndex(pokestop.hasLurePokemon() ? 1.0f : 0.5f);

                }
            } else if (pokestopsList != null && !pokestopsList.isEmpty() && !mPref.getShowPokestops()) {
                for (Iterator<Map.Entry<String, PokestopMarkerExtended>> pokestopIterator = pokestopsList.entrySet().iterator(); pokestopIterator.hasNext(); ) {
                    Map.Entry<String, PokestopMarkerExtended> pokestopEntry = pokestopIterator.next();
                    Marker marker = pokestopEntry.getValue().getMarker();
                    marker.remove();
                    pokestopIterator.remove();
                }
            }

            if (gymsList != null && !gymsList.isEmpty() && mPref.getShowGyms()) {

                for (Iterator<Map.Entry<String, GymMarkerExtended>> gymIterator = gymsList.entrySet().iterator(); gymIterator.hasNext(); ) {
                    Map.Entry<String, GymMarkerExtended> gymEntry = gymIterator.next();
                    final FortDataOuterClass.FortData gym = gymEntry.getValue().getGym();
                    final Marker marker = gymEntry.getValue().getMarker();

                    int markerSize = getResources().getDimensionPixelSize(R.dimen.gym_marker);


                    BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.map_ic_elfroad);
                    marker.setIcon(bitmapDescriptor);
                }
            } else if (gymsList != null && !gymsList.isEmpty() && !mPref.getShowGyms()) {
                for (Iterator<Map.Entry<String, GymMarkerExtended>> gymIterator = gymsList.entrySet().iterator(); gymIterator.hasNext(); ) {
                    Map.Entry<String, GymMarkerExtended> gymEntry = gymIterator.next();
                    Marker marker = gymEntry.getValue().getMarker();
                    marker.remove();
                    gymIterator.remove();
                }
            }


            if (!mPref.getShowScannedPlaces() && userSelectedPositionCircles != null && !userSelectedPositionCircles.isEmpty()) {
                for (Circle circle : userSelectedPositionCircles) {
                    circle.remove();
                }
                userSelectedPositionCircles.clear();
            }
        }
    }

    private void setPokestopsMarkers(final PokestopsEvent event) {
        if (mGoogleMap != null) {

            int markerSize = getResources().getDimensionPixelSize(R.dimen.pokestop_marker);
            Collection<Pokestop> pokestops = event.getPokestops();

            if (pokestops != null && mPref.getShowPokestops()) {
                Set<String> markerKeys = pokestopsList.keySet();

                for (final Pokestop pokestop : pokestops) {

                    // radial boxing
                    double distanceFromCenterInMeters = MapHelper.distance(new LatLng(event.getLatitude(), event.getLongitude()), new LatLng(pokestop.getLatitude(), pokestop.getLongitude())) * 1000;

                    if (!markerKeys.contains(pokestop.getId()) && distanceFromCenterInMeters <= MapHelper.convertStepsToRadius(mPref.getSteps())) {


                        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.filter_ic_pokestop);

                        Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                                .position(new LatLng(pokestop.getLatitude(), pokestop.getLongitude()))
                                .title(getString(R.string.pokestop))
                                .icon(bitmapDescriptor)
                                .zIndex(MapHelper.LAYER_POKESTOPS)
                                .alpha(pokestop.hasLurePokemon() ? 1.0f : 0.5f)
                                .anchor(0.5f, 0.5f));

                        //adding pokemons to list to be removed on next search
                        pokestopsList.put(pokestop.getId(), new PokestopMarkerExtended(pokestop, marker));
                    }
                }
            }
            updateMarkers();

        } else {
            showMapNotInitializedError();
        }
    }

    private void setGymsMarkers(final GymsEvent event) {
        if (mGoogleMap != null) {

            int markerSize = getResources().getDimensionPixelSize(R.dimen.gym_marker);
            Collection<FortDataOuterClass.FortData> gyms = event.getGyms();

            if (gyms != null && mPref.getShowGyms()) {

                Set<String> markerKeys = gymsList.keySet();

                for (final FortDataOuterClass.FortData gym : gyms) {

                    double distanceFromCenterInMeters = MapHelper.distance(new LatLng(event.getLatitude(), event.getLongitude()), new LatLng(gym.getLatitude(), gym.getLongitude())) * 1000;

                    if (!markerKeys.contains(gym.getId()) && distanceFromCenterInMeters <= MapHelper.convertStepsToRadius(mPref.getSteps())) {


                        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.map_ic_elfroad);

                        Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                                .position(new LatLng(gym.getLatitude(), gym.getLongitude()))
                                .title(getString(R.string.gym))
                                .icon(bitmapDescriptor)
                                .zIndex(MapHelper.LAYER_GYMS)
                                .anchor(0.5f, 0.5f));

                        // adding gyms to list to be removed on next search
                        gymsList.put(gym.getId(), new GymMarkerExtended(gym, marker));
                    }
                }
            }
            updateMarkers();

        } else {
            showMapNotInitializedError();
        }
    }

    private void setApiPokemonMarkers(final List<ApiPokemon> pokeList) {

        int markerSize = getResources().getDimensionPixelSize(R.dimen.pokemon_marker);
        if (mGoogleMap != null) {

            Set<String> markerKeys = markerList.keySet();
            Set<String> futureKeys = futureMarkerList.keySet();
            for (final ApiPokemon poke : pokeList) {
                LunaLog.d("name: " + poke.getPokemonId().name());
                LunaLog.d("id: " + poke.getPokemonId().getNumber());
                LunaLog.d("expire: " + (poke.getExpire() - System.currentTimeMillis() + ""));
                if (futureKeys.contains(poke.getSpawnPointId())) {
                    if (poke.getExpire() > 1) {
                        futureMarkerList.get(poke.getSpawnPointId()).getMarker().remove();
                        futureKeys.remove(poke.getSpawnPointId());
                        futureMarkerList.remove(poke.getSpawnPointId());
                        markerKeys.remove(poke.getSpawnPointId());
                        markerList.remove(poke.getSpawnPointId());
                    }
                }

                if (!markerKeys.contains(poke.getSpawnPointId())) {

                    // checking if we need to show this pokemon
                    final PokemonIdOuterClass.PokemonId pokemonId = poke.getPokemonId();
                    if (showablePokemonIDs.contains(pokemonId)) {
                        RemoteImageLoader.loadMapIcon(
//                                getActivity(), "http://serebii.net/pokemongo/pokemon/" + PokemonIdUtils.getCorrectPokemonImageId(poke.getPokemonId().getNumber()) + ".png",
                                getActivity(), "http://res.pokemon.name/sprites/core/bw/front/" + PokemonIdUtils.getCorrectPokemonImageId(poke.getPokemonId().getNumber()) + ".00.png",
                                markerSize, markerSize,
                                new RemoteImageLoader.Callback() {
                                    @Override
                                    public void onFetch(Bitmap bitmap) {

                                        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);

                                        //Setting marker since we got image
//                                int resourceID = getResources().getIdentifier("p" + poke.getPokemonId().getNumber(), "drawable", getActivity().getPackageName());
                                        final Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(poke.getLatitude(), poke.getLongitude()))
                                                .title(PokemonIdUtils.getLocalePokemonName(getContext(), pokemonId.name()))
                                                .icon(bitmapDescriptor)
                                                .zIndex(MapHelper.LAYER_POKEMONS)
                                                .anchor(0.5f, 0.5f));

                                        //adding pokemons to list to be removed on next search
                                        PokemonMarkerExtended markerExtended = new PokemonMarkerExtended(poke, marker);
                                        markerList.put(poke.getSpawnPointId(), markerExtended);
                                        MarkerRefreshController.getInstance().postMarker(markerExtended);

                                    }
                                }
                        );

                        //Increase founded pokemon counter
                        nianticManager.setPokemonFound(nianticManager.getPokemonFound() + 1);
                    }
                } else if (futureMarkerList.containsKey(poke.getSpawnPointId())) {
                    if (showablePokemonIDs.contains(poke.getPokemonId())) {
                        PokemonMarkerExtended futureMarker = futureMarkerList.get(poke.getSpawnPointId());

                        futureMarkerList.remove(futureMarker);
                    }
                }
            }
            if (getView() != null) {
                if (nianticManager.getCurrentScan() != nianticManager.getPendingSearch()) {
                    snackMe(getString(R.string.toast_still_searching, nianticManager.getPokemonFound()));

                } else {
                    String text = nianticManager.getPokemonFound() > 0 ? getString(R.string.pokemon_found_new, nianticManager.getPokemonFound()) : getString(R.string.pokemon_found_none);
                    if (!mScanning) {
                        snackMe(text);
                    }
                    nianticManager.resetSearchCount();
                }
            }
            updateMarkers();
        } else {
            showMapNotInitializedError();
        }

    }


    private void setPokemonMarkers(final List<CatchablePokemon> pokeList) {
        int markerSize = getResources().getDimensionPixelSize(R.dimen.pokemon_marker);
        if (mGoogleMap != null) {

            Set<String> markerKeys = markerList.keySet();
            Set<String> futureKeys = futureMarkerList.keySet();
            for (final CatchablePokemon poke : pokeList) {
                LunaLog.d("name: " + poke.getPokemonId().name());
                LunaLog.d("id: " + poke.getSpawnPointId());
                LunaLog.d("EXPIRATION: " + poke.getExpirationTimestampMs());
                if (futureKeys.contains(poke.getSpawnPointId())) {
                    if (poke.getExpirationTimestampMs() > 1) {
                        futureMarkerList.get(poke.getSpawnPointId()).getMarker().remove();
                        futureKeys.remove(poke.getSpawnPointId());
                        futureMarkerList.remove(poke.getSpawnPointId());
                        markerKeys.remove(poke.getSpawnPointId());
                        markerList.remove(poke.getSpawnPointId());
                    }
                }

                if (!markerKeys.contains(poke.getSpawnPointId())) {

                    // checking if we need to show this pokemon
                    PokemonIdOuterClass.PokemonId pokemonId = poke.getPokemonId();
                    if (showablePokemonIDs.contains(pokemonId)) {

                        RemoteImageLoader.loadMapIcon(
                                getActivity(), "http://res.pokemon.name/sprites/core/bw/front/" + PokemonIdUtils.getCorrectPokemonImageId(poke.getPokemonId().getNumber()) + ".00.png",
//                                getActivity(), "http://serebii.net/pokemongo/pokemon/" + PokemonIdUtils.getCorrectPokemonImageId(pokemonId.getNumber()) + ".png",
                                markerSize, markerSize,
                                new RemoteImageLoader.Callback() {
                                    @Override
                                    public void onFetch(Bitmap bitmap) {

                                        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);

                                        //Setting marker since we got image
                                        //int resourceID = getResources().getIdentifier("p" + poke.getPokemonId().getNumber(), "drawable", getActivity().getPackageName());
                                        final Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(poke.getLatitude(), poke.getLongitude()))
                                                .title(PokemonIdUtils.getLocalePokemonName(getContext(), poke.getPokemonId().name()))
                                                .icon(bitmapDescriptor)
                                                .zIndex(MapHelper.LAYER_POKEMONS)
                                                .anchor(0.5f, 0.5f));

                                        //adding pokemons to list to be removed on next search
                                        PokemonMarkerExtended markerExtended = new PokemonMarkerExtended(poke, marker);
                                        markerList.put(poke.getSpawnPointId(), markerExtended);
                                        MarkerRefreshController.getInstance().postMarker(markerExtended);

                                    }
                                }
                        );

                        //Increase founded pokemon counter
                        nianticManager.setPokemonFound(nianticManager.getPokemonFound() + 1);
                    }
                } else if (futureMarkerList.containsKey(poke.getSpawnPointId())) {
                    if (showablePokemonIDs.contains(poke.getPokemonId())) {
                        PokemonMarkerExtended futureMarker = futureMarkerList.get(poke.getSpawnPointId());

                        futureMarkerList.remove(futureMarker);
                    }
                }
            }
            if (getView() != null) {
                if (nianticManager.getCurrentScan() != nianticManager.getPendingSearch()) {
                    snackMe(getString(R.string.toast_still_searching, nianticManager.getPokemonFound()));

                } else {
                    String text = nianticManager.getPokemonFound() > 0 ? getString(R.string.pokemon_found_new, nianticManager.getPokemonFound()) : getString(R.string.pokemon_found_none);
                    snackMe(text);
                    nianticManager.resetSearchCount();
                }
            }
            updateMarkers();
        } else {
            showMapNotInitializedError();
        }
    }

    private void removeExpiredMarker(final PokemonMarkerExtended pokemonMarker) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(pokemonMarker.getMarker(), "alpha", 1f, 0f);
        animator.setDuration(400);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                pokemonMarker.getMarker().remove();
                markerList.remove(pokemonMarker);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animator.start();
    }


    private void showMapNotInitializedError() {
        if (getView() != null) {
            snackMe(getString(R.string.toast_map_not_initialized), Snackbar.LENGTH_SHORT);
        }
    }

    private void showLocationFetchFailed() {
        if (getView() != null) {
            snackMe(getString(R.string.toast_no_location), Snackbar.LENGTH_SHORT);
        }
    }

    private String getExpirationBreakdown(long millis) {
        if (millis < 0) {
            return getString(R.string.pokemon_expired);
        }

        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        return getString(R.string.expiring_in, minutes, seconds);
    }

    /**
     * Called whenever a CatchablePokemonEvent is posted to the bus. Posted when new catchable pokemon are found.
     *
     * @param event The event information
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CatchablePokemonEvent event) {
        pregressStopScan();
        LunaLog.d("onEvent: Map Fragment");
        if (event.getCatchablePokemon() != null) {
            setPokemonMarkers(event.getCatchablePokemon());
        } else if (event.getApiPokemons() != null) {
            setApiPokemonMarkers(event.getApiPokemons());
        }
        drawCatchedPokemonCircle(event.getLat(), event.getLongitude());
    }

    /**
     * Called whenever a LurePokemonEvent is posted to the bus. Posted when new catchable pokemon are found.
     *
     * @param event The event information
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(LurePokemonEvent event) {
        if (!event.getCatchablePokemon().isEmpty() && mPref.getShowLuredPokemon()) {
            setPokemonMarkers(event.getCatchablePokemon());
        }
    }

    /**
     * Called whenever a ClearMapEvent is posted to the bus. Posted when the user wants to clear map of any pokemon or marker.
     *
     * @param event The event information
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ClearMapEvent event) {
        nianticManager.cancelPendingSearches();
        clearMarkers();
        MarkerRefreshController.getInstance().clear();
    }

    /**
     * Called whenever a PokestopsEvent is posted to the bus. Posted when new pokestops are found.
     *
     * @param event The event information
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(PokestopsEvent event) {
        setPokestopsMarkers(event);
    }

    /**
     * Called whenever a PokestopsEvent is posted to the bus. Posted when new gyms are found.
     *
     * @param event The event information
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(GymsEvent event) {

        setGymsMarkers(event);
    }

    /**
     * Called whenever a MarkerUpdate is posted to the bus. Posted by {@link MarkerRefreshController} when
     * expired markers need to be removed.
     *
     * @param event
     */

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MarkerExpired event) {
        removeExpiredMarker(event.getData());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MarkerUpdate event) {
        if (mSelectedMarker != null) {
            Marker marker = mSelectedMarker.getMarker();
            if (marker.isInfoWindowShown()) {

                long time = 0l;
                if (mSelectedMarker.getCatchablePokemon() != null) {
                    time = mSelectedMarker.getCatchablePokemon().getExpirationTimestampMs() - System.currentTimeMillis();
                } else {
                    time = mSelectedMarker.getApiPokemon().getExpire() - System.currentTimeMillis();
                }
                marker.setSnippet(getExpirationBreakdown(time));
                marker.showInfoWindow();
            }
        }
    }

    private void clearPokemonCircles() {

        //Check and eventually remove old marker
        if (userSelectedPositionMarkers != null && userSelectedPositionCircles != null) {

            for (Marker marker : userSelectedPositionMarkers) {
                marker.remove();
            }
            userSelectedPositionMarkers.clear();

            for (Circle circle : userSelectedPositionCircles) {
                circle.remove();
            }
            userSelectedPositionCircles.clear();
        }
    }

    private void drawCatchedPokemonCircle(double latitude, double longitude) {

        if (mGoogleMap != null && mPref.getShowScannedPlaces()) {
            double radiusInMeters = MapHelper.SCAN_RADIUS;
            int shadeColor = 0x44DCD90D; // fill
            CircleOptions circleOptions = new CircleOptions()
                    .center(new LatLng(latitude, longitude))
                    .radius(radiusInMeters).fillColor(shadeColor)
                    .strokeColor(Color.TRANSPARENT)
                    .zIndex(MapHelper.LAYER_SCANNED_LOCATIONS);
            final Circle circle = mGoogleMap.addCircle(circleOptions);
            userSelectedPositionCircles.add(circle);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
        MarkerRefreshController.getInstance().clear();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        UiSettings settings = mGoogleMap.getUiSettings();
        settings.setCompassEnabled(true);
        settings.setTiltGesturesEnabled(true);
        settings.setMyLocationButtonEnabled(false);
        //Handle long click
        mGoogleMap.setOnMapLongClickListener(this);
        mGoogleMap.setOnMapClickListener(this);
        mGoogleMap.setOnMarkerClickListener(this);
        mGoogleMap.setOnCameraChangeListener(this);
        //Disable for now coz is under FAB
        settings.setMapToolbarEnabled(false);

        initMap(false, false);
    }

    @Override
    public void onMapLongClick(LatLng position) {
        pregressStartScan();
        if (nianticManager.getPendingSearch() == 0) {
            clearPokemonCircles();
        }

        //Draw user position marker with circle
        drawMarker(position);

        //Sending event to MainActivity
        SearchInPosition sip = new SearchInPosition();
        sip.setPosition(position);
        sip.setSteps(mPref.getSteps());
        EventBus.getDefault().post(sip);

        mView.findViewById(R.id.layoutSuggestions).setVisibility(View.GONE);
    }

    private void drawMarker(LatLng position) {
        if (mGoogleMap != null) {

            Marker userSelectedPositionMarker = mGoogleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(getString(R.string.position_picked))
                    .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getContext().getResources(),
                            R.drawable.ic_my_location_white_24dp)))
                    .zIndex(MapHelper.LAYER_MY_SEARCH)
                    .anchor(0.5f, 0.5f));
            userSelectedPositionMarkers.add(userSelectedPositionMarker);
        } else {
            showMapNotInitializedError();
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();
        for (Map.Entry<String, PokemonMarkerExtended> pm : markerList.entrySet()) {
            if (pm.getValue().getMarker().equals(marker)) {
                mSelectedMarker = pm.getValue();

                long duration = 0l;
                if (mSelectedMarker.getCatchablePokemon() != null) {
                    duration = mSelectedMarker.getCatchablePokemon().getExpirationTimestampMs() - System.currentTimeMillis();
                } else {
                    duration = mSelectedMarker.getApiPokemon().getExpire() - System.currentTimeMillis();
                }
                if (duration < 1) continue;
                MarkerRefreshController.getInstance().startTimer(duration);
                break;
            }
        }
        return true;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        mSelectedMarker = null;
        MarkerRefreshController.getInstance().stopTimer();
    }

    private void pregressStartScan() {
        mView.findViewById(R.id.scan_progress_bar_start).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.scan_progress_bar_stop).setVisibility(View.GONE);
    }

    private void pregressStopScan() {
        mView.findViewById(R.id.scan_progress_bar_start).setVisibility(View.GONE);
        mView.findViewById(R.id.scan_progress_bar_stop).setVisibility(View.VISIBLE);
    }

    private void hideSoftInput(View view) {
        view.clearFocus();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (b && !MainActivity.hasLogin) {
            ToastUtils.show(getActivity(), getString(R.string.open_search_funtion_tips));
            compoundButton.setChecked(false);
            return;
        }
        switch (compoundButton.getId()) {
            case R.id.switch_poke:
                mPref.setUseOwnApi(b);
                break;
            case R.id.switch_stops:
                mPref.setShowPokestops(b);
                break;
            case R.id.switch_gyms:
                mPref.setShowGyms(b);
                break;
        }
    }

    private void initSearchEdit() {
        List<String> pokemonList = new ArrayList<>();

        PokemonIdOuterClass.PokemonId[] ids = PokemonIdOuterClass.PokemonId.values();

        for (PokemonIdOuterClass.PokemonId pokemonId : ids) {
            if ((pokemonId != PokemonIdOuterClass.PokemonId.MISSINGNO) && (pokemonId != PokemonIdOuterClass.PokemonId.UNRECOGNIZED)) {
                pokemonList.add(PokemonIdUtils.getLocalePokemonName(getActivity(), pokemonId.name()));
            }
        }

        CompleteAdapter adapter = new CompleteAdapter(getActivity(), pokemonList);
        searchEdit.setThreshold(1);
        searchEdit.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        searchEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    ((AutoCompleteTextView) view).showDropDown();
                }
            }
        });

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        searchEdit.setDropDownVerticalOffset(getResources().getDimensionPixelOffset(R.dimen.layout_height_6dp));
//        }

        searchEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {

                String searchName = textView.getText().toString();
                searchPokemon(searchName);
                return true;
            }
        });

        searchEdit.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                TextView tv = (TextView) view.findViewById(R.id.complete_item_name);
                String name = tv.getText().toString();
                searchPokemon(name);
                searchEdit.setText("");
                hideSoftInput(searchEdit);
            }
        });
    }

    private void searchPokemon(String searchName) {
        if (StringUtils.isBlank(searchName)) {
            return;
        }
        pregressStartScan();
        if (nianticManager.getPendingSearch() == 0) {
            clearPokemonCircles();
        }
        clearMarkers();

        LatLng position = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
        //Draw user position marker with circle
        drawMarker(position);

        //Sending event to MainActivity
        SearchInPosition sip = new SearchInPosition();
        sip.setName(searchName.toUpperCase());
        sip.setPosition(position);
        sip.setSteps(mPref.getSteps());
        EventBus.getDefault().post(sip);

        mView.findViewById(R.id.layoutSuggestions).setVisibility(View.GONE);
        hideSoftInput(searchEdit);
        snackMe(getString(R.string.toast_still_searching, nianticManager.getPokemonFound()));
        if (mLocation != null && mGoogleMap != null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLocation.getLatitude(), mLocation.getLongitude()), 15));
        } else {
            showLocationFetchFailed();
        }
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        mCurPostion = cameraPosition.target;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scanHandler.removeCallbacks(loadnew);
    }
}