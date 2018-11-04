package edu.texas.social_computing.hospitals;

public class HRPP {
    // run hospital-resident matching alg (couple agnostic)

    // check all couples for proximity violations (location mismatch)

    // while (couple proximity violations exist)

    // pick non-dominant partner (not matched partner or partner with worse preference match)

        // remove all hospitals violating proximity constraint (make a copy dont modify original preference)

        // put back in the queue

        // try to match that person

        // if matched -> good

        // if not matched

            // unmatch BOTH in the couple and add both back to the queue (now no one is dominant)

            // reset non-dominant partner's preference list

            // start from dominant partner's next hospital preference
}
