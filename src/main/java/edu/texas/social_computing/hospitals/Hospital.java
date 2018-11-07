package edu.texas.social_computing.hospitals;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@AutoValue
public abstract class Hospital {

    public abstract String getId();

    public abstract int getLocationId();

    public abstract int getCapacity();

    public abstract ImmutableList<String> getPreferences();

    public static Hospital create(String id, int locationId, int capacity, Iterable<String> preferences) {
        return new AutoValue_Hospital(
                id, locationId, capacity, ImmutableList.copyOf(preferences));
    }

    /**
     * @return rejected resident
     */
    public Resident rejectWorstResident() {
        return null;
    }

    /**
     * @return worst resident
     */
    public Resident getWorstResident() {
        return null;
    }

    /**
     * @param resident
     * @return all residents worse than provided resident
     */
    public List<Resident> getWorseThan(Resident resident) {
        return null;
    }


}
