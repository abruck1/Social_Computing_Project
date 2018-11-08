package edu.texas.social_computing.hospitals;


import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        // load lists of hospital and residents from file
        List<Resident> residents = new ArrayList<>();
        List<Hospital> hospitals = new ArrayList<>();

        // make lookup tables
        HospitalTable hospitalTable = HospitalTable.create(hospitals);
        ResidentTable residentTable = ResidentTable.create(residents);

        // call HRPP
        // HRPP.run(hospitalTable, residentTable);

    }
}
