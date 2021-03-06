package edu.texas.social_computing.hospitals;

import com.google.common.collect.Sets;
import static com.google.common.base.Preconditions.checkState;

import java.util.*;

import static com.google.common.base.Preconditions.checkState;

public class HRPP {

    public static Matching run(HospitalTable hospitalTable, ResidentTable residentTable) {
        // run hospital-resident matching alg (couple agnostic)
        Deque<Resident> initialQueue = new ArrayDeque<>(residentTable.getAll());
        Matching matching = HRP.run(hospitalTable, residentTable, initialQueue);

        // check all couples for proximity violations (location mismatch)
        Deque<Resident> unmatchedQueue = new ArrayDeque<>(matching.getAllUnassigned(residentTable.getAll()));
        Deque<Resident> violatingResidentsQ = computeViolatingResidentQ(matching, residentTable);

        // while (couple proximity violations exist)
        while (!violatingResidentsQ.isEmpty()) {

            // pick non-dominant partner (not matched partner or partner with worse preference match)
            Resident ndResident = violatingResidentsQ.poll();
            Resident partner = residentTable.getResidentById(ndResident.getPartnerId());

            // remove all hospitals violating proximity constraint (make a copy dont modify original preference)
            ndResident.setPrefsByLocation(matching.getAssignedHospital(partner).getLocationId(), hospitalTable);

            // put back in the queue
            matching.unassign(ndResident, matching.getAssignedHospital(ndResident));
            unmatchedQueue.add(ndResident);

            // try to match that person
            HRP.run(hospitalTable, residentTable, unmatchedQueue, matching);

            // reset non-dominant partner's preference list
            ndResident.setPrefsByProgress(residentTable.getResidentRankProgress(ndResident));

            // if matched -> good
            if (matching.hasAssignment(ndResident)) {
                updateViolatingResidentQ(matching, residentTable, violatingResidentsQ);
                giveSinglesAnotherChance(matching, residentTable, hospitalTable, unmatchedQueue);
                continue;
            }

            giveSinglesAnotherChance(matching, residentTable, hospitalTable, unmatchedQueue);

            // Task 2
            // if partners still not proximally matched
            // unmatch BOTH in the couple and add both back to the queue (now no one is dominant)
            matching.unassign(ndResident, matching.getAssignedHospital(ndResident));
            matching.unassign(partner, matching.getAssignedHospital(partner));

            // add both partners back to queue
            unmatchedQueue.add(ndResident);
            unmatchedQueue.add(partner);

            // start from dominant partner's next hospital preference (changing the rank pointer)
            residentTable.incrementResidentRankProgress(partner);
            partner.setPrefsByProgress(residentTable.getResidentRankProgress(partner));

            // run again
            HRP.run(hospitalTable, residentTable, unmatchedQueue, matching);

            // check for proximity violations again ... could be some new ones
            updateViolatingResidentQ(matching, residentTable, violatingResidentsQ);

            // make sure all unassigned residents (for whatever reason) are added back into the queue for consideration
            unmatchedQueue.addAll(matching.getAllUnassigned(residentTable.getAll()));

            // give the singles another chance
            giveSinglesAnotherChance(matching, residentTable, hospitalTable, unmatchedQueue);
            printProgress(residentTable);
        }

        System.out.println("Size of violating Q: " + violatingResidentsQ.size());

        return matching;
    }

    private static Deque<Resident> computeViolatingResidentQ(Matching matching, ResidentTable residentTable) {
        Set<Resident> freeViolatingResidents = matching.getNDProximityViolators(residentTable.getAll(), residentTable);
        return new ArrayDeque<>(freeViolatingResidents);
    }

    private static void updateViolatingResidentQ(
            Matching matching, ResidentTable residentTable, Deque<Resident> violatingResidentsQ) {
        Set<Resident> newViolators = new HashSet<>(computeViolatingResidentQ(matching, residentTable));
        Set<Resident> oldViolators = new HashSet<>(violatingResidentsQ);
        violatingResidentsQ.addAll(Sets.difference(newViolators, oldViolators));
    }

    public static void giveSinglesAnotherChance(
            Matching matching,
            ResidentTable residentTable,
            HospitalTable hospitalTable,
            Queue<Resident> unmatchedQueue) {

        residentTable.getAll().stream()
                .filter(resident -> !resident.hasPartner())
                .filter(matching::hasAssignment)
                .filter(resident -> canDoBetter(matching, resident, hospitalTable))
                .forEach(resident -> {
                    matching.unassign(resident, matching.getAssignedHospital(resident));
                    unmatchedQueue.add(resident);
                });
    }

    public static boolean canDoBetter(Matching matching, Resident resident, HospitalTable hospitalTable) {
        Hospital assignedHospital = matching.getAssignedHospital(resident);
        int rankOfAssigned = resident.rankOf(assignedHospital);
        checkState(rankOfAssigned < resident.getPreferences().size(),
                "%s >= %s", rankOfAssigned, resident.getPreferences().size());

        for(int i=0; i<rankOfAssigned; i++) {
            Hospital nextHospital = hospitalTable.getHospitalById(resident.getPreferences().get(i));
            if(matching.isRankedHigherThanWorstMatch(nextHospital, resident.getId())) {
                return true;
            }
        }
        return false;
    }

    private static void printProgress(ResidentTable residentTable) {
        Optional<Integer> totalRank = residentTable.getAll().stream()
                .map(residentTable::getResidentRankProgress)
                .reduce((rank1, rank2) -> rank1 + rank2);

        Optional<Integer> maxtotalRank = residentTable.getAll().stream()
                .map(res -> res.getInitialPreferences().size())
                .reduce((size1, size2) -> size1 + size2);

        if (totalRank.get() % 10 == 0) {
            System.out.println(totalRank.get() + "/" + maxtotalRank.get());
        }
    }
}
