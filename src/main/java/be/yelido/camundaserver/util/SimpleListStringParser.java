package be.yelido.camundaserver.util;

import java.util.Arrays;
import java.util.List;

public class SimpleListStringParser {

    /**
     * Parse a String containing a list separated by commas (ex. "a, b, c, d") into a list of String
     *
     * @param listString a list separated by commas
     * @return the list of String
     */
    public List<String> parse(String listString){
        return Arrays.asList(listString.split(","));
    }
}
