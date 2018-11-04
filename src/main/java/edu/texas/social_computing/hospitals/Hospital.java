package edu.texas.social_computing.hospitals;

import java.util.List;
import java.util.Set;

public class Hospital {
    private int locationId;
    private Set<Resident> residents; // current residents
    private List<Resident> preferences;
    private int capacity;

    public int getLocationId() {
        return locationId;
    }

    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

    public Set<Resident> getResidents() {
        return residents;
    }

    public void setResidents(Set<Resident> residents) {
        this.residents = residents;
    }

    public List<Resident> getPreferences() {
        return preferences;
    }

    public void setPreferences(List<Resident> preferences) {
        this.preferences = preferences;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public static Hospital getAssignment(Resident resident, List<Hospital> hospitals) {
        return new Hospital();
    }

    public boolean isOverSubscribed() {
        return residents.size() > capacity;
    }

    public boolean isFull() {
        return residents.size() == capacity;
    }

    public void assign(Resident resident) {

    }

    /**
     *
     * @return rejected resident
     */
    public Resident rejectWorstResident() {
        return new Resident();
    }

    /**
     *
     * @return worst resident
     */
    public Resident getWorstResident() {
        return null;
    }

    /**
     *
     * @param resident
     * @return all residents worse than provided resident
     */
    public List<Resident> getWorseThan(Resident resident) {
        return null;
    }



}
