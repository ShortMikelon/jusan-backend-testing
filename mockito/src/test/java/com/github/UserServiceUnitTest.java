package com.github;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.mockito.ArgumentMatchers.anyString;

/*
    Необходимо замокать nameService и протестировать userService.getUserName
    TODO
        Использовать аннотацию запуска проекта с использованием SpringJUnit4ClassRunner
        Использовать аннотацию SpringBootTest вместе с MocksApplication.class
        Добавить ссылка на UserService
        Добавить ссылку на NameService
        Написать функцию для тестирования userService которая мокает nameService и возвращает "Mock user name"
*/
@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
public class UserServiceUnitTest {
    // your solution

    private NameService nameService;

    private UserService userService;

    @Before
    public void setup() {
        nameService = Mockito.mock(NameService.class);
        userService = new UserService(nameService);
    }

    @Test
    public void getUserName_acceptsAnyString_returnString() {
        String expect = "Mock user name";
        Mockito.when(nameService.getUserName(anyString())).thenReturn(expect);

        String actual = userService.getUserName("ID");

        Assert.assertEquals(expect, actual);
    }
}