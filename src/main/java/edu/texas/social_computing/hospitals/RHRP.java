package edu.texas.social_computing.hospitals;

import com.google.common.collect.ImmutableList;

import java.io.FileNotFoundException;
import java.util.*;

public class RHRP {
    private RHRP() {
    }

    /**
     * Real HRP with Couples From http://www.nrmp.org/couples-match-videos/
     * - Treat couples as a unit
     * - Only make a match if the hospital is on resident's pref list and resident is on hospital's
     * - If one partner has failed to match, increment the other's pref rank
     * - If one partner has been kicked out due to being the worst, then break the other partner's match (if it has one)
     * - Assumes that both partners have same pref list size
     */

    public static Matching run(HospitalTable hospitalTable,
                               ResidentTable residentTable,
                               Queue<Resident> freeResidents,
                               Matching existingMatching) {
        Matching m = existingMatching;

        while (!freeResidents.isEmpty()) {
            Resident currentResident = freeResidents.poll();
            if (m.hasAssignment(currentResident)) continue;

            // if the current resident has no more preferences move to next in q
            int residentRankProgress = residentTable.getResidentRankProgress(currentResident);
            if (currentResident.getPreferences().isEmpty() || residentRankProgress >= currentResident.getPreferences().size()) {
                continue;
            }

            // get current resident's most preferred hospital
            Hospital hospital = hospitalTable.getHospitalById(currentResident.getPreferences().get(residentRankProgress));
            residentTable.incrementResidentRankProgress(currentResident);

            if (!currentResident.hasPartner()) {
                tryToMatch(hospital, currentResident, residentTable, freeResidents, m);
            }

            // if current resident has a partner, update partner's pref to their most preferred hospital
            if (currentResident.hasPartner()) {
                // get the next hospital from partner's pref list and increment the rank progress
                // we always want this to happen unless the partner is out of preferences so the preference lists remain in lock step
                Resident partner = residentTable.getResidentById(currentResident.getPartnerId());
                int partnerRankProgress = residentTable.getResidentRankProgress(partner);
                if (!partner.getPreferences().isEmpty() && partnerRankProgress < partner.getPreferences().size()) {
                    Hospital partnerHospital = hospitalTable.getHospitalById(partner.getPreferences().get(residentTable.getResidentRankProgress(partner)));
                    residentTable.incrementResidentRankProgress(partner);
                    tryToMatchCouple(hospital, currentResident, partnerHospital, partner, residentTable, freeResidents, m);
                } else {
                    tryToMatch(hospital, currentResident, residentTable, freeResidents, m);
                }
            }

            giveSinglesAnotherChance(m, residentTable, hospitalTable, freeResidents);
            // if i have a partner check if either of us are unmatched
            if (currentResident.hasPartner()) {
                Resident partner = residentTable.getResidentById(currentResident.getPartnerId());
                // if current is not match OR partner is not matched AND has a preference then unassign both and add them to the q
                if (!m.hasAssignment(currentResident) || (!m.hasAssignment(partner) && !partner.getPreferences().isEmpty())) {
                    m.unassign(currentResident, hospital);
                    m.unassign(partner, m.getAssignedHospital(partner));
                    if (!freeResidents.contains(currentResident)) freeResidents.add(currentResident);
                    if (!freeResidents.contains(partner)) freeResidents.add(partner);
                }
                // if i dont have a partner check if im unmatched
            } else {
                if (!m.hasAssignment(currentResident) && !freeResidents.contains(currentResident)) {
                    freeResidents.add(currentResident);
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

//        String resFile = "./resources/test_hos5_loc2_res16_coup3_residents.csv";
//        String hosFile = "./resources/test_hos5_loc2_res16_coup3_hospitals.csv";

//        String resFile = "./resources/test_hos50_loc50_res100_coup50_residents.csv";
//        String hosFile = "./resources/test_hos50_loc50_res100_coup50_hospitals.csv";

//        String resFile = "./resources/test_hos50_loc50_res150_coup20_residents.csv";
//        String hosFile = "./resources/test_hos50_loc50_res150_coup20_hospitals.csv";

//        String resFile = "./resources/test_hos100_loc10_res200_coup50_residents.csv";
//        String hosFile = "./resources/test_hos100_loc10_res200_coup50_hospitals.csv";

        String resFile = "./resources/test_hos300_loc50_res500_coup100_residents.csv";
        String hosFile = "./resources/test_hos300_loc50_res500_coup100_hospitals.csv";
        List<Resident> residents = FileImporter.importResidents(resFile);
        List<Hospital> hospitals = FileImporter.importHospitals(hosFile);
        System.out.println("loaded Files");
        System.out.println("residents: " + Integer.toString(residents.size()));
        System.out.println("hospitals: " + Integer.toString(hospitals.size()));

        // make lookup tables
        HospitalTable hospitalTable = HospitalTable.create(hospitals);
        ResidentTable residentTable = ResidentTable.create(residents);
        System.out.println("generated tables");

        // call HRP
        Matching finalMatch = RHRP.run(hospitalTable, residentTable, new ArrayDeque<>(residentTable.getAll()));
        finalMatch.validateCapacities(hospitals);
        finalMatch.validateStability(residentTable, hospitalTable);

        finalMatch.outputMatchingToCsv("RHRP_testFix", residents, residentTable, hospitals);
        System.out.println("Done");
    }

    private static void tryToMatch(Hospital h, Resident resident,
                                   ResidentTable residentTable, Queue<Resident> freeResidents, Matching m) {

        // Only if the hospital has this resident on its pref list can it accept the resident
        if (h.getPreferences().contains(resident.getId())) {
            m.assign(resident, h);

            // if hospital now has too many residents
            if (m.isOverSubscribed(h)) {
                fixOverSubscribed(h, freeResidents, residentTable, m);
            }
        }
    }

    private static void tryToMatchCouple(Hospital h, Resident currentResident, Hospital partnerHospital,
                                         Resident partner, ResidentTable residentTable,
                                         Queue<Resident> freeResidents, Matching m) {
        // if both residents are candidates for the hospital
        // and neither are the worst
        // and they wont kick each other out if its the same hospital
        // then assign both
        // else put them back in the queue if they arent in it
        if (h.getPreferences().contains(currentResident.getId()) && partnerHospital.getPreferences().contains(partner.getId())) {
            if (!h.getId().equals(partnerHospital.getId())) {
                if (m.isRankedHigherThanWorstMatch(h, currentResident.getId()) && m.isRankedHigherThanWorstMatch(partnerHospital, partner.getId())) {
                    m.assign(currentResident, h);
                    m.assign(partner, partnerHospital);
                    if (m.isOverSubscribed(h)) {
                        fixOverSubscribed(h, freeResidents, residentTable, m);
                    }
                    if (m.isOverSubscribed(partnerHospital)) {
                        fixOverSubscribed(partnerHospital, freeResidents, residentTable, m);
                    }
                    return;
                }
            } else {
                if (!worstTwoResidents(h, currentResident, partner, m)) {
                    m.assign(currentResident, h);
                    m.assign(partner, partnerHospital);
                    if (m.isOverSubscribed(h)) {
                        fixOverSubscribed(h, freeResidents, residentTable, m);
                    }
                    if (m.isOverSubscribed(partnerHospital)) {
                        fixOverSubscribed(partnerHospital, freeResidents, residentTable, m);
                    }
                    return;
                }
            }
        } else {
            if (!freeResidents.contains(currentResident)) freeResidents.add(currentResident);
            if (!freeResidents.contains(partner)) freeResidents.add(partner);
        }
    }

    private static boolean worstTwoResidents(Hospital hospital, Resident r1, Resident r2, Matching m) {
        ImmutableList<Resident> residents =
                ImmutableList.<Resident>builder()
                        .addAll(m.getAssignedResidents(hospital))
                        .add(r1)
                        .add(r2)
                        .build();
        List<Resident> reverseRankSortedResidents = residents.stream()
                .sorted(Comparator.comparing(hospital::rankOf))
                .collect(ImmutableList.toImmutableList())
                .reverse();

        // if either the violating resident or its partner are in one of the first two positions of this
        // list, i.e. are either the worst or second worst ranked of this group by this hospital,
        // then there wasn't room for both at this hospital, and not a violation
        return reverseRankSortedResidents.indexOf(r1) < 2 || reverseRankSortedResidents.indexOf(r2) < 2;
    }

    private static void fixOverSubscribed(Hospital h, Queue<Resident> freeResidents, ResidentTable residentTable, Matching m) {
        Resident worstResident = m.getWorstAssignedResident(h);

        // unassign the worst resident then add them back to the queue
        m.unassign(worstResident, h);
        if (!freeResidents.contains(worstResident)) freeResidents.add(worstResident);

        if (worstResident.hasPartner()) {
            // unassign the worst resident's partner and add the partner back to the queue
            Resident worstResidentPartner = residentTable.getResidentById(worstResident.getPartnerId());
            m.unassign(worstResidentPartner, m.getAssignedHospital(worstResidentPartner));
            if (!freeResidents.contains(worstResidentPartner)) freeResidents.add(worstResidentPartner);
        }
    }

    private static void giveSinglesAnotherChance(
            Matching matching,
            ResidentTable residentTable,
            HospitalTable hospitalTable,
            Queue<Resident> unmatchedQueue) {

        residentTable.getAll().stream()
                .filter(resident -> !resident.hasPartner())
                .filter(matching::hasAssignment)
                .filter(resident -> HRPP.canDoBetter(matching, resident, hospitalTable))
                .forEach(resident -> {
                    residentTable.resetResidentRankProgress(resident);
                    matching.unassign(resident, matching.getAssignedHospital(resident));
                    unmatchedQueue.add(resident);
                });
    }
}
