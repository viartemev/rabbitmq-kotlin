package com.example


import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class CoroutinesApplicationTests(@Autowired val client: WebTestClient) {

    @Test
    fun pull() {
        client.get().uri("/pull").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().is2xxSuccessful.expectBody()
    }

    @Test
    fun push() {
        client.get().uri("/push").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().is2xxSuccessful.expectBody()
    }

}
