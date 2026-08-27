package com.sqleditor.controller;

import com.sqleditor.model.MockDataRequest;
import com.sqleditor.service.ConnectionSessionService;
import com.sqleditor.service.QueryService;
import jakarta.validation.Valid;
import net.datafaker.Faker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/mock")
public class MockDataController {
    private final ConnectionSessionService sessions;
    private final QueryService queryService;
    private final Faker faker = new Faker(new java.util.Locale("tr"));

    public MockDataController(ConnectionSessionService sessions, QueryService queryService) {
        this.sessions = sessions;
        this.queryService = queryService;
    }

    @PostMapping("/generate")
    public ResponseEntity<java.util.Map<String, String>> generateMockData(@Valid @RequestBody MockDataRequest req,
                                                                          @RequestHeader("X-Connection-Token") String token,
                                                                          Authentication auth) throws Exception {
        String role = auth.getAuthorities().iterator().next().getAuthority();
        if ("READ_ONLY".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sadece okuma yetkiniz var.");
        }

        try (Connection conn = sessions.get(auth.getName(), token)) {
            StringBuilder sql = new StringBuilder("INSERT INTO `")
                    .append(req.getDatabaseName()).append("`.`").append(req.getTableName()).append("` (");
            
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < req.getMappings().size(); i++) {
                sql.append("`").append(req.getMappings().get(i).getColumnName()).append("`");
                placeholders.append("?");
                if (i < req.getMappings().size() - 1) {
                    sql.append(", ");
                    placeholders.append(", ");
                }
            }
            sql.append(") VALUES (").append(placeholders).append(")");

            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                for (int r = 0; r < req.getRowCount(); r++) {
                    for (int i = 0; i < req.getMappings().size(); i++) {
                        String type = req.getMappings().get(i).getFakerType();
                        Object val = generateValue(type);
                        pstmt.setObject(i + 1, val);
                    }
                    pstmt.addBatch();
                    if (r % 500 == 0 || r == req.getRowCount() - 1) {
                        pstmt.executeBatch();
                    }
                }
            }
            return ResponseEntity.ok(java.util.Map.of("message", req.getRowCount() + " satir sentetik veri olusturuldu."));
        }
    }

    private Object generateValue(String type) {
        return switch (type) {
            case "Name.fullName" -> faker.name().fullName();
            case "Name.firstName" -> faker.name().firstName();
            case "Name.lastName" -> faker.name().lastName();
            case "Internet.email" -> faker.internet().emailAddress();
            case "Internet.password" -> faker.internet().password();
            case "Internet.domainName" -> faker.internet().domainName();
            case "Internet.macAddress" -> faker.internet().macAddress();
            case "Internet.ipv4Address" -> faker.internet().ipV4Address();
            case "Address.fullAddress" -> faker.address().fullAddress();
            case "Address.city" -> faker.address().city();
            case "Address.country" -> faker.address().country();
            case "Address.zipCode" -> faker.address().zipCode();
            case "PhoneNumber.cellPhone" -> faker.phoneNumber().cellPhone();
            case "Number.randomDigit" -> faker.number().randomDigit();
            case "Number.randomInt" -> faker.number().numberBetween(1, 100000);
            case "Number.age" -> faker.number().numberBetween(0, 101);
            case "Number.randomDouble" -> faker.number().randomDouble(2, 1, 5000);
            case "Date.birthday" -> new java.sql.Date(faker.date().birthday().getTime());
            case "Date.future" -> new java.sql.Timestamp(faker.date().future(365, java.util.concurrent.TimeUnit.DAYS).getTime());
            case "Date.past" -> new java.sql.Timestamp(faker.date().past(365, java.util.concurrent.TimeUnit.DAYS).getTime());
            case "Date.now" -> new java.sql.Timestamp(System.currentTimeMillis());
            case "Company.name" -> faker.company().name();
            case "Company.industry" -> faker.company().industry();
            case "Commerce.productName" -> faker.commerce().productName();
            case "Commerce.price" -> Double.valueOf(faker.commerce().price().replace(",", "."));
            case "Commerce.department" -> faker.commerce().department();
            case "Finance.creditCard" -> faker.finance().creditCard();
            case "Job.title" -> faker.job().title();
            case "Lorem.word" -> faker.lorem().word();
            case "Lorem.sentence" -> faker.lorem().sentence();
            case "Lorem.paragraph" -> faker.lorem().paragraph();
            case "Color.name" -> faker.color().name();
            case "Bool.random" -> faker.bool().bool();
            case "Gender.types" -> faker.gender().types();
            default -> faker.lorem().word();
        };
    }
}
