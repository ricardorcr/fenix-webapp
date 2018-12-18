package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.raides.DegreeDesignation;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.stream.Collectors;

public class CheckMissingUnitsAndDegrees extends CustomTask {

    @Override
    public void runTask() throws Exception {
        List<Unit> units = getUnitsWithCode();
        List<DegreeDesignation> degreeDesignations = getDegreeDesignationsWithCode();

        Set<List<String>> unitsNotInSystem = new HashSet<>();
        Set<List<String>> unitsWhoseNamesDontMatch = new HashSet<>();
        Set<List<String>> degreesNotInSystem = new HashSet<>();
        Set<List<String>> degreesWhoseNamesDontMatch = new HashSet<>();

        URL csvURL = new URL("https://gist.githubusercontent.com/inesfilipe/d873eb941c6cbaf61f18212a6e819f2f/raw/59c1d2af2e5d71e7aef97b85c3b9afe3603b948a/tbl_Grau_Estabelecimento_Curso.csv");
        BufferedReader br = getReaderFromURL(csvURL);
        String line = br.readLine(); // excluding header from analysis

        while((line = br.readLine()) != null) {
            final List<String> data = parseLine(line);

            if(units.stream().noneMatch(u -> isUnitWithSameCode(u, data.get(1)))) {
                unitsNotInSystem.add(data.subList(1,3));
            }

            units.stream().filter(u -> isUnitWithSameCode(u, data.get(1)) && unitNameDoesNotMatch(u, data.get(2)))
                    .forEach(u -> unitsWhoseNamesDontMatch.add(Arrays.asList(u.getCode(), u.getName(), data.get(2))));

            if(degreeDesignations.stream().noneMatch(d -> isDegreeWithSameCodeAndUnit(d, data.get(3), data.get(1)))) {
                degreesNotInSystem.add(data);
            }

            degreeDesignations.stream().filter(d -> isDegreeWithSameCodeAndUnit(d, data.get(3), data.get(1)) && degreeDescriptionDoesNotMatch(d, data.get(4)))
                    .forEach(d -> degreesWhoseNamesDontMatch.add(Arrays.asList(data.get(0), data.get(1), data.get(2), d.getCode(), d.getDescription(), data.get(4))));
        }

        printUnitsNotInSystem(unitsNotInSystem);
        printUnitsWithDifferentName(unitsWhoseNamesDontMatch);
        printDegreesNotInSystem(degreesNotInSystem);
        printDegreesWithDifferentDescription(degreesWhoseNamesDontMatch);
    }

    private boolean isUnitWithSameCode(Unit unit, String code) {
        return code.equals(unit.getCode());
    }

    private boolean unitNameDoesNotMatch(Unit unit, String name) {
        return !name.equals(unit.getName());
    }

    private boolean isDegreeWithSameCodeAndUnit(DegreeDesignation degree, String code, String unitCode) {
        return code.equals(degree.getCode()) && degree.getInstitutionUnitSet().stream()
                .anyMatch(u -> unitCode.equals(u.getCode()));
    }

    private boolean degreeDescriptionDoesNotMatch(DegreeDesignation degree, String description) {
        return !description.equals(degree.getDescription());
    }

    private List<Unit> getUnitsWithCode() {
        return Unit.readAllUnits().stream().filter(u -> u.getCode() != null && !u.getCode().isEmpty()).collect(
                Collectors.toList());
    }

    private List<DegreeDesignation> getDegreeDesignationsWithCode() {
        return Bennu.getInstance().getDegreeDesignationsSet().stream()
                .filter(d -> d.getCode() != null && !d.getCode().isEmpty()).collect(Collectors.toList());
    }

    private void printUnitsNotInSystem(Set<List<String>> units) {
        taskLog("Units not in the system: " + units.size());

        for(List<String> l : sortByUnitCode(units)) {
            taskLog(l.get(0) + " — " + l.get(1));
        }

        taskLog();
    }

    private void printUnitsWithDifferentName(Set<List<String>> units) {
        taskLog("Units with different name: " + units.size());

        for(List<String> l : sortByUnitCode(units)) {
            taskLog(l.get(0) + " — old name: " + l.get(1) + "; new name: " + l.get(2));
        }

        taskLog();
    }

    private List<List<String>> sortByUnitCode(Set<List<String>> set) {
        return set.stream().sorted(Comparator.comparing(l -> l.get(0))).collect(Collectors.toList());
    }

    private void printDegreesNotInSystem(Set<List<String>> degrees) {
        taskLog("Degrees not in the system: " + degrees.size());

        for(List<String> l : sortByUnitAndDegreesCodes(degrees)) {
            taskLog(l.get(1) + " : " + l.get(2) + " — " + l.get(0) + " — " + l.get(3) + " : " + l.get(4));
        }

        taskLog();
    }

    private void printDegreesWithDifferentDescription(Set<List<String>> degrees) {
        taskLog("Degrees with different description: " + degrees.size());

        for(List<String> l : sortByUnitAndDegreesCodes(degrees)) {
            taskLog(l.get(1) + " : " + l.get(2) + " — " + l.get(0) + " — "  + l.get(3) + " : old name: " + l.get(4) + "; new name: " + l.get(5));
        }

        taskLog();
    }

    private List<List<String>> sortByUnitAndDegreesCodes(Set<List<String>> set) {
        Comparator<List<String>> degreeComparator = Comparator.comparing((List<String> l) -> l.get(1)).thenComparing(l -> l.get(3));
        return set.stream().sorted(degreeComparator).collect(Collectors.toList());
    }

    private BufferedReader getReaderFromURL(URL url) throws IOException {
        URLConnection urlConn = url.openConnection();
        InputStreamReader input = new InputStreamReader(urlConn.getInputStream());
        return new BufferedReader(input);
    }

    //assumes separator is a comma and that there are no quotes inside a column
    private List<String> parseLine(String line) {
        List<String> result = new ArrayList<>();

        //if empty, return!
        if (line == null || line.isEmpty()) {
            return result;
        }

        StringBuilder curVal = new StringBuilder();
        boolean inQuotes = false;

        char[] chars = line.toCharArray();

        for (char ch : chars) {
            if (inQuotes) {
                if (ch == '\"') {
                    inQuotes = false;
                } else {
                    curVal.append(ch);
                }
            } else {
                if (ch == '\"') {
                    inQuotes = true;
                } else if (ch == ',') {
                    result.add(curVal.toString());

                    curVal = new StringBuilder();
                } else if (ch == '\n') {
                    //the end, break!
                    break;
                } else if (ch != '\r') {
                    curVal.append(ch);
                }
            }

        }

        result.add(curVal.toString());

        return result;
    }

}