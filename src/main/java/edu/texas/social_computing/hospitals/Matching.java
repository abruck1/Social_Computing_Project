package edu.texas.social_computing.hospitals;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

import java.util.HashMap;
import java.util.Map;

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

}
