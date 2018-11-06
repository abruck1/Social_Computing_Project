package edu.texas.social_computing.hospitals;

import java.util.Set;

public class HRPP {
    private ResidentTable residentTable;
    private HospitalTable hospitalTable;
    private Matching matching;

    // run hospital-resident matching alg (couple agnostic)
    public void run(HospitalTable hospitalTable, ResidentTable residentTable) {
        this.hospitalTable = hospitalTable;
        this.residentTable = residentTable;

        this.matching = new HRP().run(this.hospitalTable, this.residentTable);

        // check all couples for proximity violations (location mismatch)
        Set<Resident> freeViolatingResidents = matching.getNDProximityViolators(residentTable.getAll(), residentTable);
    }

    // while (couple proximity violations exist)
    // Task 1

        // pick non-dominant partner (not matched partner or partner with worse preference match)

        // remove all hospitals violating proximity constraint (make a copy dont modify original preference)

        // put back in the queue

        // try to match that person

        // if matched -> good

        // Task 2
        // if not matched

            // unmatch BOTH in the couple and add both back to the queue (now no one is dominant)

            // reset non-dominant partner's preference list

            // start from dominant partner's next hospital preference (changing the rank pointer)

}
