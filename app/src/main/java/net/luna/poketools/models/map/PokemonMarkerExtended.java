package net.luna.poketools.models.map;

import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by socrates on 7/24/2016.
 */
public class PokemonMarkerExtended {


    private CatchablePokemon catchablePokemon;
    private Marker marker;
    private ApiPokemon apiPokemon;

    public PokemonMarkerExtended(CatchablePokemon catchablePokemon, Marker marker) {
        this.catchablePokemon = catchablePokemon;
        this.marker = marker;

    }

    public PokemonMarkerExtended(ApiPokemon apiPokemon, Marker marker) {
        this.apiPokemon = apiPokemon;
        this.marker = marker;
    }

    public CatchablePokemon getCatchablePokemon() {
        return catchablePokemon;
    }

    public void setCatchablePokemon(CatchablePokemon catchablePokemon) {
        this.catchablePokemon = catchablePokemon;
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    public ApiPokemon getApiPokemon() {
        return apiPokemon;
    }
}
