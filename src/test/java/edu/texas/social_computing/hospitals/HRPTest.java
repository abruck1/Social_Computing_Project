package edu.texas.social_computing.hospitals;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class HRPTest {

    @Test
    public void emptyHrp_shouldNotCrash() {
        HospitalTable hospitalTable = HospitalTable.create(ImmutableList.of());
        ResidentTable residentTable = ResidentTable.create(ImmutableList.of());

        Matching m = runWithAllResidents(residentTable, hospitalTable);

        assertThat(m).isNotNull();
    }

    @Test
    public void oneResident_shouldGetFirstChoice() {
        Hospital h1 = Hospital.create("h1", 1, 1, ImmutableList.of("r1"));
        Resident r1 = Resident.create("r1", ImmutableList.of("h1"));

        HospitalTable hospitalTable = HospitalTable.create(ImmutableList.of(h1));
        ResidentTable residentTable = ResidentTable.create(ImmutableList.of(r1));

        Matching m = runWithAllResidents(residentTable, hospitalTable);

        assertThat(m.getAssignedHospital(r1)).isEqualTo(h1);
        assertThat(m.getAssignedResidents(h1)).contains(r1);
        assertThat(m.getAllUnassigned(residentTable.getAll())).isEmpty();
    }

    @Test
    public void twoResidentsOneSlot_hospitalShouldGetFirstChoice() {
        Hospital h1 = Hospital.create("h1", 1, 1, ImmutableList.of("r1", "r2"));
        Resident r1 = Resident.create("r1", ImmutableList.of("h1"));
        Resident r2 = Resident.create("r2", ImmutableList.of("h1"));

        HospitalTable hospitalTable = HospitalTable.create(ImmutableList.of(h1));
        ResidentTable residentTable = ResidentTable.create(ImmutableList.of(r1, r2));

        Matching m = runWithAllResidents(residentTable, hospitalTable);

        assertThat(m.getAssignedHospital(r1)).isEqualTo(h1);
        assertThat(m.getAssignedResidents(h1)).contains(r1);
        assertThat(m.getAllUnassigned(residentTable.getAll())).contains(r2);
    }

    private Matching runWithAllResidents(ResidentTable residentTable, HospitalTable hospitalTable) {
        Queue<Resident> freeResidents = new ArrayDeque<>();
        freeResidents.addAll(residentTable.getAll());
        return HRP.run(hospitalTable, residentTable, freeResidents);
    }

}
