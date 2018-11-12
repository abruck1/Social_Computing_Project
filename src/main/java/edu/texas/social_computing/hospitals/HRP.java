package edu.texas.social_computing.hospitals;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;

public class HRP {
    private HRP() {
    }

    /**
     * From: The Hospitals / Residents Problem (1962; Gale, Shapley)
     * David F. Manlove
     * https://pdfs.semanticscholar.org/77d9/f84082674888ca90ca662847983381b23338.pdf?_ga=2.216999441.622816506.1541285838-70410261.1541285838
     * <p>
     * M := ∅;
     * while (some resident r_i is unassigned and r_i has a non-empty list) {
     * h_j := first hospital on r_i’s list;
     * // r_i applies to h_j
     * M := M ∪ {(r_i, h_j)};
     * if (h_j is over-subscribed) {
     * r_k := worst resident in M(h_j) according to h_j’s list;
     * M := M\{(r_k, h_j)};
     * }
     * if (h_j is full) {
     * r_k := worst resident in M(h_j) according to h_j’s list;
     * for (each successor r_l of r_k on h_j’s list)
     * delete the pair (r_l, h_j);
     * }
     * }
     */

    public static Matching run(HospitalTable hospitalTable,
                               ResidentTable residentTable,
                               Queue<Resident> freeResidents) {
        Matching m = new Matching();

        Map<Hospital, List<String>> hospitalsPrefs = new HashMap<>();
        for (Hospital hospital : hospitalTable.getAll()) {
            hospitalsPrefs.put(hospital, newArrayList(hospital.getPreferences()));
        }

        Map<Resident, List<String>> residentsPrefs = new HashMap<>();
        for (Resident resident : residentTable.getAll()) {
            residentsPrefs.put(resident, newArrayList(resident.getPreferences()));
        }

        while (!freeResidents.isEmpty()) {
            Resident currentResident = freeResidents.poll();
            if(residentsPrefs.get(currentResident).isEmpty()) {
                continue;
            }
            List<String> currentResidentPref = residentsPrefs.get(currentResident);
            Hospital hospital = hospitalTable.getHospitalById(currentResidentPref.get(0));

            m.assign(currentResident, hospital);
            if (m.isOverSubscribed(hospital)) {
                Resident worstResident = m.getWorstAssignedResident(hospital);
                m.unassign(worstResident);
                if(!residentsPrefs.get(worstResident).isEmpty()) {
                    freeResidents.add(worstResident);
                }
            }
            if (m.isFull(hospital)) {
                List<String> badResidentIds = hospital.getWorseThanIds(m.getWorstAssignedResident(hospital));
                if (!badResidentIds.isEmpty()) {
                    hospitalsPrefs.get(hospital).removeAll(badResidentIds);

                    for (String badId : badResidentIds) {
                        Resident badResident = residentTable.getResidentById(badId);
                        residentsPrefs.get(badResident).remove(hospital.getId());
                    }
                }

            }

            // if currentResident has more applying to do, put back in Q
            if (!m.hasAssignment(currentResident)) {
                freeResidents.add(currentResident);
            }
        }
        return m;
    }
}
