package edu.texas.social_computing.hospitals;


import java.io.FileNotFoundException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        // load lists of hospital and residents from file
        List<Resident> residents = FileImporter.importResidents(args[0]);
        List<Hospital> hospitals = FileImporter.importHospitals(args[1]);
        System.out.println("loaded Files");
        System.out.println("residents: " + Integer.toString(residents.size()));
        System.out.println("hospitals: " + Integer.toString(hospitals.size()));

        // make lookup tables
        HospitalTable hospitalTable = HospitalTable.create(hospitals);
        ResidentTable residentTable = ResidentTable.create(residents);
        System.out.println("generated tables");

        // call HRPP
        Matching finalMatch = HRPP.run(hospitalTable, residentTable);
        finalMatch.validateProximities(residents, residentTable);
        finalMatch.validateCapacities(hospitals);
        finalMatch.validateStability(residents, hospitalTable, residentTable);

        finalMatch.outputMatchingToCsv("export_hos300_loc50_res500_coup100", residents, residentTable, hospitals);
        System.out.println("Done");

    }
}
