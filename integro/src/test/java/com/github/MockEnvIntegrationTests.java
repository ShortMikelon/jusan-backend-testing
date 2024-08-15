package com.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import javax.money.CurrencyQuery;
import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class MockEnvIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExchangeRateClient exchangeRateClient;

    @Test
    void createOrder() throws Exception {
        CurrencyUnit euro = Monetary.getCurrency("EUR");
        MonetaryAmount amount = Monetary.getDefaultAmountFactory()
                .setCurrency(euro)
                .setNumber(100)
                .create();
        OrderRequest request = new OrderRequest();
        request.setAmount(amount);

        ResultActions response = mockMvc.perform(post("/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        response
                .andExpect(status().isCreated())
                .andExpect(jsonPath("paid").value(false));
    }

    @Test
    @Sql("/unpaid-order.sql")
    void payOrder() throws Exception {
        PaymentRequest request = new PaymentRequest();
        String creditCard = "random credit card number";
        request.setCreditCardNumber(creditCard);

        ResultActions response = mockMvc
                .perform(post("/order/1/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

        response
                .andExpect(status().isCreated())
                .andExpect(jsonPath("orderId").value(1))
                .andExpect(jsonPath("creditCardNumber").value(creditCard));
    }

    @Test
    @Sql("/paid-order.sql")
    void getReceipt() throws Exception {
        // TODO: Протестируйте получение чека на заказ №1 c currency = USD
        // Примечание: используйте мок для ExchangeRateClient
        BigDecimal rate = BigDecimal.valueOf(109.191999);
        Mockito.when(exchangeRateClient.getExchangeRate(any(), any()))
                .thenReturn(rate);

        ResultActions actual = mockMvc.perform(get("/order/{id}/receipt?currency=USD", 1));

        actual.andExpect(status().isOk());
    }
}
