package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicAccessRule;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicOperationType;
import org.fenixedu.academic.domain.accessControl.rules.AccessRule_Base;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.groups.PersistentGroup;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

public class CleanUpPermissions extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Group activeStudents = Group.parse("activeStudents");
        AcademicAccessRule.accessRules()
                .filter(r -> r.getOperation() == AcademicOperationType.STUDENT_ENROLMENTS)
                .peek(r -> taskLog("Rule = %s %s%n", r.getExternalId(), r.getOperation().name()))
                .forEach(rule -> {
                    final Set<User> users = members(rule).stream()
                            .filter(u ->  activeStudents.isMember(u))
                            .collect(Collectors.toSet());
                    if (users.size() == 1) {
                        rule.revoke();
                    } else if (activeStudents.isMember(rule.getCreator())) {
                        taskLog("Removed users from rule %s - criada pelo aluno%n", rule.getExternalId());
                        users.forEach(u -> rule.revoke(u));
                    } else {
                        taskLog("Remover todos?? %s%n", rule.getExternalId());
//                        users.forEach(u -> rule.revoke(u));
                    }
                });
        taskLog("Done");
    }

    private Set<User> members(final AcademicAccessRule rule) {
        try {
            final Method method = AccessRule_Base.class.getDeclaredMethod("getPersistentGroup");
            method.setAccessible(true);
            final PersistentGroup persistentGroup = (PersistentGroup) method.invoke(rule);
            final Group group = persistentGroup.toGroup();
            return group.getMembers().collect(Collectors.toSet());
        } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new Error(e);
        }
    }

}