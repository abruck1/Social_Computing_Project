package edu.texas.social_computing.hospitals;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

import java.util.*;

public class Matching {

    private static final Hospital NO_MATCH = Hospital.create("NO_MATCH", -1, 0, ImmutableList.of());

    private Multimap<Hospital, Resident> hospitalAssignments = HashMultimap.create();
    private Map<Resident, Hospital> residentAssignments = new HashMap<>();

    public void assign(Resident r, Hospital h) {
        hospitalAssignments.put(h, r);
        residentAssignments.put(r, h);
    }

    public void unassign(Resident r) {
        Hospital h = getAssignedHospital(r);
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

    public Set<Resident> getAllUnassigned(List<Resident> residents) {
        Set<Resident> unassignedResidents = new HashSet<>();
        for(Resident resident : residents) {
            if(!hasAssignment(resident)) {
                unassignedResidents.add(resident);
            }
        }
        return unassignedResidents;
    }

    /**
     *
     * @param residents : a list of Non-Dominant (ND) residents in a matching that needs to be checked to see
     *                 if any of the residents in the list violate a proximity constraint
     * @return a set of residents that violate the proximity constraint AND are the worse placed partner
     */
    public Set<Resident> getNDProximityViolators(List<Resident> residents, ResidentTable residentTable) {
        Set<Resident> freeViolatingResidents = new HashSet<>();
        for(Resident resident : residents) {
            if(!resident.hasPartner()) continue;
            Resident partner = residentTable.getResidentById(resident.getPartnerId());
            if(!hasAssignment(resident)) continue;
            if(!hasAssignment(partner)) {
                freeViolatingResidents.add(partner);
                continue;
            }
            Hospital assignedHospital = this.getAssignedHospital(resident);
            Hospital partnerAssignedHospital = this.getAssignedHospital(partner);
            int location = assignedHospital.getLocationId();
            int partnerLocation = partnerAssignedHospital.getLocationId();
            if(location != partnerLocation) {
                // if there is a tie for worse placed partner we only want 1 to be non-dominant
                if(!freeViolatingResidents.contains(partner)) {
                    Resident ndPartner = MatchingUtils.worsePlacedResident(this, resident, partner);
                    freeViolatingResidents.add(ndPartner);
                    unassign(ndPartner);
                }
            }
        }
        return freeViolatingResidents;
    }

}
