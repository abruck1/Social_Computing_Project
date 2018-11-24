package edu.texas.social_computing.hospitals;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.List;

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

    // This currently includes a cheesy way to assign (probably) unique
    // very poor ranks to unranked residents. This is a band-aid for
    // getting ranks for residents that have a hospital on their
    // preference list which does not have that resident on its list.
    public int rankOf(Resident resident) {
        return isRanked(resident)
                ? getPreferences().indexOf(resident.getId())
                : Integer.MAX_VALUE - stringToCharSum(resident.getId());
    }

    public boolean isRanked(Resident resident) {
        return getPreferences().contains(resident.getId());
    }

    private static int stringToCharSum(String s) {
        return s.chars().reduce((a, b) -> a + b).orElse(0);
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
