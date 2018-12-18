package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.accessControl.PersistentCoordinatorGroup;
import org.fenixedu.bennu.core.domain.groups.PersistentGroup;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.lang.reflect.Method;

public class DeleteCAMEPPDegree extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Degree degree = FenixFramework.getDomainObject("284236640681992"); //CAMEPP

        degree.getStudentGroupSet().stream().forEach(sg -> sg.setDegree(null));
        degree.getTeacherGroupSet().stream().forEach(tg -> tg.setDegree(null));
        degree.getCoordinatorGroupSet().stream().forEach(cg -> cg.setDegree(null));
        final Method method = Degree.class.getSuperclass().getDeclaredMethod("getThesisManager");
        method.setAccessible(true);
        PersistentCoordinatorGroup thesisManager = (PersistentCoordinatorGroup) method.invoke(degree, null);
        thesisManager.setDegree(null);

        PersistentGroup pg = null;
        final Method methodSet = Degree.class.getSuperclass().getDeclaredMethod("setThesisManager", PersistentGroup.class);
        methodSet.setAccessible(true);
        methodSet.invoke(degree, pg);
        degree.delete();
    }
}
