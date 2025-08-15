package org.example.vacations.models;

public class ReportStat {
    private Long employeeId;
    private String employeeName;
    private long vacationsCount;
    private long coveragesCount;

    public ReportStat(Long employeeId, String employeeName, long vacationsCount, long coveragesCount) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.vacationsCount = vacationsCount;
        this.coveragesCount = coveragesCount;
    }

    public Long getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public long getVacationsCount() { return vacationsCount; }
    public long getCoveragesCount() { return coveragesCount; }

    public double getCoveragePerVacation() {
        return vacationsCount == 0 ? coveragesCount : (double) coveragesCount / (double) vacationsCount;
    }
}
