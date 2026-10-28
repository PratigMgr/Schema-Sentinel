package dev.sentinel.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

/** Loads and caches versioned XSDs from classpath:schemas/{version}/order-feed.xsd */
@Component
public class SchemaRegistry {

    private static final Pattern VERSION = Pattern.compile("v\\d{1,3}");
    private final Map<String, Schema> cache = new ConcurrentHashMap<>();

    public Schema get(String version) {
        if (!VERSION.matcher(version).matches()) {
            throw new UnknownSchemaVersionException(version);
        }
        return cache.computeIfAbsent(version, this::load);
    }

    private Schema load(String version) {
        ClassPathResource res = new ClassPathResource("schemas/" + version + "/order-feed.xsd");
        if (!res.exists()) {
            throw new UnknownSchemaVersionException(version);
        }
        try (InputStream in = res.getInputStream()) {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            sf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            sf.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            sf.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return sf.newSchema(new StreamSource(in));
        } catch (IOException | SAXException e) {
            throw new IllegalStateException("Could not load schema " + version, e);
        }
    }
}
