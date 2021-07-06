package com.example;

import java.beans.ConstructorProperties;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EmployeeEvents {
    int employeeId;
    List<Map.Entry<String, LocalDateTime>> events;

    @ConstructorProperties({"employee_id", "events_str"})
    public EmployeeEvents(int employeeId, String events) {
        this.employeeId = employeeId;

        this.events = new ArrayList<>();

        for (String item : events.split(";")) {
            String[] pair = item.split(",");
            this.events.add(Map.entry(pair[0], LocalDateTime.ofEpochSecond(Long.parseLong(pair[1]), 0, ZoneOffset.UTC)));
        }
        this.events.sort(Map.Entry.comparingByValue());
    }

    public boolean isValid() {

        Set<String> openingEvents = Set.of("opted_in.submitted", "enrolled.auto_enrolled", "enrolled.opted_in", "enrolled.join", "enrolled.reenrolled");
        Set<String> closingEvents = Set.of("opted_out", "ceased_membership", "withdraw_all_funds", "death");

        boolean isPeriodOpen = false;
        for (Map.Entry<String, LocalDateTime> pair : events) {

            String event = pair.getKey();

            if (openingEvents.contains(event)) {
                if (isPeriodOpen) {
                    // We can't open period if previous event already opened it
                    return false;
                } else
                    isPeriodOpen = true;
            } else
                if (closingEvents.contains(event)) {
                    if (isPeriodOpen)
                        isPeriodOpen = false;
                    else {
                        // Any closing event must be preceded by something that opens enrollment
                        return false;
                    }
                }
        }

        return true;
    }
}
