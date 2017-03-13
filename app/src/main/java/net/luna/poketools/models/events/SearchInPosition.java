package net.luna.poketools.models.events;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Link on 23/07/2016.
 */
public class SearchInPosition {

    private LatLng position;
    private int steps;
    private String name;

    public LatLng getPosition() {
        return position;
    }

    public void setPosition(LatLng position) {
        this.position = position;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
