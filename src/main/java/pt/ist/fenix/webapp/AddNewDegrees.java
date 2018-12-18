package org.fenixedu.academic.task;

import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.raides.DegreeClassification;
import org.fenixedu.academic.domain.raides.DegreeDesignation;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AddNewDegrees extends CustomTask {
    private List<List<String>> createdDegrees = new ArrayList<>();
    private List<List<String>> erroredDegrees = new ArrayList<>();
    private List<List<String>> updatedDegrees = new ArrayList<>();

    @Override
    public void runTask() throws Exception {
        List<DegreeDesignation> degreeDesignations;
        List<String> tecnicoCodes = Arrays.asList("0807", "0808", "1518", "1519");

        URL csvURL = new URL("https://gist.githubusercontent.com/inesfilipe/d873eb941c6cbaf61f18212a6e819f2f/raw/59c1d2af2e5d71e7aef97b85c3b9afe3603b948a/tbl_Grau_Estabelecimento_Curso.csv");
        BufferedReader br = getReaderFromURL(csvURL);
        String line = br.readLine(); // excluding header from analysis

        while((line = br.readLine()) != null) {
            final List<String> data = parseLine(line);

            degreeDesignations = getDegreeDesignationsWithCode();
            if(!tecnicoCodes.contains(data.get(1))) {
                if(degreeDesignations.stream().noneMatch(d -> isDegreeWithSameCodeAndUnit(d, data.get(3), data.get(1)))) {
                    addDegreeToSystem(data);
                }
                else if(degreeDesignations.stream().anyMatch(d -> isDegreeWithSameCodeAndUnit(d, data.get(3), data.get(1)) && !data.get(4).equals(d.getDescription()))) {
                    updateDegreeName(data);
                }
            }
        }

        printErroredDegrees(erroredDegrees);
        printCreatedDegrees(createdDegrees);
        printUpdatedDegrees(updatedDegrees);
    }

    private void addDegreeToSystem(List<String> degree) {
        if(getDegreeClassificationFromName(degree.get(0)) == null) {
            erroredDegrees.add(degree);
            return;
        }

        getUnitsWithCode().stream().filter(u -> isUnitWithSameCode(u, degree.get(1))).findFirst().ifPresent(unit -> {
            DegreeDesignation degreeDesignation = getDegreeDesignationsWithCode().stream()
                    .filter(d -> degree.get(3).equals(d.getCode())).findFirst().orElseGet(() -> new DegreeDesignation(degree.get(3), degree.get(4), getDegreeClassificationFromName(degree.get(0))));
            unit.addDegreeDesignation(degreeDesignation);
            createdDegrees.add(degree);
        });

    }

    private void updateDegreeName(List<String> degree) {
        getDegreeDesignationsWithCode().stream().filter(d -> isDegreeWithSameCodeAndUnit(d, degree.get(3), degree.get(1))).findFirst().ifPresent(d -> d.setDescription(degree.get(4)));
        updatedDegrees.add(degree);
    }

    private void printErroredDegrees(List<List<String>> erroredDegrees) {
        taskLog("Error creating the following degrees: " + erroredDegrees.size());
        printDegrees(erroredDegrees);
        taskLog();
    }

    private void printCreatedDegrees(List<List<String>> createdDegrees) {
        taskLog("Created the following degrees: " + createdDegrees.size());
        printDegrees(createdDegrees);
        taskLog();
    }

    private void printUpdatedDegrees(List<List<String>> updatedDegrees) {
        taskLog("Updated the following degrees: " + updatedDegrees.size());
        printDegrees(updatedDegrees);
        taskLog();
    }

    private void printDegrees(List<List<String>> degrees) {
        for(List<String> d : degrees) {
            taskLog(d.get(1) + " : " + d.get(2) + " — " + d.get(0) + " — " + d.get(3) + " : " + d.get(4));
        }
    }

    //FIXME: DegreeClassification table has to be updated
    private DegreeClassification getDegreeClassificationFromName(String name) {
        switch(name) {
        case "Curso superior não conferente de grau":
            return DegreeClassification.readByCode("A");
        case "Bacharelato":
            return DegreeClassification.readByCode("B");
        case "Bacharelato/Licenciatura":
            return DegreeClassification.readByCode("BL");
        case "Curso de especialização tecnológica":
            return DegreeClassification.readByCode("C"); //file doesn't specify which code ("duplicated" name) - default to C according to ticket
        case "Complemento de formação":
            return DegreeClassification.readByCode("CF");
        case "Doutoramento":
            return DegreeClassification.readByCode("D");
        case "Doutoramento 3.º ciclo":
            return DegreeClassification.readByCode("D3");
        case "Diploma de estudos superiores especializados":
            return DegreeClassification.readByCode("DE");
        case "Especialização pós-licenciatura":
            return DegreeClassification.readByCode("E");
        case "Especialização pós-bacharelato":
            return DegreeClassification.readByCode("GB");
        case "Licenciatura":
            return DegreeClassification.readByCode("L");
        case "Licenciatura 1.º ciclo":
            return DegreeClassification.readByCode("L1");
        case "Licenciatura bietápica":
            return DegreeClassification.readByCode("LB");
        case "Licenciatura de mestrado integrado":
            return DegreeClassification.readByCode("LI");
        case "Licenciatura terminal":
            return DegreeClassification.readByCode("LT");
        case "Mestrado":
            return DegreeClassification.readByCode("M");
        case "Mestrado 2.º ciclo":
            return DegreeClassification.readByCode("M2");
        case "Mestrado integrado":
            return DegreeClassification.readByCode("MI");
        case "Mestrado integrado terminal":
            return DegreeClassification.readByCode("MT");
        case "Outros cursos de compl de form para prof do Ens Básico e Sec":
            return DegreeClassification.readByCode("OC");
        case "Preparatórios de licenciatura":
            return DegreeClassification.readByCode("P");
        case "Bacharelato em ensino + licenciatura em ensino":
            return DegreeClassification.readByCode("PB");
        case "Preparatórios de licenciatura 1.º ciclo":
            return DegreeClassification.readByCode("PL");
        case "Preparatórios de mestrado integrado":
            return DegreeClassification.readByCode("PM");
        case "Qualificação para o exercício de outras funções educativas":
            return DegreeClassification.readByCode("QE");
        case "Aguarda reconhecimento como licenciatura":
            return DegreeClassification.readByCode("X");
        case "Curso técnico superior profissional":
            return DegreeClassification.readByCode("T"); //this code doesn't exist!!!
        default:
            return null;
        }
    }

    private List<Unit> getUnitsWithCode() {
        return Unit.readAllUnits().stream().filter(u -> u.getCode() != null && !u.getCode().isEmpty()).collect(
                Collectors.toList());
    }

    private List<DegreeDesignation> getDegreeDesignationsWithCode() {
        return Bennu.getInstance().getDegreeDesignationsSet().stream()
                .filter(d -> d.getCode() != null && !d.getCode().isEmpty()).collect(Collectors.toList());
    }

    private boolean isUnitWithSameCode(Unit unit, String code) {
        return code.equals(unit.getCode());
    }

    private boolean isDegreeWithSameCodeAndUnit(DegreeDesignation degree, String code, String unitCode) {
        return code.equals(degree.getCode()) && degree.getInstitutionUnitSet().stream()
                .anyMatch(u -> unitCode.equals(u.getCode()));
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
