package net.luna.poketools.controllers.app_preferences;

import android.support.annotation.NonNull;

import net.luna.poketools.models.login.LoginInfo;

import java.util.Set;

import POGOProtos.Enums.PokemonIdOuterClass;

/**
 * A contract which defines a user's app preferences
 */
public interface PokemapAppPreferences {

    LoginInfo getLoginInfo();

    void setLoginInfo(LoginInfo loginInfo);

    boolean isLoggedIn();

    boolean getShowScannedPlaces();
    boolean getShowPokestops();
    void setShowPokestops(boolean bool);
    boolean getShowGyms();
    void setShowGyms(boolean bool);
    boolean getShowLuredPokemon();
    int getSteps();

    boolean getUseOwnApi();
    void setUseOwnApi(boolean bool);

    void clearLoginCredentials();
    /**
     *
     * @param isEnabled Sets if the background service is enabled.
     */
    void setServiceState(@NonNull boolean isEnabled);

    /**
     *
     * @return Returns service state as set in preffs
     */
    boolean isServiceEnabled();

    int getServiceRefreshRate();

    /**
     * @return a set of pokemonIDs which can be shown according to the preferences.
     */
    Set<PokemonIdOuterClass.PokemonId> getShowablePokemonIDs();

    void setShowablePokemonIDs(Set<PokemonIdOuterClass.PokemonId> pokemonIDs);

    void setShowMapSuggestion(boolean showMapSuggestion);

    boolean getShowMapSuggestion();
}
