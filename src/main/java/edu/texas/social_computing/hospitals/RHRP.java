package edu.texas.social_computing.hospitals;

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

            tryToMatch(hospital, currentResident, residentTable, freeResidents, m);

            // if current resident has a partner, update partner's pref to their most preferred hospital
            if (currentResident.hasPartner()) {
                // get the next hospital from partner's pref list and increment the rank progress
                // we always want this to happen unless the partner is out of preferences so the preference lists remain in lock step
                Resident partner = residentTable.getResidentById(currentResident.getPartnerId());
                int partnerRankProgress = residentTable.getResidentRankProgress(partner);
                if (!partner.getPreferences().isEmpty() && partnerRankProgress < partner.getPreferences().size()) {
                    Hospital partnerHospital = hospitalTable.getHospitalById(partner.getPreferences().get(residentTable.getResidentRankProgress(partner)));
                    residentTable.incrementResidentRankProgress(partner);

                    // if current resident got assigned then try to match their partner
                    if (m.hasAssignment(currentResident)) {
                        tryToMatch(partnerHospital, partner, residentTable, freeResidents, m);
                    }
                }
            }

            // if i have a partner check if either of us are unmatched
            if (currentResident.hasPartner()) {
                Resident partner = residentTable.getResidentById(currentResident.getPartnerId());
                // if current is not match OR partner is not matched AND has a preference then unassign both and add them to the q
                if (!m.hasAssignment(currentResident) || (!m.hasAssignment(partner) && !partner.getPreferences().isEmpty())) {
                    m.unassign(currentResident);
                    m.unassign(partner);
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
        String resfile = "resources/test_hos5_loc2_res16_coup3_residents.csv";
        String hosfile = "resources/test_hos5_loc2_res16_coup3_hospitals.csv";
        List<Resident> residents = FileImporter.importResidents(resfile);
        List<Hospital> hospitals = FileImporter.importHospitals(hosfile);
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
        finalMatch.validateStability(residents, hospitalTable, residentTable);

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
                Resident worstResident = m.getWorstAssignedResident(h);

                // unassign the worst resident then add them back to the queue
                m.unassign(worstResident);
                if (!freeResidents.contains(worstResident)) freeResidents.add(worstResident);

                if (worstResident.hasPartner()) {
                    // unassign the worst resident's partner and add the partner back to the queue
                    Resident worstResidentPartner = residentTable.getResidentById(worstResident.getPartnerId());
                    m.unassign(worstResidentPartner);
                    if (!freeResidents.contains(worstResidentPartner)) freeResidents.add(worstResidentPartner);
                }
            }
        }
    }
}
