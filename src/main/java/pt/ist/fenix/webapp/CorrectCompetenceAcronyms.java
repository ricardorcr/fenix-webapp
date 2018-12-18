package pt.ist.fenix.webapp;

import static pt.ist.fenixframework.FenixFramework.atomic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseInformation;
import org.fenixedu.academic.util.UniqueAcronymCreator;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class CorrectCompetenceAcronyms extends CustomTask {

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {

        Set<CompetenceCourse> competenceCourses = new HashSet<>(CompetenceCourse.readBolonhaCompetenceCourses());
        Multimap<String, CompetenceCourse> existingAcronyms = HashMultimap.create();

        //Correcting non bologna competence course acronym
        atomic(() -> {
            CompetenceCourse cc = FenixFramework.getDomainObject("1691078948291346"); //Políticas de Solos e Fiscalidade Urbana
            CompetenceCourseInformation courseInformation = cc.getCompetenceCourseInformationsSet().iterator().next();
            courseInformation.setName(courseInformation.getName().replace("(", "")); //open brackets without a closing one messes up the acronym generation algorithm
            courseInformation.setNameEn(courseInformation.getNameEn().replace("(", " "));
            cc.setAcronym("PSFU");

            CompetenceCourseInformation cci = FenixFramework.getDomainObject("1627792700847"); //CC 2229088031004 MM, it's more recent than CC 283704064737681
            cci.setAcronym("MM1");
        });

        Set<CompetenceCourse> sane = new HashSet<>();
        Set<CompetenceCourse> insane = new HashSet<>();
        for (final CompetenceCourse cc : competenceCourses) {
            String acronym = cc.getAcronym();
            if (acronym != null) {
                existingAcronyms.put(acronym, cc);
                sane.add(cc);
            }
        }

        Department preBolognaDepartment = Department.find("DPB");
        existingAcronyms.asMap().entrySet().stream().filter(e -> e.getValue().size() > 1).forEach(e -> {
            List<CompetenceCourse> list = e.getValue().stream()
                    .filter(cc -> cc.getDepartmentUnit().getDepartment() == preBolognaDepartment).collect(Collectors.toList());
            int difference = Math.abs(e.getValue().size() - list.size());
            if (difference != 0 && difference != 1) {
                taskLog("Tamanho original: %s - Tamanho depois: %s  - %s %n", e.getValue().size(), list.size(), e.getKey());
            }
            if (e.getValue().size() == list.size()) {
                list = list.subList(1, list.size());
            }
            sane.removeAll(list);
            insane.addAll(list);
        });

        ExecutionSemester actualExecutionSemester = ExecutionSemester.readActualExecutionSemester();
        insane.forEach(cc -> {
            renewAcronym(cc, actualExecutionSemester, sane);
            sane.add(cc);
        });
    }

    private void renewAcronym(CompetenceCourse cc, ExecutionSemester semester, Set<CompetenceCourse> ccs) {
        String attempt;
        try {
            attempt = new UniqueAcronymCreator<>(CompetenceCourse::getName, competence -> competence.getAcronym(), ccs).create(cc)
                    .getLeft();
        } catch (Exception e) {
            if (e.getMessage().equals("unable to create acronym!")) {
                Set<String> acronyms = ccs.stream().map(competence -> competence.getAcronym()).collect(Collectors.toSet());
                String prefix = Stream.of(cc.getName().replaceAll("[^\\sa-zA-Z0-9]", "").split("\\s+"))
                        .map(token -> token.substring(0, 1)).collect(Collectors.joining());
                int count = 0;
                do {
                    attempt = prefix + "-" + count++;
                } while (acronyms.contains(attempt));
            } else {
                taskLog("CC: %s %s %n", cc.getExternalId(), cc.getAcronym());
                throw new RuntimeException(e);
            }
        }
        final String acronym = attempt;
        CompetenceCourseInformation info = cc.findCompetenceCourseInformationForExecutionPeriod(null);
        if (info.getExecutionPeriod().equals(semester)) {
            atomic(() -> info.setAcronym(acronym));
        } else {
            atomic(() -> {
                CompetenceCourseInformation newInfo = new CompetenceCourseInformation(info);
                newInfo.setCompetenceCourse(cc);
                newInfo.setExecutionPeriod(semester);
                newInfo.setAcronym(acronym);
            });
        }
        taskLog("%s %s has new acronym %s %s %s %n", cc.getExternalId(), cc.getName(), attempt,
                cc.getDepartmentUnit().getDepartment().getAcronym(), semester.getQualifiedName());
    }
}