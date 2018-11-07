package edu.texas.social_computing.hospitals;

public class MatchingUtils {

    /**
     *
     * @return the Resident that is matched to a Hospital that is lower on their preference list than their partner.
     * The tie always goes to r2
     */
    public static Resident worsePlacedResident(Matching m, Resident r1, Resident r2) {
        Hospital h1 = m.getAssignedHospital(r1);
        Hospital h2 = m.getAssignedHospital(r2);

        int pref1 = r1.rankOf(h1);
        int pref2 = r2.rankOf(h2);

        return pref1 >= pref2 ? r1 : r2;
    }
}
