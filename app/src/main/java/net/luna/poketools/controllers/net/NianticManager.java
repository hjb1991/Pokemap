package net.luna.poketools.controllers.net;

import android.app.Activity;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.auth.GoogleUserCredentialProvider;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import net.luna.common.debug.LunaLog;
import net.luna.common.entity.HttpRequest;
import net.luna.common.entity.HttpResponse;
import net.luna.common.util.HttpUtils;
import net.luna.common.util.JSONUtils;
import net.luna.poketools.models.events.CatchablePokemonEvent;
import net.luna.poketools.models.events.GymsEvent;
import net.luna.poketools.models.events.InternalExceptionEvent;
import net.luna.poketools.models.events.LoginEventResult;
import net.luna.poketools.models.events.LurePokemonEvent;
import net.luna.poketools.models.events.PokestopsEvent;
import net.luna.poketools.models.events.ServerUnreachableEvent;
import net.luna.poketools.models.login.GoogleLoginInfo;
import net.luna.poketools.models.login.LoginInfo;
import net.luna.poketools.models.login.PtcLoginInfo;
import net.luna.poketools.models.map.ApiPokemon;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import POGOProtos.Map.Fort.FortDataOuterClass;
import POGOProtos.Map.Fort.FortLureInfoOuterClass;
import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.pokegoapi.auth.PtcCredentialProvider.CLIENT_ID;
import static com.pokegoapi.auth.PtcCredentialProvider.CLIENT_SECRET;
import static com.pokegoapi.auth.PtcCredentialProvider.LOGIN_OAUTH;
import static com.pokegoapi.auth.PtcCredentialProvider.LOGIN_URL;
import static com.pokegoapi.auth.PtcCredentialProvider.REDIRECT_URI;

/**
 * Created by vanshilshah on 20/07/16.
 */
public class NianticManager {
    private static final String TAG = "NianticManager";

    private static final String BASE_URL = "https://sso.pokemon.com/sso/";

    private static final NianticManager instance = new NianticManager();

    private Handler mHandler;
    private AuthInfo mAuthInfo;
    private NianticService mNianticService;
    private final OkHttpClient mClient;
    private final OkHttpClient mPoGoClient;
    private PokemonGo mPokemonGo;

    private int pokemonFound = 0;
    private int currentScan = 0;
    private int pendingSearch = 0;

    private int currentBatchCall = 0;

    public static NianticManager getInstance() {
        return instance;
    }

    private NianticManager() {
        mPoGoClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        HandlerThread thread = new HandlerThread("Niantic Manager Thread");
        thread.start();
        mHandler = new Handler(thread.getLooper());

                  /*
        This is a temporary, in-memory cookie jar.
		We don't require any persistence outside of the scope of the login,
		so it being discarded is completely fine
		*/
        CookieJar tempJar = new CookieJar() {
            private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                cookieStore.put(url.host(), cookies);
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> cookies = cookieStore.get(url.host());
                return cookies != null ? cookies : new ArrayList<Cookie>();
            }
        };

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        mClient = new OkHttpClient.Builder()
                .cookieJar(tempJar)
                .addInterceptor(new NetworkRequestLoggingInterceptor())
                .build();

