package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.transitions.domain.DegreeCurricularTransitionPlan;
import org.fenixedu.academic.transitions.domain.DegreeModulePath;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FixAggregatedTransitionRules extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getDegreeCurricularPlansSet().stream()
                .flatMap(dcp -> dcp.getDestinationTransitionPlanSet().stream())
                .forEach(this::fixTP);
    }

    private void fixTP(DegreeCurricularTransitionPlan dctp) {
        Set<DegreeModulePath> destinationModules = new HashSet<>();
        dctp.getTransitionPlanRuleSet().forEach(tpr -> {
            if (tpr.getDestinationDegreeModulePathSet().size() == 1 && tpr.getOriginDegreeModulePathSet().size() == 1) {
                final DegreeModulePath destinationPath = tpr.getDestinationDegreeModulePathSet().iterator().next();
                final DegreeModulePath originPath = tpr.getOriginDegreeModulePathSet().iterator().next();
                if (destinationPath.getDegreeModule().isCurricularCourse() && originPath.getDegreeModule().isCurricularCourse()) {
                    CurricularCourse originCC = (CurricularCourse) originPath.getDegreeModule();
                    CurricularCourse destinationCC = (CurricularCourse) destinationPath.getDegreeModule();
                    if (originCC.getEctsCredits() < destinationCC.getEctsCredits()) {
                        destinationModules.add(destinationPath);
                    }
                }
            }
        });
        destinationModules.forEach(dm -> {
            if (dm.is$$do$$Valid()) {
                final Set<DegreeModulePath> equalPaths = findEqualPaths(dm, destinationModules);
                if (equalPaths.size() >= 1) {
                    final Double destinationCredits = ((CurricularCourse) dm.getDegreeModule()).getEctsCredits();
                    equalPaths.add(dm);
                    final Double originCredits = equalPaths.stream()
                            .flatMap(dmp -> dmp.getOriginTransitionPlanRule().getOriginDegreeModulePathSet().stream())
                            .map(dmp -> (CurricularCourse) dmp.getDegreeModule())
                            .map(CurricularCourse::getEctsCredits)
                            .reduce(0.0, Double::sum);
                    DegreeCurricularTransitionPlan dtp = dm.getOriginTransitionPlanRule().getDegreeCurricularTransitionPlan();
                    taskLog("##%s -> %s%n", dtp.getOriginDegreeCurricularPlan().getName(), dtp.getDestinationDegreeCurricularPlan().getName());
                    taskLog("Origens");
                    equalPaths.stream()
                            .map(dmp -> (CurricularCourse) dmp.getOriginTransitionPlanRule().getOriginDegreeModulePathSet().iterator().next().getDegreeModule())
                            .forEach(cc -> taskLog("\t%s %s%n", cc.getName(), cc.getEctsCredits()));
                    taskLog("Destino");
                    taskLog("\t%s %s%n", dm.getDegreeModule().getName(), ((CurricularCourse) dm.getDegreeModule()).getEctsCredits());
                    if (originCredits.doubleValue() == destinationCredits.doubleValue()) {
                        fix(equalPaths);
                    }
                }
            }
        });
    }

    private void fix(Set<DegreeModulePath> destinationModules) {
        final List<DegreeModulePath> originDMPs = destinationModules.stream()
                .map(dmp -> dmp.getOriginTransitionPlanRule().getOriginDegreeModulePathSet().iterator().next())
                .collect(Collectors.toList());

        Set<DegreeModule[]> destinationArraySet = new HashSet<>();
        ArrayList<DegreeModule> destinationArrayList = new ArrayList<>();
        final DegreeModulePath destinationModulePath = destinationModules.iterator().next();
        buildList(destinationModulePath, destinationArrayList);
        final DegreeModule[] destinationArray = new DegreeModule[destinationArrayList.size()];
        destinationArrayList.toArray(destinationArray);
        destinationArraySet.add(destinationArray);

        final DegreeCurricularTransitionPlan degreeCurricularTransitionPlan = destinationModulePath.getOriginTransitionPlanRule().getDegreeCurricularTransitionPlan();

        Set<DegreeModule[]> originsArraySet = new HashSet<>();
        originDMPs.forEach(dmp -> {
            ArrayList<DegreeModule> arrayList = new ArrayList<>();
            buildList(dmp, arrayList);
            final DegreeModule[] originArray = new DegreeModule[arrayList.size()];
            arrayList.toArray(originArray);
            originsArraySet.add(originArray);
            dmp.getDestinationTransitionPlanRule().delete();
        });

        printInfo(degreeCurricularTransitionPlan, originsArraySet, destinationArraySet);
        degreeCurricularTransitionPlan.createTransitionPlanRule(originsArraySet, destinationArraySet);
    }

    private void printInfo(DegreeCurricularTransitionPlan dtp, Set<DegreeModule[]> originsArray, Set<DegreeModule[]> destinationArray) {
        taskLog("%s -> %s%n", dtp.getOriginDegreeCurricularPlan().getName(), dtp.getDestinationDegreeCurricularPlan().getName());
        taskLog("Origens");
        originsArray.forEach(dm -> {
            CurricularCourse originCC = (CurricularCourse) dm[0];
            taskLog("\t%s %s%n", originCC.getName(), originCC.getEctsCredits());
        });
        taskLog("Destino");
        final CurricularCourse destinationCC = (CurricularCourse) destinationArray.iterator().next()[0];
        taskLog("\t%s %s%n", destinationCC.getName(), destinationCC.getEctsCredits());
        taskLog("------------------------------------");
    }

    private void buildList(DegreeModulePath dmp, ArrayList arrayList) {
        arrayList.add(dmp.getDegreeModule());
        if (dmp.getParentPath() != null) {
            buildList(dmp.getParentPath(), arrayList);
        }
    }

    private Set<DegreeModulePath> findEqualPaths(DegreeModulePath dm, Set<DegreeModulePath> destinationModules) {
        final Set<DegreeModulePath> degreeModulePaths = destinationModules.stream()
                .filter(dmp -> dmp != dm)
                .filter(dmp -> dmp.is$$do$$Valid())
                .filter(dmp -> hasSamePath(dm, dmp))
                .collect(Collectors.toSet());
        return degreeModulePaths;
    }

    private boolean hasSamePath(DegreeModulePath dm, DegreeModulePath dmp) {
        if (dm.getDegreeModule() == dmp.getDegreeModule()) {
            if (dm.getParentPath() == null && dmp.getParentPath() == null) {
                return true;
            }
            return hasSamePath(dm.getParentPath(), dmp.getParentPath());
        }
        return false;
    }
}
