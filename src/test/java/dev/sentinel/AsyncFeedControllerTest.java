package dev.sentinel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@AutoConfigureMockMvc
class AsyncFeedControllerTest {

    @Autowired MockMvc mvc;
    @MockBean RabbitTemplate rabbit;

    private static final String XML = "<orderFeed/>";

    @Test
    void submitReturns202AndPublishesToTheIncomingQueue() throws Exception {
        mvc.perform(post("/api/v1/feeds/async").contentType(MediaType.APPLICATION_XML).content(XML))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.feedId").isNotEmpty());
        verify(rabbit).send(eq(""), eq("feeds.incoming"), any(Message.class));
    }

    @Test
    void unknownVersionIs404AndNothingIsQueued() throws Exception {
        mvc.perform(post("/api/v9/feeds/async").contentType(MediaType.APPLICATION_XML).content(XML))
                .andExpect(status().isNotFound());
        verifyNoInteractions(rabbit);
    }

    @Test
    void statusOfUnknownFeedIs404() throws Exception {
        mvc.perform(get("/api/feeds/does-not-exist")).andExpect(status().isNotFound());
    }
}
