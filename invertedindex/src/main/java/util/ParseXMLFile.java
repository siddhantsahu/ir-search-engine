package util;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses a document in the collection to extract only text fields and discard fields containing only numbers.
 */
public class ParseXMLFile extends DefaultHandler {
    private boolean title = false;
    private boolean text = false;
    private boolean author = false;
    private boolean biblio = false;

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
            case "author":
                author = true;
                break;
            case "biblio":
                biblio = true;
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
            case "author":
                author = false;
                break;
            case "biblio":
                biblio = false;
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
        } else if (biblio) {
            String biblioLine = new String(ch, start, length);
            textField.append(biblioLine);
        } else if (author) {
            String authorLine = new String(ch, start, length);
            textField.append(authorLine);
        } else {
            // ignore
        }
    }
}