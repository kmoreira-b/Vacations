package org.example.vacations.models;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder @ToString
public class ReportStat {
    private Long employeeId;
    private String employeeName;
    private long vacationsCount;   // how many vacations this TAM requested
    private long coveragesCount;   // how many account-slots this TAM covered

    // Derived metric used by the UI
    public double getCoveragePerVacation() {
        return vacationsCount == 0
                ? (double) coveragesCount
                : (double) coveragesCount / (double) vacationsCount;
    }
}
