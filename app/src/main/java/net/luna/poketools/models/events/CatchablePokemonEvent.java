package net.luna.poketools.models.events;

import net.luna.poketools.models.map.ApiPokemon;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;

import java.util.List;

/**
 * Created by Jon on 7/23/2016.
 */
public class CatchablePokemonEvent implements IEvent {

    private List<CatchablePokemon> catchablePokemon;
    private List<ApiPokemon> apiPokemons;
    private double lat;
    private double longitude;


    public CatchablePokemonEvent(List<CatchablePokemon> catchablePokemon, double lat, double longitude) {
        this.catchablePokemon = catchablePokemon;
        this.lat = lat;
        this.longitude = longitude;
    }

    public CatchablePokemonEvent(double lat, double longitude, List<ApiPokemon> apiPokemons) {
        this.longitude = longitude;
        this.lat = lat;
        this.apiPokemons = apiPokemons;
    }

    public List<ApiPokemon> getApiPokemons() {
        return apiPokemons;
    }

    public void setApiPokemons(List<ApiPokemon> apiPokemons) {
        this.apiPokemons = apiPokemons;
    }

    public List<CatchablePokemon> getCatchablePokemon() {
        return catchablePokemon;
    }

    public void setCatchablePokemon(List<CatchablePokemon> catchablePokemon) {
        this.catchablePokemon = catchablePokemon;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
