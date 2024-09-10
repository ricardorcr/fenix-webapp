package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import java.util.Arrays;
import java.util.List;

public class InitExtraCurricularActivityType extends CustomTask {
    @Override
    public void runTask() throws Exception {
        final List<String> setActive =  Arrays.asList("3382750602067970","1975375718514711","1693900741804127","1693900741804054","1693900741804043","1412425765093399",
                "1412425765093387","1130950788382833","1130950788382784","1130950788382725","1130950788382723","1130950788382721","849475811672074","849475811672070",
                "849475811672067","286525858250756","286525858250754","286525858250753"); //núcleos
        Bennu.getInstance().getExtraCurricularActivityTypeSet()
                .forEach(ea -> {
                    if (setActive.contains(ea.getExternalId())) {
                        ea.setActive(true);
                    }
                });
    }
}
