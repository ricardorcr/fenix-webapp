package pt.ist.fenix.webapp;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.spaces.domain.Space;

import pt.ist.fenixframework.FenixFramework;

public class FirstTimeTestData extends CustomTask {

    private static final String INPUT_FILENAME =
            "/home/rcro/Documents/fenix/inscricoes/2016_2017/1ano1vez/Colocados por Curso - dados fenix – taguspark-UTF8.txt";
    private static final String OUTPUT_FILENAME =
            "/home/rcro/Documents/fenix/inscricoes/2017_2018/1ano1vez/colocados_tagus_teste.txt";
    //private static final String CAMPUS_OID = "2448131360897"; // Alameda
    private static final String CAMPUS_OID = "2448131360898"; // Tagus

    private final Random random = new Random();

    @Override
    public void runTask() throws Exception {
        final File in = new File(INPUT_FILENAME);
        try (final PrintWriter out = new PrintWriter(OUTPUT_FILENAME)) {
            Files.readAllLines(in.toPath()).forEach(line -> {
                final String[] parts = line.split("\t");
                final String degreeCode = degreeCode(parts[1]);
                if (degreeCode != null) {
                    final String idNumber = newIdNumber();
                    final String check = newCheck(idNumber);
                    for (int i = 0; i < parts.length; i++) {
                        final String part = parts[i];
                        if (i > 0) {
                            out.print("\t");
                        }
                        if (i == 2) {
                            out.print(idNumber);
                        } else if (i == 5) {
                            out.print(check);
                        } else {
                            out.print(part);
                        }
                    }
                    out.println();
                    }
                });
        }
    }

    private String degreeCode(final String part) {
        final Space campus = FenixFramework.getDomainObject(CAMPUS_OID);
        final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();

        try {
            final ExecutionDegree executionDegree =
                    ExecutionDegree.readByDegreeCodeAndExecutionYearAndCampus(part, executionYear, campus);
            if (executionDegree != null) {
                return part;
            }
        } catch (NullPointerException ex) {
        }
        return null;
    }

    private String newIdNumber() {
        final String id = Long.toString(Math.abs(random.nextLong()));
        return validNewIdNumber(id.length() == 8 ? id : id.length() < 8 ? fill(id) : id.substring(0, 8));
    }

    private String validNewIdNumber(final String n) {
        return calculateBICheckDigit(n) == null ? newIdNumber() : n;
    }

    private String fill(final String id) {
        return StringUtils.leftPad(id, 8, '0');
    }

    private String newCheck(final String num) {
        final Integer biCheckDigit = calculateBICheckDigit(num);
        final String version = "ZZ";
        final String biNum = num + biCheckDigit;
        final String ccNumPrefix = biNum + version;
        return biCheckDigit.toString() + version + calculateCCCheckDigit(ccNumPrefix).toString();
    }

    private Integer calculateBICheckDigit(final String documentNumber) {
        for (int i = 0; i <= 9; i++) {
            if (isValidBI(documentNumber + i)) {
                return Integer.valueOf(i);
            }
        }
        return null;
    }

    private Integer calculateCCCheckDigit(final String documentNumber) {
        for (int i = 0; i <= 9; i++) {
            if (isValidCC(documentNumber + i)) {
                return Integer.valueOf(i);
            }
        }
        return null;
    }

    final static int[] factor = new int[] { 9, 8, 7, 6, 5, 4, 3, 2 };

    private static boolean isValidBI(final String num) {
        final int l = num.length();
        if (l == 9) {
            int sum = 0;
            for (int i = 0; i < l - 1; sum += toInt(num.charAt(i)) * factor[i++]);
            int checkDigit = toInt(num.charAt(l - 1));
            final int mod = sum % 11;
            return mod == 0 || mod == 1 ? checkDigit == 0 : checkDigit == 11 - mod;
        }
        return l < 9 && isValidBI("0" + num);
    }

    private boolean isValidCC(final String num) {
        final int l = num.length();
        if (l == 12) {
            int sum = 0;
            for (int i = 0; i < l; i++, i++) {
                final char c0 = num.charAt(i);
                final char c1 = num.charAt(i + 1);

                if (i != 8 && !Character.isDigit(c1)) {
                    return false;
                }
                if (i != 10 && !Character.isDigit(c0)) {
                    return false;
                }

                final int d0 = toInt(c0) * 2;
                final int d1 = toInt(c1);

                final int d09 = d0 > 9 ? d0 - 9 : d0;
                sum += d09 + d1;
            }
            return sum % 10 == 0;
        }
        return false;
    }

    private static int toInt(final char c) {
        return Character.isDigit(c) ? Character.getNumericValue(c) : ((int) c) - ((int) 'A') + 10;
    }

}
