package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.groups.PersistentUserGroup;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.cms.domain.CmsSettings;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;
import java.util.List;

public class Teste extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final List<String> istIDs = Arrays.asList("ist12543","ist425049","ist23025","ist12288","ist12177","ist11861","ist34777","ist180963","ist154802","ist181943","ist24518","ist23000","ist12736","ist12662","ist11992","ist24299","ist12886","ist187617","ist168193","ist23886","ist23717");
        for (String istID : istIDs) {
            final User user = User.findByUsername(istID);
            taskLog("%s\t%s", istID, user.getDisplayName());
        }
    }
}