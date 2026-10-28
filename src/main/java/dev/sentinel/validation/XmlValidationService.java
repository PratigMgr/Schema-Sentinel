package dev.sentinel.validation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;
import org.springframework.stereotype.Service;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

@Service
public class XmlValidationService {

    private final SchemaRegistry registry;

    public XmlValidationService(SchemaRegistry registry) {
        this.registry = registry;
    }

    /** Validates XML against the given schema version; returns every problem found, with line/column. */
    public List<ValidationError> validate(byte[] xml, String version) {
        List<ValidationError> errors = new ArrayList<>();
        try {
            Validator validator = registry.get(version).newValidator();
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            validator.setErrorHandler(new ErrorHandler() {
                public void warning(SAXParseException e) { errors.add(of("WARNING", e)); }
                public void error(SAXParseException e) { errors.add(of("ERROR", e)); }
                public void fatalError(SAXParseException e) throws SAXException {
                    errors.add(of("FATAL", e));
                    throw e;
                }
            });
            validator.validate(new StreamSource(new ByteArrayInputStream(xml)));
        } catch (SAXParseException ignored) {
            // already recorded by fatalError()
        } catch (SAXException | IOException e) {
            errors.add(new ValidationError(0, 0, "FATAL", e.getMessage()));
        }
        return errors;
    }

    private static ValidationError of(String severity, SAXParseException e) {
        return new ValidationError(e.getLineNumber(), e.getColumnNumber(), severity, e.getMessage());
    }
}
