package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicAccessRule;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class UpdateUnitAcademicAuthorizationGroup extends CustomTask {

    @Override
    public void runTask() throws Exception {
        AcademicAccessRule.accessRules()
                .map(rule -> rule.getWhoCanAccess().getClass().getName())
                .distinct()
                .forEach(this::taskLog);

        AcademicAccessRule.accessRules()
                .forEach(rule -> taskLog("%s\t%s%n", rule.getExternalId(),
                        rule.getWhoCanAccess().getClass().getName()));

//        AcademicAccessRule.accessRules().map(AuthorizationGroupBean::new)
//                .filter(authBean -> authBean.getParty() != null && authBean.getParty() instanceof Unit)
//                .forEach(authBean -> {
//                    authBean.setParty(authBean.getParty());
//                    authBean.edit();
//                });
    }
}
