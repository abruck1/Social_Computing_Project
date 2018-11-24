package edu.texas.social_computing.hospitals;

import com.google.common.collect.*;

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

        // step 2: if in a couple, determine if blocking pairs can be resolved without a proximity violation
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

        if (!violation) {
            System.out.println("Passes stability validation");
        }
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
