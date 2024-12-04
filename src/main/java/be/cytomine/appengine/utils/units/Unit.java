package be.cytomine.appengine.utils.units;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;

@Getter
public class Unit {

    public static final String REGEX_VALUE = "^(?:0|[1-9]\\d*)(?:\\.\\d+)?";

    public static final String REGEX_PREFIX = "\\s*(?:Ki|Mi|Gi|Ti|Pi|Ei|Zi|Yi|k|M|G|T|P|E|Z|Y|R|Q)?";

    public static final String REGEX_SUFFIX = "(?:B|Byte|byte|b|bit)$";

    private static final List<String> byteUnits = List.of("B", "Byte", "byte");

    private static final List<String> bitUnits = List.of("b", "bit");

    private static final Map<String, Integer> decimals = new HashMap<>();

    private static final Map<String, Integer> binary = new HashMap<>();

    private long bytes;

    private long bits;

    static {
        decimals.put("k", 3);
        decimals.put("M", 6);
        decimals.put("G", 9);
        decimals.put("T", 12);
        decimals.put("P", 15);
        decimals.put("E", 18);
        decimals.put("Z", 21);
        decimals.put("Y", 24);
        decimals.put("R", 27);
        decimals.put("Q", 30);

        binary.put("Ki", 10);
        binary.put("Mi", 20);
        binary.put("Gi", 30);
        binary.put("Ti", 40);
        binary.put("Pi", 50);
        binary.put("Ei", 60);
        binary.put("Zi", 70);
        binary.put("Yi", 80);
    }

    public Unit(String dataSize) {
        List<String> parts = parse(dataSize);

        double value = Double.parseDouble(parts.get(0));
        String unitPrefix = parts.get(1);
        String unitSuffix = parts.get(2);

        long number = Math.round(value * getMultiplier(unitPrefix, unitSuffix));

        if (byteUnits.contains(unitSuffix)) {
            this.bytes = number;
            this.bits = number * 8;
        }

        if (bitUnits.contains(unitSuffix)) {
            this.bits = number;
            this.bytes = Math.round(number / 8);
        }
    }

    private long getMultiplier(String unitPrefix, String unitSuffix) {
        if (decimals.containsKey(unitPrefix)) {
            return (long) Math.pow(10, decimals.get(unitPrefix));
        }

        if (binary.containsKey(unitPrefix)) {
            return (long) Math.pow(2, binary.get(unitPrefix));
        }

        return 1;
    }

    public static boolean isValid(String dataSize) {
        return dataSize.matches(REGEX_VALUE + REGEX_PREFIX + REGEX_SUFFIX);
    }

    public List<String> parse(String dataSize) {
        String regex = "(" + REGEX_VALUE + ")" + "(" + REGEX_PREFIX + ")" + "(" + REGEX_SUFFIX + ")";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(dataSize);

        if (!matcher.find()) {
            return List.of();
        }

        String value = matcher.group(1).trim();
        String prefix = matcher.group(2).trim();
        String suffix = matcher.group(3).trim();

        return List.of(value, prefix, suffix);
    }
}
