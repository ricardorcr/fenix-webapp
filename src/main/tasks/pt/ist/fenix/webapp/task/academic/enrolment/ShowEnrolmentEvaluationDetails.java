package pt.ist.fenix.webapp.task.academic.enrolment;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;

public class ShowEnrolmentEvaluationDetails extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        User.findByUsername("ist195692").getPerson().getStudent().getRegistrationsSet().forEach(registration -> {
            taskLog("Registration: %s%n", registration.getDegree().getPresentationName());
            registration.getStudentCurricularPlanStream().forEach(scp -> {
                taskLog("   %s%n", scp.getName());
                scp.getEnrolmentStream().forEach(enrolment -> {
                    taskLog("      %s%n", enrolment.getName().getContent());
                    taskLog("         peso: %s : peso curriculo: %s : ects: %s ecto curriculo: %s%n",
                            enrolment.getWeigth(),
                            enrolment.getWeigthForCurriculum(),
                            enrolment.getEctsCredits(),
                            enrolment.getEctsCreditsForCurriculum()
                            );
                    enrolment.getEvaluationsSet().forEach(ee -> {
                        taskLog("         %s : %s : %s : %s : %s : %s : %s%n",
                                ee.getExternalId(),
                                ee.getWhenDateTime(),
                                ee.getEvaluationSeason().getAcronym().getContent(),
                                ee.getGradeValue(),
                                ee.getMarkSheet() == null ? "null" : ee.getMarkSheet().getCreationDateDateTime().toString(),
                                ee.getRectificationMarkSheet() == null ? "null" : ee.getRectificationMarkSheet().getCreationDateDateTime().toString(),
                                ee.getPerson() == null ? "" : ee.getPerson().getUsername()
                        );
                    });
                });
            });
        });
    }

}