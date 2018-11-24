package edu.texas.social_computing.hospitals;

import java.io.FileNotFoundException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {

        //String resFile = "./resources/test_hos5_loc2_res16_coup3_residents.csv";
        //String hosFile = "./resources/test_hos5_loc2_res16_coup3_hospitals.csv";

        //String resFile = "./resources/test_hos50_loc50_res100_coup50_residents.csv";
        //String hosFile = "./resources/test_hos50_loc50_res100_coup50_hospitals.csv";

        //String resFile = "./resources/test_hos50_loc50_res150_coup20_residents.csv";
        //String hosFile = "./resources/test_hos50_loc50_res150_coup20_hospitals.csv";

        //String resFile = "./resources/test_hos100_loc10_res200_coup50_residents.csv";
        //String hosFile = "./resources/test_hos100_loc10_res200_coup50_hospitals.csv";

        String resFile = "./resources/test_hos300_loc50_res500_coup100_residents.csv";
        String hosFile = "./resources/test_hos300_loc50_res500_coup100_hospitals.csv";

        // load lists of hospital and residents from file
        List<Resident> residents = FileImporter.importResidents(resFile);
        List<Hospital> hospitals = FileImporter.importHospitals(hosFile);

        // load lists of hospital and residents from file
        //List<Resident> residents = FileImporter.importResidents(args[0]);
        //List<Hospital> hospitals = FileImporter.importHospitals(args[1]);
        System.out.println("loaded Files");
        System.out.println("residents: " + Integer.toString(residents.size()));
        System.out.println("hospitals: " + Integer.toString(hospitals.size()));

        // make lookup tables
        HospitalTable hospitalTable = HospitalTable.create(hospitals);
        ResidentTable residentTable = ResidentTable.create(residents);
        System.out.println("generated tables");

        // call HRPP
        Matching finalMatch = HRPP.run(hospitalTable, residentTable);
        System.out.println(finalMatch);
        finalMatch.validateProximities(residents, residentTable);
        finalMatch.validateCapacities(hospitals);
        finalMatch.validateStability(residentTable, hospitalTable);
        finalMatch.outputMatchingToCsv("testFix", residents, residentTable, hospitals);
        System.out.println("Done");
    }
}
