package edu.texas.social_computing.hospitals;

import java.util.List;

public class Resident {
    private Resident partner;
    private List<Hospital> preferences;
    private List<Hospital> excludedHospitals;

    public Resident getPartner() {
        return partner;
    }

    public List<Hospital> getPreferences() {
        return preferences;
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
