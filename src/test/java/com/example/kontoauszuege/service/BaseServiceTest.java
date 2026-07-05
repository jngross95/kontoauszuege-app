package com.example.kontoauszuege.service;

import com.example.kontoauszuege.service.BankAccess.BankConnection;
import com.example.kontoauszuege.service.BankAccess.BankInformation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class BaseServiceTest {

    @Autowired
    BaseService baseService;

    @BeforeEach
    void initBankConnection() {
        BankConnection.init();
    }

    @Test
    void getIconFromBic_matchesSparkasse() throws Exception {
        String bic = "73150000";
        String icon = baseService.getIconFromBic(bic);
        assertEquals("sparkasse-favicon2x.ico", icon);
    }
}
