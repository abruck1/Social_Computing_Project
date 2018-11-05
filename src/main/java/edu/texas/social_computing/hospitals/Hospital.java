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

    public abstract Set<Resident> getResidents(); // current residents

    public abstract ImmutableList<Resident> getPreferences();

    public static Hospital create(String id, int locationId, int capacity, Iterable<Resident> preferences) {
        return new AutoValue_Hospital(
                id, locationId, capacity, new HashSet<Resident>(), ImmutableList.copyOf(preferences));
    }

    /**
     * Gets the {@link Hospital} that a {@link Resident} is assigned to. Returns empty {@link Optional} if
     * {@link Resident} is not assigned to any of the given {@link Hospital}s.
     */
    public static Optional<Hospital> getAssignment(Resident resident, List<Hospital> hospitals) {
        return Optional.empty();
    }

    public boolean isOverSubscribed() {
        return getResidents().size() > getCapacity();
    }

    public boolean isFull() {
        return getResidents().size() == getCapacity();
    }

    public void assign(Resident resident) {

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
