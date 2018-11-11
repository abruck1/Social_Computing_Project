package edu.texas.social_computing.hospitals;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.Preferences;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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

    public int rankOf(Resident resident) {
        return getPreferences().contains(resident.getId())
                ? getPreferences().indexOf(resident.getId())
                : Integer.MAX_VALUE;
    }

    /**
     * @param resident
     * @return all residents in the preference list worse than provided resident
     */
    public List<String> getWorseThanIds(Resident resident) {
        return getPreferences().contains(resident.getId())
                ? getPreferences().subList(rankOf(resident) + 1, getPreferences().size())
                : ImmutableList.of();
    }
}
