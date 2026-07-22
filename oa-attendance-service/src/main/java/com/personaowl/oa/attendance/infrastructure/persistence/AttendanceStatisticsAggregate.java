package com.personaowl.oa.attendance.infrastructure.persistence;

public class AttendanceStatisticsAggregate {

    private long totalRecords;
    private long totalUsers;
    private long normalCount;
    private long lateCount;
    private long earlyLeaveCount;
    private long missingCheckOutCount;

    public long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getNormalCount() {
        return normalCount;
    }

    public void setNormalCount(long normalCount) {
        this.normalCount = normalCount;
    }

    public long getLateCount() {
        return lateCount;
    }

    public void setLateCount(long lateCount) {
        this.lateCount = lateCount;
    }

    public long getEarlyLeaveCount() {
        return earlyLeaveCount;
    }

    public void setEarlyLeaveCount(long earlyLeaveCount) {
        this.earlyLeaveCount = earlyLeaveCount;
    }

    public long getMissingCheckOutCount() {
        return missingCheckOutCount;
    }

    public void setMissingCheckOutCount(long missingCheckOutCount) {
        this.missingCheckOutCount = missingCheckOutCount;
    }
}
