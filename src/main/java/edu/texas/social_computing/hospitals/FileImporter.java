package edu.texas.social_computing.hospitals;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

class FileImporter {
    private static Scanner getScanner(String path) throws FileNotFoundException {
        File file = new File(path);
        Scanner sc;
        try {
            sc = new Scanner(file);
        } catch (FileNotFoundException e) {
            System.out.println("couldn't find file" + e);
            throw e;
        }
        return sc;
    }

    static List<Hospital> importHospitals(String path) throws FileNotFoundException {
        Scanner sc = getScanner(path);

        List<Hospital> results = new ArrayList<>();

        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            String[] parts = line.split(",");
            String id = parts[0];
            Integer locationid = Integer.valueOf(parts[1]);
            Integer capacity = Integer.valueOf(parts[2]);
            String concatPrefs = parts[3];
            List<String> preferences = Arrays.asList(concatPrefs.split(" "));

            Hospital hospital = Hospital.create(id, locationid, capacity, preferences);
            results.add(hospital);
        }

        return results;
    }

    static List<Resident> importResidents(String path) throws FileNotFoundException {
        Scanner sc = getScanner(path);

        List<Resident> results = new ArrayList<>();

        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            String[] parts = line.split(",");
            String id = parts[0];
            String concatPrefs = parts[1];
            List<String> preferences = Arrays.asList(concatPrefs.split(" "));
            String partnerId = parts[2];

            Resident resident;
            if (partnerId.equals("none")) {
                resident = Resident.create(id, preferences);
            } else {
                resident = Resident.create(id, partnerId, preferences);
            }

            results.add(resident);
        }

        return results;

    }
}
