/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.example;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.apache.commons.collections4.SetUtils;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;

import javax.sql.DataSource;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/*
docker run -d --restart always --name mysql \
  -p 127.0.0.1:5432:5432 \
  -e MYSQL_ROOT_PASSWORD=Random123 \
  mysql:latest
*/

public class App {
    public static void main(String[] args) throws IOException, SQLException {

        // Instance of DataSource interface allows creating new connections to database.
        DataSource ds = createDataSource();
        // Jdbi class uses DataSource to create connections to DB but takes care of life-cycle of the connection.
        // It closes connection for us after we executed SQL query. It's important because DBs limit number of connections
        // clients can make to it.
        Jdbi jdbi = Jdbi.create(ds);

        /*

        // For the sake of example CSV file contains two columns:
        //  1,John
        //  2,Mary
        String fileName = "test.csv";

        // parseCsvFile() returns stream but we know this CSV file is very small so
        // 1) extract only employee ID field;
        // 2) convert all items to simple List.
        List<Integer> employeeIds = parseCsvFile(fileName).map(row -> Integer.parseInt(row.get("id"))).collect(Collectors.toList());


        // walk through all employees and do some work for them.
        for (Integer employeeId : employeeIds) {
            Map<String, MembershipPeriod> map = getStringMembershipPeriods(jdbi, employeeId);

            LocalDate assignedAt = map.get("employee_automatic_benefit_group_assigned").startsAt;

            System.out.println(String.format("Employee ID %d was assigned to benefits group at %s", employeeId, assignedAt.toString()));

            // Execute another program. This method blocks our program until it finishes.
            Runtime.getRuntime().exec("System specific command line text here");
        }

        */

        ////
        Map<Integer, Long> idsToCheck = readIdsFromFile("input.csv");

        Set<Integer> ids = idsToCheck.keySet();

        String joined = ids.stream().map(Object::toString).collect(Collectors.joining(","));

        try (Handle handle = jdbi.open()) {

            handle.registerRowMapper(ConstructorMapper.factory(EmployeeEvents.class));

            String query =
                    "select employee_id, group_concat(name, ',', unix_timestamp(occurred_at) separator ';') as events_str\n" +
                    "                           from membership_events\n" +
                    "                           where name is not null and occurred_at is not null and employee_id in (:ids)\n" +
                    "                           group by employee_id";

            List<EmployeeEvents> events = handle.createQuery(query)
                    .bind("ids", joined)
                    .mapTo(EmployeeEvents.class)
                    .collect(Collectors.toList());

            System.out.println("Employees with incorrect order of membership events:");
            for (EmployeeEvents event: events)
                if (!event.isValid())
                    System.out.println(event.employeeId);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

//        cleanEmployeesIds(jdbi, idsToCheck);
    }

    private static void cleanEmployeesIds(Jdbi jdbi, Map<Integer, Long> idsToCheck) throws IOException {

        // Write to file deduplicated IDs.
        try (PrintWriter writer = new PrintWriter("ids_clean.csv", StandardCharsets.UTF_8)) {
            for (int id : idsToCheck.keySet())
                writer.println(id);
        }

        // Write to files IDs that appeared more than once.
        try (PrintWriter writer = new PrintWriter("ids_duplicates.csv", StandardCharsets.UTF_8)) {
            for (Map.Entry<Integer, Long> pair : idsToCheck.entrySet()) {
                if (pair.getValue() > 1)
                    writer.println(pair.getKey());
            }
        }

        String joined = idsToCheck.keySet().stream().map(Object::toString).collect(Collectors.joining(","));

        try (Handle handle = jdbi.open()) {
            Set<Integer> foundIds = handle.createQuery("select distinct id from employees where id in (:ids)")
                    .bind("ids", joined)
                    .mapTo(Integer.TYPE)
                    .collect(Collectors.toSet());

            System.out.println("Missing IDs:");
            for (int id : SetUtils.difference(idsToCheck.keySet(), foundIds)) {
                System.out.println(id);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    private static Map<Integer, Long> readIdsFromFile(String filePath) throws IOException {
        return Files.lines(Path.of(filePath), StandardCharsets.UTF_8)
                .map(line -> {
                    try {
                        return Integer.parseInt(line.trim());
                    } catch (Throwable ex) {
                        return 0;
                    }
                })
                .filter(id -> id > 0)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private static Stream<Map<String, String>> parseCsvFile(String fileName) throws IOException {

        // We use try-finally here only to utilize new faeture of Java that automatically calls Reader.close() method
        // when this function exits.
        try (Reader reader = new FileReader(fileName)) {

            // This particular CSV file doesn' have header line therefore we manually define names of columns.
            CsvSchema schema = CsvSchema.builder().addColumn("id").addColumn("name").build();
            CsvMapper mapper = new CsvMapper();

            // We could convert each row to Java class but for simplicity we parse CSV as map of strings. Hence if column value is
            // really a number we must explicitly convert it to Integer or Long.
            ObjectReader oReader = mapper.readerFor(Map.class).with(schema);
            MappingIterator<Map<String, String>> mi = oReader.readValues(reader);

            // Convert iterator of Jackson library into standard Java stream. We use streams because they're lazy meaning that
            // unlike List all rows are not loaded into memory at once. It's useful when we process giant CSV files (billions of rows)
            // and want to process items one by one.

            // Piece of this code is great example why Kotlin exists.
            Stream<Map<String, String>> strm = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(mi, Spliterator.ORDERED),
                    false);

            return strm;
        } finally {
        }
    }

    private static Map<String, MembershipPeriod> getStringMembershipPeriods(Jdbi jdbi, int employeeId) {

        List<MembershipPeriod> periods = jdbi.withHandle(handle -> {

                    // Variable `handle` contains instance of interface Handle that lets us to send something to DB and to
                    // define rules how to encode query parameters of SQL queries and to decode returned resultset.

                    // Tells JDBI how to turn row into instance of MembershipPeriod class.
                    handle.registerRowMapper(ConstructorMapper.factory(MembershipPeriod.class));

                    // To avoid accidental modification of database let's make this connection read-only.
                    handle.setReadOnly(true);

                    return handle.createQuery("select employee_id, start_reason, starts_at, ends_at from membership_periods where employee_id = :eid")
                            .bind("eid", employeeId) // Instead of making parameter part of String query we pass a variable. JDBI knows how to send it to DB.
                            .mapTo(MembershipPeriod.class)
                            .collect(Collectors.toList());
                }
        );

        // It might be convenient to quickly find membership period of certain type so we convert List to Map.
        // If there are many rows with the same "start_reason" value we'll lose them in resulting map.
        Map<String, MembershipPeriod> map = periods.stream().collect(Collectors.toMap(p -> p.reason.toLowerCase(), o -> o));
        return map;
    }

    private static DataSource createDataSource() throws SQLException {
        MysqlDataSource ds = new MysqlDataSource();
        ds.setUser("root");
        ds.setPassword("Random123");
        ds.setServerName("localhost");
        ds.setDatabaseName("anon");
        ds.setRequireSSL(true);
        ds.setUseSSL(true);
        return ds;
    }
}
