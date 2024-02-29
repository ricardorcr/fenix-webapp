package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class Teste extends CustomTask {

    @Override
    public void runTask() throws Exception {
        DegreeType.all().filter(type -> type.hasExactlyOneCycleType() && type.isFirstCycle())
                .forEach(type -> taskLog(type.getName().getContent()));
    }
}