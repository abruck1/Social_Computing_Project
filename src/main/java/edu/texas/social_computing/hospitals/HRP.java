package edu.texas.social_computing.hospitals;

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

    public static Matching run(HospitalTable hospitalTable, ResidentTable residentTable, Queue<Resident> freeResidents) {
        Iterator<Resident> freeResidentsItir = freeResidents.iterator();
        Matching m = new Matching();

        Map<Hospital,List<String>> HospitalsPrefs = new HashMap<>();
        for (Hospital hospital: hospitalTable.getAll()) {
            HospitalsPrefs.put(hospital,hospital.getPreferences());
        }

        Map<Resident,List<String>> ResidentsPrefs = new HashMap<>();
        for (Resident resident: residentTable.getAll()) {
            ResidentsPrefs.put(resident,resident.getPreferences());
        }

        Resident currentResident = freeResidentsItir.next();
        List<String> currentResidentPref = ResidentsPrefs.get(currentResident);
        Iterator currentResidentPrefItir = currentResidentPref.iterator();
        while (!m.hasAssignment(currentResident) &&
                (ResidentsPrefs.get(currentResident).size() > 0)) {
            Hospital hospital = hospitalTable.getHospitalById((String) currentResidentPrefItir.next());

            m.assign(currentResident, hospital);
            if (m.isOverSubscribed(hospital)) {
                Resident worstResident = hospital.getWorstResident();
                m.unassign(worstResident, hospital);
            }
            if (m.isFull(hospital)) {
                List<Resident> listOfBadResidents = hospital.getWorseThan(hospital.getWorstResident());
                if (!listOfBadResidents.isEmpty()) {
                    Resident worseThanResident = listOfBadResidents.get(0);
                    int indexOfFirstBadResident = HospitalsPrefs.get(hospital).indexOf(worseThanResident.toString()); // FIXME this should be an ID/hash
                    List<String> modifiedHospitalPrefList = HospitalsPrefs.get(hospital).subList(0, indexOfFirstBadResident);
                    HospitalsPrefs.put(hospital, modifiedHospitalPrefList);

                    for (Resident resident : listOfBadResidents) {
                        List<String> modifiedResidentPrefList = ResidentsPrefs.get(resident);
                        modifiedResidentPrefList.remove(modifiedResidentPrefList.indexOf(hospital));
                        ResidentsPrefs.put(resident, modifiedResidentPrefList);
                    }
                }

            }

            if (m.hasAssignment(currentResident)) {
                currentResident = freeResidentsItir.next();
                currentResidentPref = ResidentsPrefs.get(currentResident);
                currentResidentPrefItir = currentResidentPref.iterator();
            }
        }
        return m;
    }
}
