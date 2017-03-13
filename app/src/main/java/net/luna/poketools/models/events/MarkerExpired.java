package net.luna.poketools.models.events;

import com.google.android.gms.maps.model.Marker;
import net.luna.poketools.models.map.ApiPokemon;
import net.luna.poketools.models.map.PokemonMarkerExtended;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;

/**
 * Created by chris on 7/26/2016.
 */

public class MarkerExpired {

    private PokemonMarkerExtended mData;

    public MarkerExpired(PokemonMarkerExtended markerData) {
        mData = markerData;
    }

    public PokemonMarkerExtended getData() {
        return mData;
    }

    public Marker getMarker() {
        return mData.getMarker();
    }

    public CatchablePokemon getPokemon() {
        return mData.getCatchablePokemon();
    }

    public ApiPokemon getApiPokemon() {
        return mData.getApiPokemon();
    }


}
