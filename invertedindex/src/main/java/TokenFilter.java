import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenFilter {
    static Pattern nonAlphabets = Pattern.compile("^[^a-zA-Z]*$");
    static Pattern startsWithNumber = Pattern.compile("^\\d+(?:\\W*\\w*)*");
    static Pattern acronyms = Pattern.compile(".*(?<![a-zA-Z0-9])([a-zA-Z]{1}\\.{1}){2,5}.*");
    static Pattern dashes = Pattern.compile(".*(?:\\w+-\\d+).*|^[a-zA-Z]{1,2}-\\w+");

    private List<String> tokens = new ArrayList<>();

    private String[] splitToken(String token) {
        Matcher m1 = startsWithNumber.matcher(token);
        Matcher m2 = dashes.matcher(token);
        if (m1.find() || !m2.find()) {
            return token.split("\\W");
        } else {
            return new String[]{token};
        }
    }

    private String filterToken(String token) {
        Matcher m1 = nonAlphabets.matcher(token);
        Matcher m2 = acronyms.matcher(token);
        if (m1.find()) {
            return "";
        } else if (m2.find()) {
            return token.replace(".", "");
        } else {
            return token;
        }
    }

    public List<String> getTokens() {
        return tokens;
    }

    public TokenFilter(String token) {
        token = filterToken(token);
        String[] results = splitToken(token);
        for (String t : results) {
            String filteredToken = filterToken(t);
            if (!"".equals(filteredToken)) {
                this.tokens.add(filteredToken);
            }
        }
    }
}