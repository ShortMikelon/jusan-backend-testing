package com.github;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.BodySpec;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ServerIntegrationTests {
    @Autowired
    private WebTestClient webClient;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private static MockWebServer mockWebServer;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("exchange-rate-api.base-url", () -> mockWebServer.url("/").url().toString());
    }

    @BeforeAll
    static void setupMockWebServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void deleteEntities() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void createOrder() {
        // TODO: протестируйте успешное создание заказа на 100 евро
        // используя webClient
        CurrencyUnit euro = Monetary.getCurrency("EUR");
        MonetaryAmount amount = Monetary.getDefaultAmountFactory()
                .setCurrency(euro)
                .setNumber(100)
                .create();
        OrderRequest request = new OrderRequest();
        request.setAmount(amount);
        BigDecimal expectedAmount = BigDecimal.valueOf(100);

        ResponseSpec response = webClient.post().uri("/order")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        response.expectStatus().isCreated();
        BodySpec<Order, ? extends BodySpec<Order, ?>> body = response.expectBody(Order.class);
        BigDecimal actualAmount = body.returnResult().getResponseBody().getAmount();
        assertEquals(expectedAmount, actualAmount);
    }

    @Test
    void payOrder() {
        Long id = getIdFromOrderRepository();
        PaymentRequest request = new PaymentRequest();
        String creditCard = "123456789";
        request.setCreditCardNumber(creditCard);
        PaymentResponse expectedBody = new PaymentResponse(id, creditCard);

        ResponseSpec response = webClient.post().uri("/order/{id}/payment", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();

        response.expectStatus().isCreated();
        PaymentResponse body = response.expectBody(PaymentResponse.class).returnResult().getResponseBody();
        assertEquals(expectedBody, body);
    }

    @Test
    void getReceipt() {
        // TODO: Протестируйте получение чека на заказ №1 c currency = USD
        // Создайте объект Order, Payment и выполните save, используя orderRepository
        // Используйте mockWebServer для получения conversion_rate
        // Сделайте запрос через webClient
        BigDecimal amount = BigDecimal.valueOf(100);
        Order order = new Order(LocalDateTime.now(), amount, true);
        order = orderRepository.save(order);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setId(null);
        payment.setCreditCardNumber("123456789");
        paymentRepository.save(payment);

        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200)
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody("{\"conversion_rate\": 0.8412}")
        );

        webClient.get().uri("/order/{id}/receipt", order.getId())
                .exchange()
                .expectStatus().isOk();
    }

    private Long getIdFromOrderRepository() {
        CurrencyUnit euro = Monetary.getCurrency("EUR");
        MonetaryAmount amount = Monetary.getDefaultAmountFactory()
                .setCurrency(euro)
                .setNumber(100)
                .create();
        OrderRequest request = new OrderRequest();
        request.setAmount(amount);
        webClient.post().uri("/order")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange();
        return orderRepository.findAll().get(0).getId();
    }
}