        mNianticService = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(mClient)
                .build()
                .create(NianticService.class);
    }

    public void login(final String username, final String password, final LoginListener loginListener) {
        Callback<NianticService.LoginValues> valuesCallback = new Callback<NianticService.LoginValues>() {
            @Override
            public void onResponse(Call<NianticService.LoginValues> call, Response<NianticService.LoginValues> response) {
                if (response.body() != null) {
                    loginPTC(username, password, response.body(), loginListener);
                } else {
                    Log.e(TAG, "PTC login failed via login(). There was no response.body().");
                    loginListener.authFailed("Fetching Pokemon Trainer Club's Login Url Values Failed");
                }

            }

            @Override
            public void onFailure(Call<NianticService.LoginValues> call, Throwable t) {
                t.printStackTrace();
                Log.e(TAG, "PTC login failed via login(). valuesCallback.onFailure() threw: " + t.getMessage());
                loginListener.authFailed("Fetching Pokemon Trainer Club's Login Url Values Failed");
            }
        };
        Call<NianticService.LoginValues> call = mNianticService.getLoginValues();
        call.enqueue(valuesCallback);
    }

    private void loginPTC(final String username, final String password, NianticService.LoginValues values, final LoginListener loginListener) {
        HttpUrl url = HttpUrl.parse(LOGIN_URL).newBuilder()
                .addQueryParameter("lt", values.getLt())
                .addQueryParameter("execution", values.getExecution())
                .addQueryParameter("_eventId", "submit")
                .addQueryParameter("username", username)
                .addQueryParameter("password", password)
                .build();

        OkHttpClient client = mClient.newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .build();

        NianticService service = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
                .create(NianticService.class);

        Callback<NianticService.LoginResponse> loginCallback = new Callback<NianticService.LoginResponse>() {
            @Override
            public void onResponse(Call<NianticService.LoginResponse> call, Response<NianticService.LoginResponse> response) {
                String location = response.headers().get("location");
                if (location != null && location.split("ticket=").length > 0) {
                    String ticket = location.split("ticket=")[1];
                    requestToken(ticket, loginListener);
                } else {
                    Log.e(TAG, "PTC login failed via loginPTC(). There was no location header in response.");
                    loginListener.authFailed("Pokemon Trainer Club Login Failed");
                }
            }

            @Override
            public void onFailure(Call<NianticService.LoginResponse> call, Throwable t) {
                t.printStackTrace();
                Log.e(TAG, "PTC login failed via loginPTC(). loginCallback.onFailure() threw: " + t.getMessage());
                loginListener.authFailed("Pokemon Trainer Club Login Failed");
            }
        };
        Call<NianticService.LoginResponse> call = service.login(url.toString());
        call.enqueue(loginCallback);
    }

    private void requestToken(String code, final LoginListener loginListener) {
        Log.d(TAG, "requestToken() called with: code = [" + code + "]");
        HttpUrl url = HttpUrl.parse(LOGIN_OAUTH).newBuilder()
                .addQueryParameter("client_id", CLIENT_ID)
                .addQueryParameter("redirect_uri", REDIRECT_URI)
                .addQueryParameter("client_secret", CLIENT_SECRET)
                .addQueryParameter("grant_type", "refresh_token")
                .addQueryParameter("code", code)
                .build();

        Callback<ResponseBody> authCallback = new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String token = response.body().string().split("token=")[1];

                    if (token != null) {
                        token = token.split("&")[0];

                        loginListener.authSuccessful(token);
                    } else {
                        Log.e(TAG, "PTC login failed while fetching a requestToken via requestToken(). Token is null.");
                        loginListener.authFailed("Pokemon Trainer Club Login Failed");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "PTC login failed while fetching a requestToken authCallback.onResponse() raised: " + e.getMessage());
                    loginListener.authFailed("Pokemon Trainer Club Authentication Failed");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                Log.e(TAG, "PTC login failed while fetching a requestToken authCallback.onResponse() threw: " + t.getMessage());
                loginListener.authFailed("Pokemon Trainer Club Authentication Failed");
            }
        };
        Call<ResponseBody> call = mNianticService.requestToken(url.toString());
        call.enqueue(authCallback);
    }

    public int getPokemonFound() {
        return pokemonFound;
    }

    public void setPokemonFound(int pokemonFound) {
        this.pokemonFound = pokemonFound;
    }

    public int getCurrentScan() {
        return currentScan;
    }

    public int getPendingSearch() {
        return pendingSearch;
    }

    public interface LoginListener {
        void authSuccessful(String authToken);

        void authFailed(String message);
    }

    public interface AuthListener {
        void authSuccessful();

        void authFailed(String message, String Provider);
    }

    /**
     * Sets the google auth token for the auth info also invokes the onLogin callback.
     */
    public void setLoginInfo(@NonNull final LoginInfo info) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mAuthInfo = info.createAuthInfo();
                    if (info instanceof PtcLoginInfo) {
                        mPokemonGo = new PokemonGo(new PtcCredentialProvider(mPoGoClient, ((PtcLoginInfo) info).getUsername(), ((PtcLoginInfo) info).getPassword()), mPoGoClient);
                    }
                    EventBus.getDefault().post(new LoginEventResult(true, mAuthInfo, mPokemonGo));
                } catch (RemoteServerException | LoginFailedException | RuntimeException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Setting google auth token failed. setGoogleAuthToken() raised: " + e.getMessage());
                    EventBus.getDefault().post(new LoginEventResult(false, null, null));
                }
            }
        });
    }

    /**
     * Sets the pokemon trainer club auth token for the auth info also invokes the onLogin callback.
     */
    public void setLoginInfo(final Activity activity, @NonNull final LoginInfo info, @NonNull final AuthListener listener) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mAuthInfo = info.createAuthInfo();
                    if (info instanceof PtcLoginInfo) {
                        mPokemonGo = new PokemonGo(new PtcCredentialProvider(mPoGoClient, ((PtcLoginInfo) info).getUsername(), ((PtcLoginInfo) info).getPassword()), mPoGoClient);
                    } else if (info instanceof GoogleLoginInfo) {
                        mPokemonGo = new PokemonGo(new GoogleUserCredentialProvider(mPoGoClient, ((GoogleLoginInfo) info).getRefreshToken()), mPoGoClient);
                    }

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.authSuccessful();
                        }
                    });
                } catch (RemoteServerException | LoginFailedException | RuntimeException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed to login using PoGoAPI via login(). Raised: " + e.getMessage());
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.authFailed(e.getMessage(), info.getProvider());
                        }
                    });
                }
            }
        });
    }

    public void getCatchablePokemon(final double lat, final double longitude, final double alt) {
        final int myCurrentBatch = this.currentBatchCall;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mPokemonGo != null && NianticManager.this.currentBatchCall == myCurrentBatch) {
                        Thread.sleep(133);
                        mPokemonGo.setLocation(lat, longitude, alt);
                        Thread.sleep(133);
                        List<CatchablePokemon> catchablePokemons = mPokemonGo.getMap().getCatchablePokemon();
                        LunaLog.d("getCatchablePokemon(" + lat + ", " + longitude + ")");
                        if (NianticManager.this.currentBatchCall == myCurrentBatch)
                            EventBus.getDefault().post(new CatchablePokemonEvent(catchablePokemons, lat, longitude));
                    }

                } catch (LoginFailedException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed to fetch map information via getCatchablePokemon(). Login credentials wrong or user banned. Raised: " + e.getMessage());
                    EventBus.getDefault().post(new LoginEventResult(false, null, null));
                } catch (RemoteServerException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed to fetch map information via getCatchablePokemon(). Remote server unreachable. Raised: " + e.getMessage());
                    EventBus.getDefault().post(new ServerUnreachableEvent(e));
                } catch (InterruptedException | RuntimeException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed to fetch map information via getCatchablePokemon(). PoGoAPI crashed. Raised: " + e.getMessage());
                    EventBus.getDefault().post(new InternalExceptionEvent(e));
                }
                NianticManager.this.currentScan++;
            }
        });
        this.pendingSearch++;
    }

    public void findTargetPokemonFromThridApi(final double latitude, final double longitude, final String name, final int id) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    double north = latitude + 0.02;
                    double south = latitude - 0.02;
                    double west = longitude - 0.02;
                    double east = longitude + 0.02;


                    HttpRequest request = new HttpRequest("http://skiplagged.com/api/pokemon.php");
                    request.setPostContent("bounds=" + south + "%2C" + west + "%2C" + north + "%2C" + east);
                    request.setRequestProperty("User-Agent", "Android");
                    String result = HttpUtils.httpPost(request).getResponseBody();
                    List<ApiPokemon> apiPokemons = new ArrayList<ApiPokemon>();
                    LunaLog.d(result);
                    JSONObject jsonObject = JSONUtils.toJsonObject(result);
                    if (jsonObject != null) {
                        JSONArray jsonArray2 = jsonObject.optJSONArray("pokemons");

                        if (jsonArray2 != null && jsonArray2.length() > 0) {
                            for (int i = 0; i < jsonArray2.length(); i++) {
                                JSONObject pokeJo = jsonArray2.optJSONObject(i);
                                if (pokeJo != null) {
                                    ApiPokemon apiPokemon = new ApiPokemon();
                                    apiPokemon.setPokemonId(pokeJo.optInt("pokemon_id"));
                                    if (apiPokemon.getPokemonId() != null) {
                                        LunaLog.d(apiPokemon.getPokemonId().name() + "   contains:  " + name);
                                        if (!apiPokemon.getPokemonId().name().contains(name)) {
                                            continue;
                                        }
                                    }
                                    apiPokemon.setExpire(pokeJo.optLong("expires") * 1000);
                                    apiPokemon.setLatitude(pokeJo.optDouble("latitude"));
                                    apiPokemon.setLongitude(pokeJo.optDouble("longitude"));
                                    apiPokemon.generateSpawnPointId();
                                    apiPokemons.add(apiPokemon);
                                }
                            }
                            EventBus.getDefault().post(new CatchablePokemonEvent(latitude, longitude, apiPokemons));
                        }
                    }
                } catch (Exception e) {
                    LunaLog.e(e);
                }
            }
        });
    }

    public void getCatchablePokemonFromThridApi(final double latitude, final double longitude) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    double north = latitude + 0.01;
                    double south = latitude - 0.01;
                    double west = longitude - 0.01;
                    double east = longitude + 0.01;
                    List<ApiPokemon> apiPokemons = new ArrayList<ApiPokemon>();
                    HttpRequest request = new HttpRequest("http://skiplagged.com/api/pokemon.php");
                    request.setPostContent("bounds=" + south + "%2C" + west + "%2C" + north + "%2C" + east);
                    request.setRequestProperty("User-Agent", "Android");
                    HttpResponse response = HttpUtils.httpPost(request);
                    LunaLog.d("wodiu");
                    if (response != null) {
                        String result2 = response.getResponseBody();
                        LunaLog.d("pokenmon: "+result2);
                        JSONObject jsonObject = JSONUtils.toJsonObject(result2);
                        if (jsonObject != null) {
                            JSONArray jsonArray2 = jsonObject.optJSONArray("pokemons");

                            if (jsonArray2 != null && jsonArray2.length() > 0) {
                                for (int i = 0; i < jsonArray2.length(); i++) {
                                    JSONObject pokeJo = jsonArray2.optJSONObject(i);
                                    if (pokeJo != null) {
                                        ApiPokemon apiPokemon = new ApiPokemon();
                                        apiPokemon.setExpire(pokeJo.optLong("expires") * 1000);
                                        apiPokemon.setPokemonId(pokeJo.optInt("pokemon_id"));
                                        apiPokemon.setLatitude(pokeJo.optDouble("latitude"));
                                        apiPokemon.setLongitude(pokeJo.optDouble("longitude"));
                                        apiPokemon.generateSpawnPointId();
                                        apiPokemons.add(apiPokemon);
                                    }
                                }
                                EventBus.getDefault().post(new CatchablePokemonEvent(latitude, longitude, apiPokemons));
                                return;
                            }
                        }
                    }
                    String url = "https://65kjknf2f8.execute-api.us-west-2.amazonaws.com/beta/pokemon?east=[east]&south=[south]&north=[north]&west=[west]";
                    url = url.replace("[north]", north + "").replace("[south]", south + "").replace("[west]", west + "").replace("[east]", east + "");
                    LunaLog.d(url);
                    String result = HttpUtils.httpGetString(url);
                    LunaLog.d("result: " + result);
                    JSONArray jsonArray = JSONUtils.toJsonArray(result);
                    if (jsonArray != null && jsonArray.length() > 0) {
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject pokeJo = jsonArray.optJSONObject(i);
                            if (pokeJo != null) {
                                ApiPokemon apiPokemon = new ApiPokemon();
                                apiPokemon.setPokemonId(pokeJo.optInt("pokemon_id"));
                                apiPokemon.setExpire(pokeJo.optLong("expire"));
                                apiPokemon.setLatitude(pokeJo.optDouble("latitude"));
                                apiPokemon.setLongitude(pokeJo.optDouble("longitude"));
                                apiPokemon.generateSpawnPointId();
                                apiPokemons.add(apiPokemon);
                            }
                        }
                        EventBus.getDefault().post(new CatchablePokemonEvent(latitude, longitude, apiPokemons));
                        return;
                    }

                } catch (Exception e) {
                    LunaLog.e(e);
                }
                EventBus.getDefault().post(new CatchablePokemonEvent(null, latitude, longitude));

            }
        });
    }

    public void getLuredPokemon(final double lat, final double longitude, final double alt) {
        final int myCurrentBatch = this.currentBatchCall;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {

                    if (mPokemonGo != null && NianticManager.this.currentBatchCall == myCurrentBatch) {

                        Thread.sleep(133);
                        mPokemonGo.setLocation(lat, longitude, alt);
                        Thread.sleep(133);

                        List<CatchablePokemon> pokemon = new ArrayList<>();
                        for (Pokestop pokestop : mPokemonGo.getMap().getMapObjects().getPokestops()) {
                            if (!pokestop.getFortData().getLureInfo().equals(FortLureInfoOuterClass.FortLureInfo.getDefaultInstance())) {
                                Log.d(TAG, "run: hasFortInfo = " + pokestop.getFortData().getLureInfo());
                                pokemon.add(new CatchablePokemon(mPokemonGo, pokestop.getFortData()));
                            }
                        }
                        if (NianticManager.this.currentBatchCall == myCurrentBatch)
                            EventBus.getDefault().post(new LurePokemonEvent(pokemon, lat, longitude));
                    }

                } catch (LoginFailedException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed to fetch map information via getPokeStops(). Login credentials wrong or user banned. Raised: " + e.getMessage());
                    EventBus.getDefault().post(new LoginEventResult(false, null, null));
                } catch (RemoteServerException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed to fetch map information via getPokeStops(). Remote server unreachable. Raised: " + e.getMessage());
                    EventBus.getDefault().post(new ServerUnreachableEvent(e));
                } catch (InterruptedException | RuntimeException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed to fetch map information via getPokeStops(). PoGoAPI crashed. Raised: " + e.getMessage());
                    EventBus.getDefault().post(new InternalExceptionEvent(e));
                }
            }
        });
    }


    public void getPokeStops(final double lat, final double longitude, final double alt) {
        final int myCurrentBatch = this.currentBatchCall;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {

                    if (mPokemonGo != null && NianticManager.this.currentBatchCall == myCurrentBatch) {

                        Thread.sleep(133);
                        mPokemonGo.setLocation(lat, longitude, alt);
                        Thread.sleep(133);
                        Collection<Pokestop> pokestops = mPokemonGo.getMap().getMapObjects().getPokestops();
                        if (NianticManager.this.currentBatchCall == myCurrentBatch)
                            EventBus.getDefault().post(new PokestopsEvent(pokestops, lat, longitude));
                    }

                } catch (LoginFailedException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed to fetch map information via getPokeStops(). Login credentials wrong or user banned. Raised: " + e.getMessage());
                    EventBus.getDefault().post(new LoginEventResult(false, null, null));
                } catch (RemoteServerException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed to fetch map information via getPokeStops(). Remote server unreachable. Raised: " + e.getMessage());
                    EventBus.getDefault().post(new ServerUnreachableEvent(e));
                } catch (InterruptedException | RuntimeException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed to fetch map information via getPokeStops(). PoGoAPI crashed. Raised: " + e.getMessage());
                    EventBus.getDefault().post(new InternalExceptionEvent(e));
                }
            }
        });
    }

    public void getGyms(final double latitude, final double longitude, final double alt) {

        final int myCurrentBatch = this.currentBatchCall;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {

                    if (mPokemonGo != null && NianticManager.this.currentBatchCall == myCurrentBatch) {

                        Thread.sleep(133);
                        mPokemonGo.setLocation(latitude, longitude, alt);
                        Thread.sleep(133);
                        Collection<FortDataOuterClass.FortData> gyms = mPokemonGo.getMap().getMapObjects().getGyms();
                        if (NianticManager.this.currentBatchCall == myCurrentBatch)
                            EventBus.getDefault().post(new GymsEvent(gyms, latitude, longitude));
                    }

                } catch (LoginFailedException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed to fetch map information via getGyms(). Login credentials wrong or user banned. Raised: " + e.getMessage());
                    EventBus.getDefault().post(new LoginEventResult(false, null, null));
                } catch (RemoteServerException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed to fetch map information via getGyms(). Remote server unreachable. Raised: " + e.getMessage());
                    EventBus.getDefault().post(new ServerUnreachableEvent(e));
                } catch (InterruptedException | RuntimeException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed to fetch map information via getGyms(). PoGoAPI crashed. Raised: " + e.getMessage());
                    EventBus.getDefault().post(new InternalExceptionEvent(e));
                }
            }
        });
    }


    public void resetSearchCount() {
        this.pendingSearch = 0;
        this.currentScan = 0;
        this.pokemonFound = 0;
        this.currentBatchCall++;
    }

    public void cancelPendingSearches() {
        this.currentBatchCall++;
    }
}
