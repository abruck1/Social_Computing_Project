package edu.texas.social_computing.hospitals;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.HashMap;
import java.util.Map;

public class ResidentTable {

    private Map<String, Resident> residentMap = new HashMap<>();
    private Map<String, Integer> residentRankProgress = new HashMap<>();

    public static ResidentTable create(Iterable<Resident> residents) {
        return new ResidentTable(residents);
    }

    private ResidentTable(Iterable<Resident> residents) {
        for (Resident resident : residents) {
            add(resident);
            residentRankProgress.put(resident.getId(), 0);
        }
    }

    public void add(Resident r) {
        checkArgument(!residentMap.containsKey(r.getId()));
        residentMap.put(r.getId(), r);
    }

    public Resident getResidentById(String id) {
        return residentMap.get(id);
    }

    public ImmutableList<Resident> getAll() {
        return ImmutableList.copyOf(residentMap.values());
    }

    public int getResidentRankProgress(Resident r) {
        return residentRankProgress.get(r.getId());
    }

    public void incrementResidentRankProgress(Resident r) {
        residentRankProgress.put(r.getId(), residentRankProgress.get(r.getId()) + 1);
    }

    public void resetResidentRankProgress(Resident r) {
        residentRankProgress.put(r.getId(), 0);
    }
}
