package edu.texas.social_computing.hospitals;

import com.google.common.collect.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Matching {

    private static final Hospital NO_MATCH = Hospital.create("NO_MATCH", -1, 0, ImmutableList.of());

    private Multimap<Hospital, Resident> hospitalAssignments = HashMultimap.create();
    private Map<Resident, Hospital> residentAssignments = new HashMap<>();

    public void assign(Resident r, Hospital h) {
        hospitalAssignments.put(h, r);
        residentAssignments.put(r, h);
    }

    public void unassign(Resident r, Hospital h) {
        hospitalAssignments.remove(h, r);
        residentAssignments.remove(r);
    }

    public ImmutableList<Resident> getAssignedResidents(Hospital h) {
        return ImmutableList.copyOf(hospitalAssignments.get(h));
    }

    public Hospital getAssignedHospital(Resident r) {
        return residentAssignments.getOrDefault(r, NO_MATCH);
    }

    public boolean hasAssignment(Resident r) {
        return residentAssignments.containsKey(r);
    }

    public boolean isOverSubscribed(Hospital h) {
        return getAssignedResidents(h).size() > h.getCapacity();
    }

    public boolean isFull(Hospital h) {
        return getAssignedResidents(h).size() == h.getCapacity();
    }

    public Resident getWorstAssignedResident(Hospital h) {
        return hospitalAssignments.get(h).stream()
                .max(Comparator.comparing(h::rankOf))
                .orElse(null);
    }

    public Set<Resident> getAllUnassigned(List<Resident> residents) {
        return residents.stream()
                .filter(res -> !hasAssignment(res))
                .collect(ImmutableSet.toImmutableSet());
    }

    /**
     * @param residents : a list of Non-Dominant (ND) residents in a matching that needs to be checked to see
     *                  if any of the residents in the list violate a proximity constraint
     * @return a set of residents that violate the proximity constraint AND are the worse placed partner
     */
    public Set<Resident> getNDProximityViolators(List<Resident> residents, ResidentTable residentTable) {
        Set<Resident> freeViolatingResidents = new HashSet<>();
        for (Resident resident : residents) {
            if (!resident.hasPartner()) continue;
            Resident partner = residentTable.getResidentById(resident.getPartnerId());
            if (!hasAssignment(resident)) continue;
            if (!hasAssignment(partner)) {
                freeViolatingResidents.add(partner);
                continue;
            }
            Hospital assignedHospital = this.getAssignedHospital(resident);
            Hospital partnerAssignedHospital = this.getAssignedHospital(partner);
            int location = assignedHospital.getLocationId();
            int partnerLocation = partnerAssignedHospital.getLocationId();
            if (location != partnerLocation) {
                // if there is a tie for worse placed partner we only want 1 to be non-dominant
                if (!freeViolatingResidents.contains(partner)) {
                    Resident ndPartner = MatchingUtils.worsePlacedResident(this, resident, partner);
                    freeViolatingResidents.add(ndPartner);
                    unassign(ndPartner, getAssignedHospital(ndPartner));
                }
            }
        }
        return freeViolatingResidents;
    }

    public void validateProximities(List<Resident> residents, ResidentTable residentTable) {
        List<String> violations = new ArrayList<>();
        for (Resident resident : residents) {
            if (resident.hasPartner()) {
                String partnerId = resident.getPartnerId();
                Resident partner = residentTable.getResidentById(partnerId);
                Hospital residentAssignment = getAssignedHospital(resident);
                Hospital partnerAssignment = getAssignedHospital(partner);
                if (residentAssignment.getLocationId() != partnerAssignment.getLocationId()) {
                    violations.add(resident.getId() + ", " + partnerId + " contains a proximity violation");
                }
            }
        }
        if (violations.size() == 0) {
            System.out.println("Passes proximity validation");
        } else {
            for (String v : violations) {
                System.out.println(v);
            }
        }
    }

    public void validateCapacities(List<Hospital> hospitals) {
        List<String> violations = new ArrayList<>();
        for (Hospital hospital : hospitals) {
            Integer assignmentSize = getAssignedResidents(hospital).size();
            Integer capacity = hospital.getCapacity();
            if (assignmentSize > capacity) {
                violations.add(hospital.getId() + " exceeds capacity: " + assignmentSize + " > " + capacity);
            }
        }
        if (violations.size() == 0) {
            System.out.println("Passes capacity validation");
        } else {
            for (String v : violations) {
                System.out.println(v);
            }
        }
    }

    public boolean isRankedHigherThanWorstMatch(Hospital hospital, String residentId) {
        Resident worstMatch = getWorstAssignedResident(hospital);
        List<String> hosPrefs = hospital.getPreferences();
        if (!hosPrefs.contains(residentId)) {
            return false;
        }
        if (worstMatch == null) {
            return true;
        }
        int hosResPrefIndex = hosPrefs.indexOf(residentId);
        int hosWorstPrefIndex = hosPrefs.indexOf(worstMatch.getId());
        return hosResPrefIndex < hosWorstPrefIndex;
    }

    public void validateStability(ResidentTable residentTable, HospitalTable hospitalTable) {
        // step 1: find all classical blocking pairs
        // these are the stability violations found when considering all residents as singles
        Multimap<String, String> violations = findSingleStyleViolations(residentTable, hospitalTable);

        // step 2: if in a couple, determine if blocking pairs can be resolved without a proximity violation. If they
        // can, then this is a real stability violation. If not in a couple, then this is a real violation
        boolean violation = hasActualStabilityViolations(violations, residentTable, hospitalTable);

        if (!violation) {
            System.out.println("Passes stability validation");
        }
    }

    // Find any classical stability violations. That is, find any blocking pairs (R, H) where
    // R prefers H to R's current assignment, and H prefers R to H's current worst resident.
    private Multimap<String, String> findSingleStyleViolations(
            ResidentTable residentTable, HospitalTable hospitalTable) {
        Multimap<String, String> violations = HashMultimap.create();

        for (Resident resident : residentTable.getAll()) {
            String resId = resident.getId();
            List<String> resPrefs = resident.getInitialPreferences();

            Hospital residentAssignment = getAssignedHospital(resident);
            int resPrefIndex = resident.rankOf(residentAssignment);
            if (residentAssignment.isRanked(resident) && resPrefIndex > 0) {
                for (int i = 0; i < resPrefIndex; i++) {
                    Hospital h = hospitalTable.getHospitalById(resPrefs.get(i));
                    if (isRankedHigherThanWorstMatch(h, resId)) {
                        violations.put(resId, h.getId());
                    }
                }
            }
        }
        return violations;
    }

    // Determine whether a set of classical, single-style violations contains any true violations, accounting for
    // partners. A partner stability violation occurs if two partners R1 and R2, who are currently matched to
    // hospitals H1 and H2, could be matched to two different hospitals H1' and H2' such that (R1, H1') and (R2, H2')
    // are classical blocking pairs, and H1' and H2' are in the same location. In the special case that H1'==H2', then
    // H1' must prefer both R1 and R2 to the current worst two residents.
    private boolean hasActualStabilityViolations(
            Multimap<String, String> violations, ResidentTable residentTable, HospitalTable hospitalTable) {
        boolean violation = false;
        for (String violatingResidentId : violations.keySet()) {
            Resident violatingResident = residentTable.getResidentById(violatingResidentId);
            if (!violatingResident.hasPartner()) {
                System.out.println(
                        "Single resident stability violation: "
                                + violatingResidentId + " - "
                                + ImmutableList.copyOf(violations.get(violatingResidentId)));
                violation = true;
            } else {
                String partnerId = violatingResident.getPartnerId();
                if (hasPartnerStabilityViolation(
                        violations,
                        residentTable.getResidentById(violatingResidentId),
                        residentTable.getResidentById(partnerId),
                        hospitalTable)) {
                    violation = true;
                }
            }
        }
        return violation;
    }

    private boolean hasPartnerStabilityViolation(
            Multimap<String, String> violations,
            Resident violatingResident,
            Resident partner,
            HospitalTable hospitalTable) {

        List<Hospital> violationHospitals = violations.get(violatingResident.getId()).stream()
                .map(hospitalTable::getHospitalById)
                .collect(ImmutableList.toImmutableList());

        List<Hospital> partnerViolationHospitals = violations.get(partner.getId()).stream()
                .map(hospitalTable::getHospitalById)
                .collect(ImmutableList.toImmutableList());

        boolean violation = false;
        for (Hospital violatingHospital : violationHospitals) {
            for (Hospital partnerViolatingHospital : partnerViolationHospitals) {
                if (violatingHospital.getLocationId() == partnerViolatingHospital.getLocationId()) {
                    // check special case of the same hospital, in which case if the partners are
                    // better than the worst two of the hospital's current assignment, then violation
                    if (violatingHospital.getId().equals(partnerViolatingHospital.getId())) {
                        ImmutableList<Resident> residents =
                                ImmutableList.<Resident>builder()
                                        .addAll(getAssignedResidents(violatingHospital))
                                        .add(violatingResident)
                                        .add(partner)
                                        .build();
                        List<Resident> reverseRankSortedResidents = residents.stream()
                                .sorted(Comparator.comparing(violatingHospital::rankOf))
                                .collect(ImmutableList.toImmutableList())
                                .reverse();

                        // if either the violating resident or its partner are in one of the first two positions of this
                        // list, i.e. are either the worst or second worst ranked of this group by this hospital,
                        // then there wasn't room for both at this hospital, and not a violation
                        if (reverseRankSortedResidents.indexOf(violatingResident) < 2 ||
                                reverseRankSortedResidents.indexOf(partner) < 2) {
                            continue;
                        }

                    }
                    System.out.println(String.format(
                            "Partner stability violation for residents (%s, %s) at location [%d], " +
                                    "preferred hospitals (%s, %s), current matches (%s, %s)",
                            violatingResident.getId(),
                            partner.getId(),
                            violatingHospital.getLocationId(),
                            violatingHospital.getId(),
                            partnerViolatingHospital.getId(),
                            getAssignedHospital(violatingResident).getId(),
                            getAssignedHospital(partner).getId()));
                    violation = true;
                }
            }
        }
        return violation;
    }

    private String getPrefString(List<String> preferences) {
        StringBuilder prefString = new StringBuilder();
        for (String p: preferences) {
            prefString.append(p).append(" ");
        }
        return prefString.toString().trim();
    }

    public void outputMatchingToCsv(String filePrefix, List<Resident> residents, ResidentTable residentTable, List<Hospital> hospitals) {
        List<String> resCSV = new ArrayList<>();
        List<String> hospCSV = new ArrayList<>();
        Charset utf8 = StandardCharsets.UTF_8;
        // add headers
        List<String> resHeaders = new ArrayList<>(Arrays.asList(
                "residentId",
                "residentAssignment",
                "partnerId",
                "partnerAssignment",
                "residentAssignmentLocation",
                "residentAssignmentRank",
                "residentInitialPreferences",
                "residentFinalPreferences",
                "hasPartner",
                "isAnchorPartner",
                "partnerAssignmentLocation",
                "partnerAssignmentRank",
                "partnerInitialPreferences",
                "partnerFinalPreferences"
        ));
        resCSV.add(String.join(", ", resHeaders));
        List<String> hospHeaders = new ArrayList<>(Arrays.asList(
                "hospitalId",
                "hospitalLocation",
                "capacity",
                "assignmentCount",
                "filledPercentage",
                "preferences",
                "assignments",
                "assignmentRanks"
        ));
        hospCSV.add(String.join(", ", hospHeaders));
        // get all Residents
        for (Resident resident : residents) {
            List<String> resInitPrefs = resident.getInitialPreferences();
            List<String> resPrefs = resident.getPreferences();
            Hospital residentAssignment = getAssignedHospital(resident);
            String partnerId = resident.getPartnerId();
            Resident partner = residentTable.getResidentById(partnerId);
            Hospital partnerAssignment = getAssignedHospital(partner);
            boolean hasPartner = resident.hasPartner();
            String isAnchorPartner;
            if (hasPartner) {
                Resident ndResident = MatchingUtils.worsePlacedResident(this, resident, partner);
                isAnchorPartner = ndResident.getId().equals(resident.getId()) ? "false" : "true";
            } else {
                isAnchorPartner = "NA";
            }

            // build output
            List<String> resRow = new ArrayList<>(Arrays.asList(
                    resident.getId(),
                    residentAssignment.getId(),
                    (hasPartner ? partnerId : "NA"),
                    (hasPartner ? partnerAssignment.getId() : "NA"),
                    Integer.toString(residentAssignment.getLocationId()),
                    Integer.toString(resident.rankOf(residentAssignment)),
                    getPrefString(resInitPrefs),
                    getPrefString(resPrefs),
                    (hasPartner ? "true" : "false"),
                    isAnchorPartner,
                    (hasPartner ? Integer.toString(partnerAssignment.getLocationId()) : "NA"),
                    (hasPartner ? Integer.toString(partner.rankOf(partnerAssignment)) : "NA"),
                    (hasPartner ? getPrefString(partner.getInitialPreferences()) : "NA"),
                    (hasPartner ? getPrefString(partner.getPreferences()) : "NA")
            ));
            resCSV.add(String.join(", ", resRow));
        }

        for (Hospital hospital : hospitals) {
            List<String> hosPrefs = hospital.getPreferences();
            List<Resident> assignments = getAssignedResidents(hospital);
            List<String> assignmentIds = new ArrayList<>();
            List<String> assignmentRanks = new ArrayList<>();
            Integer capacity = hospital.getCapacity();
            Integer assignmentCount = assignments.size();
            float percentFull = (float) assignmentCount / capacity;
            for (Resident res : assignments) {
                String resId = res.getId();
                assignmentIds.add(resId);
                assignmentRanks.add(Integer.toString(hosPrefs.indexOf(resId)));
            }
            // build output
            List<String> hosRow = new ArrayList<>(Arrays.asList(
                    hospital.getId(),
                    Integer.toString(hospital.getLocationId()),
                    Integer.toString(capacity),
                    Integer.toString(assignmentCount),
                    Float.toString(percentFull),
                    getPrefString(hosPrefs),
                    getPrefString(assignmentIds),
                    getPrefString(assignmentRanks)
            ));
            hospCSV.add(String.join(", ", hosRow));
        }

        try {
            Files.write(Paths.get("matching_output/" + filePrefix + "_hospitals.csv"), hospCSV, utf8);
            Files.write(Paths.get("matching_output/" + filePrefix + "_residents.csv"), resCSV, utf8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Hospital h : hospitalAssignments.keySet()) {
            sb.append(h.getId());
            sb.append(" : ");
            sb.append(hospitalAssignments.get(h).stream()
                    .map(Resident::getId)
                    .collect(ImmutableList.toImmutableList()));
            sb.append("\n");
        }
        return sb.toString();
    }
}
