package dev.sentinel;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FeedControllerTest {

    @Autowired MockMvc mvc;

    private static final String VALID = """
            <orderFeed>
              <order id="1001">
                <customer>Ada</customer>
                <total currency="CAD">25.50</total>
                <item sku="A-100" qty="2"/>
                <item sku="B-200" qty="1"/>
              </order>
            </orderFeed>""";

    private static final String INVALID = """
            <orderFeed>
              <order id="abc">
                <customer>Grace</customer>
                <total currency="EUR">10.00</total>
                <item sku="C-300" qty="0"/>
              </order>
            </orderFeed>""";

    @Test
    void validFeedIsConvertedToJson() throws Exception {
        mvc.perform(post("/api/v1/feeds").contentType(MediaType.APPLICATION_XML).content(VALID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.orders[0].id").value(1001))
                .andExpect(jsonPath("$.orders[0].currency").value("CAD"))
                .andExpect(jsonPath("$.orders[0].items.length()").value(2));
    }

    @Test
    void invalidFeedReturnsAllErrorsWithLineNumbers() throws Exception {
        mvc.perform(post("/api/v1/feeds").contentType(MediaType.APPLICATION_XML).content(INVALID))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0].line").isNumber());
    }

    @Test
    void malformedXmlIsRejected() throws Exception {
        mvc.perform(post("/api/v1/feeds/validate").contentType(MediaType.APPLICATION_XML).content("<orderFeed><order>"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[?(@.severity == 'FATAL')]").isNotEmpty());
    }

    @Test
    void unknownSchemaVersionIs404() throws Exception {
        mvc.perform(post("/api/v9/feeds/validate").contentType(MediaType.APPLICATION_XML).content(VALID))
                .andExpect(status().isNotFound());
    }

    @Test
    void doctypeIsBlockedAgainstXxe() throws Exception {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><orderFeed>&e;</orderFeed>";
        mvc.perform(post("/api/v1/feeds").contentType(MediaType.APPLICATION_XML).content(xxe))
                .andExpect(status().isUnprocessableEntity());
    }
}
