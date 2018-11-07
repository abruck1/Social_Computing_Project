package edu.texas.social_computing.hospitals;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.List;

@AutoValue
public abstract class Resident {

    private static final String NO_PARTNER = "";

    public abstract String getId();

    public abstract String getPartnerId();

    public abstract ImmutableList<String> getInitialPreferences();

    /**
     * Creates a {@link Resident} with a partner.
     */
    static Resident create(String id, String partnerId, Iterable<String> initialPreferences) {
        return new AutoValue_Resident(id, partnerId, ImmutableList.copyOf(initialPreferences));
    }

    /**
     * Creates a {@link Resident} with no partner.
     */
    static Resident create(String id, Iterable<String> initialPreferences) {
        return new AutoValue_Resident(id, NO_PARTNER, ImmutableList.copyOf(initialPreferences));
    }

    /**
     * Returns the current view of the preferences, which is some modified version of the
     * initial preferences.
     */
    public ImmutableList<String> getPreferences() {
        return getInitialPreferences();
    }

    /**
     * @param locationId
     * @return hospitals in the intersection of the given list and preferences lists
     */
    public List<Hospital> preferencesForLocation(int locationId) {
        return null;
    }

    /**
     * @param hospitals
     * @return hospitals in preference list - given list
     */
    public List<Hospital> exclusivePrunePrefs(List<Hospital> hospitals) {
        return null;
    }

    public boolean hasPartner() {
        return !getPartnerId().equals(NO_PARTNER);
    }

    public int rankOf(Hospital hospital) {
        return getPreferences().contains(hospital.getId())
                ? getPreferences().indexOf(hospital.getId())
                : Integer.MAX_VALUE;
    }
}
