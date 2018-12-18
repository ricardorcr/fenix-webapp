package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseInformation;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class FixOldCoursesNames extends CustomTask {

    @Override
    public void runTask() throws Exception {
        updatePortugueseNames();
        updateEnglishNames();
        throw new Exception();
    }

    private void updatePortugueseNames() {
//        taskLog("------------- Portuguese Names -------------");
//        Bennu.getInstance()
//                .getCompetenceCoursesSet()
//                .stream()
//                .filter(cc -> !cc.isBolonha()
//                        && cc.getAssociatedCurricularCoursesSet().size() > 0
//                        && cc.getAssociatedCurricularCoursesSet().stream().map(course -> course.getName())
//                                .collect(Collectors.toSet()).size() > 1)
//                .forEach(
//                        cc -> {
//                            taskLog("%s - Competência: %s\n", cc.getAssociatedCurricularCoursesSet().iterator().next()
//                                    .getDegree().getSigla(), cc.getName());
//                            cc.getAssociatedCurricularCoursesSet().stream().map(course -> course.getName())
//                                    .collect(Collectors.toSet()).forEach(name -> taskLog("Curricular: %s\n", name));
//                            String chosenCourseName =
//                                    cc.getAssociatedCurricularCoursesSet().stream()
//                                            .max(Comparator.comparing(course -> course.getName().length())).get().getName();
//
//                            if (chosenCourseName.equals(cc.getName())) {
//                                taskLog("Nome escolhido: %s\n--------------------\n", chosenCourseName);
//                            } else {
//                                if (cc.getName().length() >= chosenCourseName.length()) {
//                                    taskLog("Nome escolhido: %s\n--------------------\n", cc.getName());
//                                } else {
//                                    cc.setName(chosenCourseName);
//                                    taskLog("Nome escolhido: %s\n--------------------\n", chosenCourseName);
//                                }
//                            }
//                            cc.getAssociatedCurricularCoursesSet().stream().forEach(course -> course.setName(null));
//                        });
//
//        Bennu.getInstance()
//                .getCompetenceCoursesSet()
//                .stream()
//                .filter(cc -> !cc.isBolonha()
//                        && cc.getAssociatedCurricularCoursesSet().size() > 0
//                        && cc.getAssociatedCurricularCoursesSet().stream().map(course -> course.getName())
//                                .collect(Collectors.toSet()).size() == 1).forEach(cc -> {
//                    CurricularCourse curricularCourse = cc.getAssociatedCurricularCoursesSet().iterator().next();
//                    String courseName = curricularCourse.getName();
//                    if (!courseName.equals(cc.getName())) {
//                        taskLog("%s - Competência: %s\n", curricularCourse.getDegree().getSigla(), cc.getName());
//                        taskLog("Curricular: %s\n", courseName);
//                        if (courseName.length() > cc.getName().length()) {
//                            cc.setName(courseName);
//                            taskLog("Nome escolhido: %s\n--------------------\n", courseName);
//                        } else {
//                            taskLog("Nome escolhido: %s\n--------------------\n", cc.getName());
//                        }
//                    }
//                    //now we can clean all the curricular courses names
//                        cc.getAssociatedCurricularCoursesSet().stream().forEach(course -> course.setName(null));
//                    });
    }

    private void updateEnglishNames() {
//        taskLog("------------- English Names -------------");
//        Bennu.getInstance()
//                .getCompetenceCoursesSet()
//                .stream()
//                .filter(cc -> !cc.isBolonha()
//                        && cc.getAssociatedCurricularCoursesSet().size() > 0
//                        && cc.getAssociatedCurricularCoursesSet().stream().map(course -> course.getNameEn())
//                                .filter(Objects::nonNull).collect(Collectors.toSet()).size() > 1)
//                .forEach(
//                        cc -> {
//                            taskLog("%s - Competência: %s\n", cc.getAssociatedCurricularCoursesSet().iterator().next()
//                                    .getDegree().getSigla(), cc.getNameEn());
//                            cc.getAssociatedCurricularCoursesSet().stream().map(course -> course.getNameEn())
//                                    .collect(Collectors.toSet()).forEach(name -> taskLog("Curricular: %s\n", name));
//                            String chosenCourseName =
//                                    cc.getAssociatedCurricularCoursesSet().stream()
//                                            .max(Comparator.comparing(course -> course.getNameEn().length())).get().getNameEn();
//                            boolean toDelete = true;
//                            if (chosenCourseName.equals(cc.getNameEn())) {
//                                taskLog("Nome escolhido: %s\n--------------------\n", chosenCourseName);
//                            } else {
//                                if (cc.getNameEn() != null && cc.getNameEn().length() >= chosenCourseName.length()) {
//                                    taskLog("Nome escolhido: %s\n--------------------\n", cc.getNameEn());
//                                } else {
//                                    CompetenceCourseInformation cci = getLastCompetenceCourseInformation(cc);
//                                    if (cci != null) {
//                                        cci.setNameEn(chosenCourseName);
//                                        taskLog("Nome escolhido: %s\n--------------------\n", cc.getName());
//                                    } else {
//                                        taskLog("Nomes inglês foram mantidos nas curriculares da competência: %s - %s\n",
//                                                cc.getExternalId(), chosenCourseName);
//                                        toDelete = false;
//                                    }
//                                }
//                            }
//                            if (toDelete) {
//                                cc.getAssociatedCurricularCoursesSet().stream().forEach(course -> course.setNameEn(null));
//                            }
//                        });
//
//        Bennu.getInstance()
//                .getCompetenceCoursesSet()
//                .stream()
//                .filter(cc -> !cc.isBolonha()
//                        && cc.getAssociatedCurricularCoursesSet().size() > 0
//                        && cc.getAssociatedCurricularCoursesSet().stream().map(course -> course.getNameEn())
//                                .filter(Objects::nonNull).collect(Collectors.toSet()).size() == 1)
//                .forEach(
//                        cc -> {
//                            CurricularCourse curricularCourse = cc.getAssociatedCurricularCoursesSet().iterator().next();
//                            String courseName = curricularCourse.getNameEn();
//                            boolean toDelete = true;
//                            if (!courseName.equals(cc.getNameEn())) {
//                                if (cc.getNameEn() != null) {
//                                    taskLog("%s - Competência: %s\n", curricularCourse.getDegree().getSigla(), cc.getNameEn());
//                                    taskLog("%s\n", courseName);
//                                }
//                                if (cc.getNameEn() == null || courseName.length() > cc.getNameEn().length()) {
//                                    CompetenceCourseInformation cci = getLastCompetenceCourseInformation(cc);
//                                    if (cci != null) {
//                                        cci.setNameEn(courseName);
//                                        if (cc.getNameEn() != null) {
//                                            taskLog("Nome escolhido: %s\n--------------------\n", courseName);
//                                        }
//                                    } else {
//                                        taskLog("Nomes inglês foram mantidos nas curriculares da competência: %s - %s\n",
//                                                cc.getExternalId(), cc.getName());
//                                        toDelete = false;
//                                    }
//                                } else {
//                                    taskLog("Nome escolhido: %s\n--------------------\n", cc.getNameEn());
//                                }
//                            }
//                            if (toDelete) {
//                                //now we can clean all the curricular courses names
//                                cc.getAssociatedCurricularCoursesSet().stream().forEach(course -> course.setNameEn(null));
//                            }
//                        });
    }

    private CompetenceCourseInformation getLastCompetenceCourseInformation(CompetenceCourse competenceCourse) {
        return competenceCourse.getCompetenceCourseInformationsSet().stream()
                .max(CompetenceCourseInformation.COMPARATORY_BY_EXECUTION_PERIOD).orElse(null);
    }
}
