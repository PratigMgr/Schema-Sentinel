package dev.sentinel.transform;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Converts an already-validated order feed into canonical Java objects (serialized to JSON by Jackson). */
@Component
public class OrderFeedMapper {

    public List<Order> map(byte[] xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // blocks XXE
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            DocumentBuilder b = f.newDocumentBuilder();
            Document doc = b.parse(new ByteArrayInputStream(xml));

            List<Order> orders = new ArrayList<>();
            NodeList orderNodes = doc.getElementsByTagName("order");
            for (int i = 0; i < orderNodes.getLength(); i++) {
                Element o = (Element) orderNodes.item(i);
                Element total = (Element) o.getElementsByTagName("total").item(0);
                List<Order.Item> items = new ArrayList<>();
                NodeList itemNodes = o.getElementsByTagName("item");
                for (int j = 0; j < itemNodes.getLength(); j++) {
                    Element it = (Element) itemNodes.item(j);
                    items.add(new Order.Item(it.getAttribute("sku"), Integer.parseInt(it.getAttribute("qty"))));
                }
                orders.add(new Order(
                        Long.parseLong(o.getAttribute("id")),
                        o.getElementsByTagName("customer").item(0).getTextContent().trim(),
                        new BigDecimal(total.getTextContent().trim()),
                        total.getAttribute("currency"),
                        items));
            }
            return orders;
        } catch (Exception e) {
            throw new IllegalStateException("Mapping failed after validation passed", e);
        }
    }
}
