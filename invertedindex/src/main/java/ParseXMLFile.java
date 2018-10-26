import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ParseXMLFile extends DefaultHandler {
    private boolean title = false;
    private boolean text = false;
    private StringBuilder textField = new StringBuilder();

    public StringBuilder getTextField() {
        return textField;
    }

    @Override
    public void startElement(String uri,
                             String localName, String qName, Attributes attributes) throws SAXException {

        switch (qName.toLowerCase()) {
            case "text":
                text = true;
                break;
            case "title":
                title = true;
                break;
        }
    }

    @Override
    public void endElement(String uri,
                           String localName, String qName) throws SAXException {
        switch (qName.toLowerCase()) {
            case "text":
                text = false;
                break;
            case "title":
                title = false;
                break;
        }
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {

        if (title) {
            String titleLine = new String(ch, start, length);
            textField.append(titleLine);
        } else if (text) {
            String textLine = new String(ch, start, length);
            textField.append(textLine);
        } else {
            // ignore the other tags
        }
    }
}