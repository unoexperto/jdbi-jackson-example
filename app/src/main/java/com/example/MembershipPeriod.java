package com.example;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.beans.ConstructorProperties;
import java.time.LocalDate;

public class MembershipPeriod {
//    @ColumnName("employee_id")
    int employeeId;
//    @ColumnName("start_reason")
    String reason;
//    @ColumnName("starts_at")
    LocalDate startsAt;
//    @ColumnName("ends_at")
    LocalDate endsAt;

    @ConstructorProperties({"employee_id", "start_reason", "starts_at", "ends_at"})
    public MembershipPeriod(int employeeId, String reason, LocalDate startsAt, LocalDate endsAt) {
        this.employeeId = employeeId;
        this.reason = reason;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }
}
