package edu.texas.social_computing.hospitals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;

public class RHRP {
    private RHRP() {
    }

    /**
     * Real HRP with Couples From http://www.nrmp.org/couples-match-videos/
     * - Treat couples as a unit
     * - Only make a match if the hospital is on resident's pref list and resident is on hospital's
     * - If one partner has failed to match, increment the other's pref rank
     * - If one partner has been kicked out due to being the worst, then break the other partner's match (it it has one)
     * - Assumes that both partners have same pref list size
     */

    public static Matching run(HospitalTable hospitalTable,
                               ResidentTable residentTable,
                               Queue<Resident> freeResidents,
                               Matching existingMatching) {
        Matching m = existingMatching;

        while (!freeResidents.isEmpty()) {
            Resident currentResident = freeResidents.poll();
            if (currentResident.getPreferences().isEmpty()) {
                if (!currentResident.hasPartner()) {
                    continue;
                }
                if (residentTable.getResidentById(currentResident.getPartnerId()).getPreferences().isEmpty()) {
                    continue;
                }
                // resident and partner have to have the same pref list size
                throw new IllegalArgumentException(String.format("Partners [%s] and [%s] must have the same length preference list.", currentResident.getId(), currentResident.getPartnerId()));
            }

            Hospital hospital = hospitalTable.getHospitalById(currentResident.getPreferences()
                    .get(residentTable.getResidentRankProgress(currentResident)));
            residentTable.incrementResidentRankProgress(currentResident);

            tryToMatch(hospital,currentResident,residentTable,freeResidents,m);

            // if the currentResident managed to get assigned then try to assign partner if he has one
            if (m.hasAssignment(currentResident) && currentResident.hasPartner()) {
                tryToMatch(hospital,residentTable.getResidentById(currentResident.getPartnerId()),
                        residentTable,freeResidents,m);
            }

            // if currentResident has more applying to do, put back in Q
            if (!m.hasAssignment(currentResident)) {
                freeResidents.add(currentResident);
                if (currentResident.hasPartner()) {
                    freeResidents.add(residentTable.getResidentById(currentResident.getPartnerId()));
                }
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
        Matching m = RHRP.run(hospitalTable, residentTable, new ArrayDeque<>(residentTable.getAll()));
        System.out.println(m);


        Set<String> unassignedIds = m.getAllUnassigned(residentTable.getAll()).stream()
                .map(Resident::getId)
                .collect(ImmutableSet.toImmutableSet());
        System.out.println("Unassigned: " + unassignedIds);
    }

    private static List<String> getNonRankedIds(Hospital h, List<Resident> residents) {
        return residents.stream()
                .filter(res -> h.rankOf(res) == Integer.MAX_VALUE)
                .map(Resident::getId)
                .collect(ImmutableList.toImmutableList());
    }

    private static void tryToMatch(Hospital h, Resident resident,
                                   ResidentTable residentTable, Queue<Resident> freeResidents, Matching m) {
        // Only if the hospital has this resident on its pref list can it accept the resident
        if (h.getPreferences().contains(resident.getId())) {
            m.assign(resident, h);
            if (m.isOverSubscribed(h)) {
                Resident worstResident = m.getWorstAssignedResident(h);
                m.unassign(worstResident);
                if (!worstResident.getPreferences().isEmpty() && !worstResident.equals(resident)) {
                    freeResidents.add(worstResident);
                }
                if (worstResident.hasPartner()) {
                    Resident worstResidentPartner = residentTable.getResidentById(worstResident.getPartnerId());
                    m.unassign(worstResidentPartner);
                    if (!worstResidentPartner.getPreferences().isEmpty()
                            && !worstResident.equals(resident)) {
                        // FIXME Problem here! During the partner's call worstResident.equals(resident) check
                        //  needs to be modified
                        freeResidents.add(worstResidentPartner);
                    }
                }
            }
        } else {
            if (resident.hasPartner()) {
                if (m.hasAssignment(residentTable.getResidentById(resident.getPartnerId()))) {
                    m.unassign(residentTable.getResidentById(resident.getPartnerId()));
                }
                residentTable.incrementResidentRankProgress(residentTable.getResidentById(resident.getPartnerId()));
            }
        }
    }
}
