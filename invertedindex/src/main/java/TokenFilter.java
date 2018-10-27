import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to re-tokenize results of lemmatization. Tailored for cranfield collection.
 */
public class TokenFilter {
    /**
     * Regex to match words that do not have any alphabets.
     */
    static Pattern nonAlphabets = Pattern.compile("^[^a-zA-Z]*$");

    /**
     * Regex to match words that start with a number.
     */
    static Pattern startsWithNumber = Pattern.compile("^\\d+(?:\\W*\\w*)*");

    /**
     * Regex to match acronyms like u.s.a. or i.b.m. Doesn't match ph.d. (the acronym has to be a single character
     * followed by a dot (.)
     */
    static Pattern acronyms = Pattern.compile(".*(?<![a-zA-Z0-9])([a-zA-Z]{1}\\.{1}){2,5}.*");

    /**
     * Regex to match dashes when they impart meaning -- when a sequence of digits follow a word or character separated
     * by a dash or when the word preceding the dash is 2 characters or less and only a sequence of alphabets follow the
     * dash.
     */
    static Pattern dashes = Pattern.compile(".*(?:\\w+-\\d+).*|^[a-zA-Z]{1,2}-\\w+");

    private List<String> tokens = new ArrayList<>();

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

    /**
     * When a token needs to be split, usually on the punctuation in the token.
     *
     * @param token word to split
     * @return a list of Strings after splitting the token
     */
    private String[] splitToken(String token) {
        Matcher m1 = startsWithNumber.matcher(token);
        Matcher m2 = dashes.matcher(token);
        if (m1.find() || !m2.find()) {
            return token.split("\\W");
        } else {
            return new String[]{token};
        }
    }

    /**
     * When the token only needs to be filtered or dots in acronyms need to be removed.
     *
     * @param token word to filter
     * @return filtered word
     */
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
}