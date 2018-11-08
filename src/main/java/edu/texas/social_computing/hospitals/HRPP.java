package edu.texas.social_computing.hospitals;

import com.google.common.collect.ImmutableList;

import java.util.*;

public class HRPP {
    private ResidentTable residentTable;
    private HospitalTable hospitalTable;
    private Matching matching;

    public void run(HospitalTable hospitalTable, ResidentTable residentTable) {
        this.hospitalTable = hospitalTable;
        this.residentTable = residentTable;

        // run hospital-resident matching alg (couple agnostic)
        ImmutableList<Resident> initialQueue = this.residentTable.getAll();
        this.matching = HRP.run(this.hospitalTable, this.residentTable, new LinkedList<>(initialQueue));

        // check all couples for proximity violations (location mismatch)
        Set<Resident> freeViolatingResidents = matching.getNDProximityViolators(residentTable.getAll(), residentTable);
        Queue<Resident> unmatchedQueue = new LinkedList<>(matching.getAllUnassigned(this.residentTable.getAll()));
        Queue<Resident> violatingResidentsQ = new LinkedList<>(freeViolatingResidents);

        // while (couple proximity violations exist)
        while(!violatingResidentsQ.isEmpty()) {

            // pick non-dominant partner (not matched partner or partner with worse preference match)
            Resident ndResident = violatingResidentsQ.poll();
            Resident partner = residentTable.getResidentById(ndResident.getPartnerId());

            // remove all hospitals violating proximity constraint (make a copy dont modify original preference)
            ndResident.setPrefsByLocation(this.matching.getAssignedHospital(partner), this.hospitalTable);

            // put back in the queue
            ((LinkedList<Resident>) unmatchedQueue).addFirst(ndResident);
            // try to match that person
            this.matching = HRP.run(this.hospitalTable, this.residentTable, unmatchedQueue);
            // if matched -> good
            if(matching.hasAssignment(ndResident)) {
                continue;
            }
        }
    }

        // Task 2
        // if not matched

            // unmatch BOTH in the couple and add both back to the queue (now no one is dominant)

            // reset non-dominant partner's preference list

            // start from dominant partner's next hospital preference (changing the rank pointer)

}
