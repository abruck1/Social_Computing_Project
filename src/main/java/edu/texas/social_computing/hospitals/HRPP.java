package edu.texas.social_computing.hospitals;

import com.google.common.collect.ImmutableList;

import java.util.*;

public class HRPP {

    public static void run(HospitalTable hospitalTable, ResidentTable residentTable) {
        // run hospital-resident matching alg (couple agnostic)
        ImmutableList<Resident> initialQueue = residentTable.getAll();
        Matching matching = HRP.run(hospitalTable, residentTable, new LinkedList<>(initialQueue));

        // check all couples for proximity violations (location mismatch)
        Set<Resident> freeViolatingResidents = matching.getNDProximityViolators(residentTable.getAll(), residentTable);
        Queue<Resident> unmatchedQueue = new LinkedList<>(matching.getAllUnassigned(residentTable.getAll()));
        Queue<Resident> violatingResidentsQ = new LinkedList<>(freeViolatingResidents);

        // while (couple proximity violations exist)
        while (!violatingResidentsQ.isEmpty()) {

            // pick non-dominant partner (not matched partner or partner with worse preference match)
            Resident ndResident = violatingResidentsQ.poll();
            Resident partner = residentTable.getResidentById(ndResident.getPartnerId());

            // remove all hospitals violating proximity constraint (make a copy dont modify original preference)
            ndResident.setPrefsByLocation(matching.getAssignedHospital(partner).getLocationId(), hospitalTable);

            // put back in the queue
            ((LinkedList<Resident>) unmatchedQueue).addFirst(ndResident);
            // try to match that person
            matching = HRP.run(hospitalTable, residentTable, unmatchedQueue);
            // if matched -> good
            if (matching.hasAssignment(ndResident)) {
                continue;
            }

            // Task 2
            // if partners still not proximally matched
            // unmatch BOTH in the couple and add both back to the queue (now no one is dominant)
            matching.unassign(ndResident);
            matching.unassign(partner);

            // ndResident should still be in queue, right???
            unmatchedQueue.add(partner);

            // reset non-dominant partner's preference list
            ndResident.resetPreferences();

            // start from dominant partner's next hospital preference (changing the rank pointer)
            residentTable.incrementResidentRankProgress(partner);
            partner.setPrefsByProgress(residentTable.getResidentRankProgress(partner));

            // run again
            matching = HRP.run(hospitalTable, residentTable, unmatchedQueue);
        }
    }
}
