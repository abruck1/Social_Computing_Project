package edu.texas.social_computing.hospitals;

import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

public class HospitalTable {

    private Map<String, Hospital> hospitalMap = new HashMap<>();

    public static HospitalTable create(Iterable<Hospital> hospitals) {
        return new HospitalTable(hospitals);
    }

    private HospitalTable(Iterable<Hospital> hospitals) {
        hospitals.forEach(this::add);
    }

    public void add(Hospital h) {
        checkArgument(!hospitalMap.containsKey(h.getId()));
        hospitalMap.put(h.getId(), h);
    }

    public Hospital getHospitalById(String id) {
        return hospitalMap.get(id);
    }

    public ImmutableList<Hospital> getAll() {
        return ImmutableList.copyOf(hospitalMap.values());
    }
}
