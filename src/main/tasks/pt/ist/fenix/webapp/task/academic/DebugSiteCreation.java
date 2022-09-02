package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;

import java.lang.ref.SoftReference;
import java.lang.reflect.Method;

public class DebugSiteCreation extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        final Method method = Degree.class.getDeclaredMethod("loadCache");
        method.setAccessible(true);
        method.invoke(null);

        final Degree degree = Degree.readBySigla("Min-EG");
        taskLog("Degree = %s%n", degree);

        for (final Degree d : Degree.readNotEmptyDegrees()) {
            taskLog("   [%s] = %s%n", d.getSigla(), d.getPresentationName());
        }
    }
}