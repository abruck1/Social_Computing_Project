package edu.texas.social_computing.hospitals;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.List;

@AutoValue
public abstract class Resident {
    public abstract String getId();
    public abstract String getPartnerId();
    public abstract ImmutableList<Hospital> getInitialPreferences();

    static Resident create(String id, String partnerId, Iterable<Hospital> initialPreferences) {
        return new AutoValue_Resident(id, partnerId, ImmutableList.copyOf(initialPreferences));
    }

    /**
     * Returns the current view of the preferences, which is some modified version of the
     * initial preferences.
     */
    public ImmutableList<Hospital> getPreferences() {
        return getInitialPreferences();
    }

    /**
     *
     * @param locationId
     * @return hospitals in the intersection of the given list and preferences lists
     */
    public List<Hospital> preferencesForLocation(int locationId) {
        return null;
    }

    /**
     *
     * @param hospitals
     * @return hospitals in preference list - given list
     */
    public List<Hospital> exclusivePrunePrefs(List<Hospital> hospitals) {
        return null;
    }

}
