package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeHelper;
import org.fenixedu.academic.domain.accessControl.PersistentCoordinatorGroup;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixframework.FenixFramework;

public class AssociateThesisManagerGroup extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Degree degree = FenixFramework.getDomainObject("284236640681988"); //MEST
        PersistentCoordinatorGroup pcg =
                PersistentCoordinatorGroup.getInstance(DegreeType.matching(DegreeType::isBolonhaMasterDegree).get(), degree);

        DegreeHelper.setCanManageThesis(degree, pcg.toGroup());
    }

}
