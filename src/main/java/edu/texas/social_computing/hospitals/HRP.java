package edu.texas.social_computing.hospitals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Stream;

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
                               Queue<Resident> freeResidents,
                               Matching existingMatching) {
        Matching m = existingMatching;

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
            if (residentsPrefs.get(currentResident).isEmpty()) {
                continue;
            }
            List<String> currentResidentPref = residentsPrefs.get(currentResident);
            Hospital hospital = hospitalTable.getHospitalById(currentResidentPref.get(0));
            residentsPrefs.get(currentResident).remove(hospital.getId());

            m.assign(currentResident, hospital);

            if (m.isOverSubscribed(hospital)) {
                Resident worstResident = m.getWorstAssignedResident(hospital);
                m.unassign(worstResident, hospital);
                if (!residentsPrefs.get(worstResident).isEmpty() && !worstResident.equals(currentResident)) {
                    freeResidents.add(worstResident);
                }
            }
            if (m.isFull(hospital)) {
                List<String> badResidentIds = hospital.getWorseThanIds(m.getWorstAssignedResident(hospital));
                List<String> nonRankedResidentIds = getNonRankedIds(hospital, residentTable.getAll());

                Stream.of(badResidentIds, nonRankedResidentIds).flatMap(Collection::stream)
                        .forEach(resId -> {
                            hospitalsPrefs.remove(resId);
                            Resident badResident = residentTable.getResidentById(resId);
                            residentsPrefs.get(badResident).remove(hospital.getId());
                        });

            }
            // if currentResident has more applying to do, put back in Q
            if (!m.hasAssignment(currentResident)) {
                freeResidents.add(currentResident);
            }
        }
        return m;
    }

    public static Matching run(HospitalTable hospitalTable,
                               ResidentTable residentTable,
                               Queue<Resident> freeResidents) {
        return run(hospitalTable, residentTable, freeResidents, new Matching());
    }

    public static void main(String[] args) throws FileNotFoundException {
        List<Resident> residents = FileImporter.importResidents(args[0]);
        List<Hospital> hospitals = FileImporter.importHospitals(args[1]);
        System.out.println("loaded Files");
        System.out.println("residents: " + Integer.toString(residents.size()));
        System.out.println("hospitals: " + Integer.toString(hospitals.size()));

        // make lookup tables
        HospitalTable hospitalTable = HospitalTable.create(hospitals);
        ResidentTable residentTable = ResidentTable.create(residents);
        System.out.println("generated tables");

        // call HRP
        Matching m = HRP.run(hospitalTable, residentTable, new ArrayDeque<>(residentTable.getAll()));
        System.out.println(m);


        Set<String> unassignedIds = m.getAllUnassigned(residentTable.getAll()).stream()
                .map(Resident::getId)
                .collect(ImmutableSet.toImmutableSet());
        System.out.println("Unassigned: " + unassignedIds);
    }

    private static List<String> getNonRankedIds(Hospital h, List<Resident> residents) {
        return residents.stream()
                .filter(resident -> !h.isRanked(resident))
                .map(Resident::getId)
                .collect(ImmutableList.toImmutableList());
    }
}
