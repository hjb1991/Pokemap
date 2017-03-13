package net.luna.poketools.models.map;

import net.luna.common.util.DigestUtils;

import POGOProtos.Enums.PokemonIdOuterClass;

/**
 * Created by bintou on 16/8/29.
 */

public class ApiPokemon {

    private long expire;
    private double latitude;
    private double longitude;
    private PokemonIdOuterClass.PokemonId pokemonId;
    private String spawnPointId;

    public long getExpire() {
        return expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }


    public String getSpawnPointId() {
        return spawnPointId;
    }

    public void generateSpawnPointId() {
        spawnPointId = DigestUtils.md5(latitude + "" + longitude + "" + pokemonId + "" + expire);
    }

    public PokemonIdOuterClass.PokemonId getPokemonId() {
        return pokemonId;
    }

    public void setPokemonId(int pokemonId) {
        this.pokemonId = PokemonIdOuterClass.PokemonId.forNumber(pokemonId);
    }
}
