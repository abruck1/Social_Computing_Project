package edu.texas.social_computing.hospitals;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

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
                    unassign(ndPartner);
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

    public void validateStability(List<Resident> residents, HospitalTable hospitalTable, ResidentTable residentTable) {
        List<String> violations = new ArrayList<>();
        for (Resident resident : residents) {
            String resId = resident.getId();
            if (resident.hasPartner()) {
                Resident partner = residentTable.getResidentById(resident.getPartnerId());
                Resident ndResident = MatchingUtils.worsePlacedResident(this, resident, partner);
                ndResident.setPrefsByLocation(ndResident.equals(resident) ? this.getAssignedHospital(partner).getLocationId() : this.getAssignedHospital(resident).getLocationId(), hospitalTable);
            }
            List<String> resPrefs = resident.getPreferences();
            Hospital residentAssignment = getAssignedHospital(resident);
            int resPrefIndex = hasAssignment(resident) ? resPrefs.indexOf(residentAssignment.getId()) : resPrefs.size();
            if (resPrefIndex > 0) {
                for (int i = 0; i < resPrefIndex; i++) {
                    Hospital h = hospitalTable.getHospitalById(resPrefs.get(i));
                    if (isRankedHigherThanWorstMatch(h, resId)) {
                        violations.add("(" + resId + ", " + h.getId() + ") is a blocking pair");
                    }
                }
            }
        }
        if (violations.size() == 0) {
            System.out.println("Passes stability validation");
        } else {
            for (String v : violations) {
                System.out.println(v);
            }
        }
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
                "residentAssignmentLocation",
                "residentAssignmentRank",
                "residentInitialPreferences",
                "residentFinalPreferences",
                "hasPartner",
                "isAnchorPartner",
                "partnerId",
                "partnerAssignment",
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
            String hasPartner = resident.hasPartner() ? "true" : "false";
            String isAnchorPartner = hasPartner.equals("true") ? ((resInitPrefs.equals(resPrefs)) ? "true" : "false") : "NA";

            // build output
            List<String> resRow = new ArrayList<>(Arrays.asList(
                    resident.getId(),
                    residentAssignment.getId(),
                    Integer.toString(residentAssignment.getLocationId()),
                    Integer.toString(resident.rankOf(residentAssignment)),
                    getPrefString(resInitPrefs),
                    getPrefString(resPrefs),
                    hasPartner,
                    isAnchorPartner,
                    (hasPartner.equals("true") ? partnerId : "NA"),
                    (hasPartner.equals("true") ? partnerAssignment.getId() : "NA"),
                    (hasPartner.equals("true") ? Integer.toString(partnerAssignment.getLocationId()) : "NA"),
                    (hasPartner.equals("true") ? Integer.toString(partner.rankOf(partnerAssignment)) : "NA"),
                    (hasPartner.equals("true") ? getPrefString(partner.getInitialPreferences()) : "NA"),
                    (hasPartner.equals("true") ? getPrefString(partner.getPreferences()) : "NA")
            ));
            resCSV.add(String.join(", ", resRow));
        }

        for (Hospital hospital : hospitals) {
            List<String> hosPrefs = hospital.getPreferences();
            List<Resident> assignments = getAssignedResidents(hospital);
            List<String> assignmentIds = new ArrayList<>();
            List<String> assignmentRanks = new ArrayList<>();
            for (Resident res : assignments) {
                String resId = res.getId();
                assignmentIds.add(resId);
                assignmentRanks.add(Integer.toString(hosPrefs.indexOf(resId)));
            }
            // build output
            List<String> hosRow = new ArrayList<>(Arrays.asList(
                    hospital.getId(),
                    Integer.toString(hospital.getLocationId()),
                    Integer.toString(hospital.getCapacity()),
                    Integer.toString(assignments.size()),
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
