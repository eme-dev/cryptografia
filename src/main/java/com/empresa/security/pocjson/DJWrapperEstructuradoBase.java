package com.marchena.aiocr.djpoc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DJWrapperEstructuradoBase {
    private static final String DIGITS_REGEX = "^\\d+$";
    private static final String BOX_START_REGEX = "^(\\d{3})(?:\\s(\\S+))?";

    public static void main(String[] args) {
        String[][] statementFinancialHistorical = {
                {
                        "Efectivo y equivalentes de efectivo",
                        "359",
                        "455653",
                        "Sobregiros bancarios",
                        "401",
                        "(356"
                },
                /*{
                        "Efectivo y equivalentes de efectivo 360",
                        "455653",
                        "Sobregiros bancarios",
                        "402",
                        "0"
                },
                {
                        "Efectivo y equivalentes de efectivo",
                        "361 455653",
                        "Sobregiros bancarios",
                        "403",
                        "3591"
                },
                {
                        "Efectivo y equivalentes de efectivo",
                        "362",
                        "455653",
                        "Sobregiros bancarios 404",
                        "1200"
                },*/
                {
                        "Efectivo y equivalentes de efectivo",
                        "363",
                        "455653",
                        "Sobregiros bancarios 405",
                        "3590"
                }
                /*{
                        "Efectivo y equivalentes de efectivo 373",
                        "455653 Sobregiros bancarios 405",
                        "3590"
                }*/
        };

        Map<Integer, String> financialSituation = new LinkedHashMap<>();
        extractFinancialSituation(statementFinancialHistorical, financialSituation);

        System.out.println("financialSituation = " + financialSituation);

    }

    private static void extractFinancialSituation(String[][] statementFinancialHistorical,
                                                  Map<Integer, String> financialSituation) {

        for (String[] strings : statementFinancialHistorical) {
            if (strings[0] != null && !strings[0].contains("Estado de Situación Financiera ( Balance General")) {
                Pattern pattern = Pattern.compile("[a-zA-Z]\\s(\\d{3})(?:\\s(\\S+))?");
                Matcher matcher = pattern.matcher(strings[0]);
                if (matcher.find()) {
                    String box = matcher.group(1);
                    String value1 = getAmountValues(strings, 1);
                    String value = matcher.group(2) != null ? matcher.group(2) : value1;
                    financialSituation.put(Integer.valueOf(box), value);
                }
            }

            if (strings[1] != null) {
                if (strings[1].length() == 3 && strings[1].matches(DIGITS_REGEX)) {
                    String value2 = getAmountValues(strings, 2);
                    financialSituation.put(Integer.valueOf(strings[1]), value2);
                } else {
                    Pattern pattern = Pattern.compile(BOX_START_REGEX);
                    Matcher matcher = pattern.matcher(strings[1]);
                    if (matcher.find()) {
                        String box1 = matcher.group(1);
                        String value1 = getAmountValues(strings, 1);
                        String value = matcher.group(2) != null ? matcher.group(2) : value1;
                        financialSituation.put(Integer.valueOf(box1), value);
                    }
                }
            }

            if (strings.length > 3 && strings[3] != null) {
                if (strings[3].length() == 3 && strings[3].matches(DIGITS_REGEX)) {
                    String value3 = getAmountValues(strings, 4);
                    financialSituation.put(Integer.valueOf(strings[3]), value3);
                } else {
                    Pattern pattern1 = Pattern.compile("[a-zA-Z]\\s(\\d{3})(?:\\s(\\S+))?");
                    Matcher matcher1 = pattern1.matcher(strings[3]);
                    if (matcher1.find()) {
                        String box3 = matcher1.group(1);
                        String value4 = getAmountValues(strings, 4);
                        String value = matcher1.group(2) != null ? matcher1.group(2) : value4;
                        financialSituation.put(Integer.valueOf(box3), value);
                    } else {
                        Pattern pattern2 = Pattern.compile(BOX_START_REGEX);
                        Matcher matcher2 = pattern2.matcher(strings[3]);
                        if (matcher2.find()) {
                            String box3 = matcher2.group(1);
                            String value4 = getAmountValues(strings, 4);
                            String value = matcher2.group(2) != null ? matcher2.group(2) : value4;
                            financialSituation.put(Integer.valueOf(box3), value);
                        }
                    }
                }
            }

            if (strings.length > 4 && strings[4] != null) {
                if (strings[4].length() == 3 && strings[4].matches(DIGITS_REGEX)) {
                    String value4 = getAmountValues(strings, 5);
                    financialSituation.put(Integer.valueOf(strings[4]), value4);
                } else {
                    Pattern pattern = Pattern.compile(BOX_START_REGEX);
                    Matcher matcher = pattern.matcher(strings[4]);
                    if (matcher.find()) {
                        String box4 = matcher.group(1);
                        String value4 = getAmountValues(strings, 4);
                        String value = matcher.group(2) != null ? matcher.group(2) : value4;
                        financialSituation.put(Integer.valueOf(box4), value);
                    }
                }
            }
        }
    }

    private static String getAmountValues(String[] strings, int index) {
        String amountValue = "0";
        if (strings != null && index < strings.length && strings[index] != null) {
            return (strings[index] == null || strings[index].isEmpty()
                    || (strings[index].charAt(0) != '(' && !Character.isDigit(strings[index].charAt(0)))
            ) ? "0" : strings[index];
        }
        return amountValue;
    }
}
