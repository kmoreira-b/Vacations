package org.example.vacations.models;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder @ToString
public class ReportStat {
    private Long employeeId;
    private String employeeName;

    // existing counters
    private long vacationsCount;      // times this TAM requested vacations
    private long coveragesCount;

    //  (account × day)
    private long daysCoveredForOthers;
    private long daysOthersCoveredMe;

    // existing derived metric
    public double getCoveragePerVacation() {
        return vacationsCount == 0 ? (double) coveragesCount
                : (double) coveragesCount / (double) vacationsCount;
    }

    //Ratio Calculation
    public Double getGivebackRatio() {
        if (daysOthersCoveredMe == 0 && daysCoveredForOthers == 0) return null;
        if (daysOthersCoveredMe == 0) return Double.POSITIVE_INFINITY;
        return (double) daysOthersCoveredMe / (double) daysCoveredForOthers;
    }
}
