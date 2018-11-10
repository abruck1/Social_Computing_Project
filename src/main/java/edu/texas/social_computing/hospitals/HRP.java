package edu.texas.social_computing.hospitals;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class HRP {
    private HRP() {}

    /**
     * From: The Hospitals / Residents Problem (1962; Gale, Shapley)
     * David F. Manlove
     * https://pdfs.semanticscholar.org/77d9/f84082674888ca90ca662847983381b23338.pdf?_ga=2.216999441.622816506.1541285838-70410261.1541285838
     *
     * M := ∅;
     * while (some resident r_i is unassigned and r_i has a non-empty list) {
     *     h_j := first hospital on r_i’s list;
     *     // r_i applies to h_j
     *     M := M ∪ {(r_i, h_j)};
     *     if (h_j is over-subscribed) {
     *             r_k := worst resident in M(h_j) according to h_j’s list;
     *             M := M\{(r_k, h_j)};
     *     }
     *     if (h_j is full) {
     *             r_k := worst resident in M(h_j) according to h_j’s list;
     *             for (each successor r_l of r_k on h_j’s list)
     *             delete the pair (r_l, h_j);
     *     }
     * }
     */

    public static Matching run(@NotNull HospitalTable hospitalTable,
                               ResidentTable residentTable,
                               @NotNull Queue<Resident> freeResidents) {
        Matching m = new Matching();

        Map<Hospital,List<String>> hospitalsPrefs = new HashMap<>();
        for (Hospital hospital: hospitalTable.getAll()) {
            hospitalsPrefs.put(hospital,hospital.getPreferences());
        }

        Map<Resident,List<String>> residentsPrefs = new HashMap<>();
        for (Resident resident: residentTable.getAll()) {
            residentsPrefs.put(resident,resident.getPreferences());
        }

        Resident currentResident = freeResidents.poll();
        List<String> currentResidentPref = residentsPrefs.get(currentResident);
        Iterator<String> currentResidentPrefItir = currentResidentPref.iterator();
        while (!m.hasAssignment(currentResident) &&
                (currentResidentPref.size() > 0)) {
            Hospital hospital = hospitalTable.getHospitalById((String) currentResidentPrefItir.next());

            m.assign(currentResident, hospital);
            if (m.isOverSubscribed(hospital)) {
                Resident worstResident = hospital.getWorstResident();
                m.unassign(worstResident);
            }
            if (m.isFull(hospital)) {
                List<Resident> listOfBadResidents = hospital.getWorseThan(hospital.getWorstResident());
                if (!listOfBadResidents.isEmpty()) {
                    Resident worseThanResident = listOfBadResidents.get(0);
                    int indexOfFirstBadResident = hospitalsPrefs.get(hospital).indexOf(worseThanResident.toString()); // FIXME this should be an ID/hash
                    List<String> modifiedHospitalPrefList = hospitalsPrefs.get(hospital).subList(0, indexOfFirstBadResident);
                    hospitalsPrefs.put(hospital, modifiedHospitalPrefList);

                    for (Resident resident : listOfBadResidents) {
                        residentsPrefs.get(resident).remove(residentsPrefs.get(resident).indexOf(hospital));
                    }
                }

            }

            if (m.hasAssignment(currentResident) || currentResidentPref.size() == 0) {
                currentResident = freeResidents.poll();
                currentResidentPref = residentsPrefs.get(currentResident);
                currentResidentPrefItir = currentResidentPref.iterator();
            }
        }
        return m;
    }
}
