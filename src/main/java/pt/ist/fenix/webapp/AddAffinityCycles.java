package pt.ist.fenix.webapp;

import java.util.Iterator;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degree.degreeCurricularPlan.DegreeCurricularPlanState;
import org.fenixedu.academic.domain.degreeStructure.CycleCourseGroup;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixframework.FenixFramework;

public class AddAffinityCycles extends CustomTask {

    @Override
    public void runTask() throws Exception {
        DegreeCurricularPlan megie2019 = DegreeCurricularPlan.readByNameAndDegreeSigla("MEGIE 2019", "MEGIE");
        CycleCourseGroup megie2019SecondCycleCourseGroup = megie2019.getSecondCycleCourseGroup();

        DegreeCurricularPlan mecd2019 = DegreeCurricularPlan.readByNameAndDegreeSigla("MECD2019", "MECD");
        CycleCourseGroup mecd2019SecondCycleCourseGroup = mecd2019.getSecondCycleCourseGroup();



        //degrees to add MEGIE and MECD to affinities
        DegreeCurricularPlan lean2006 = DegreeCurricularPlan.readByNameAndDegreeSigla("LEAN 2006", "LEAN");
        CycleCourseGroup lean2006FirstCycleCourseGroup = lean2006.getFirstCycleCourseGroup();
        lean2006FirstCycleCourseGroup.addDestinationAffinities(megie2019SecondCycleCourseGroup);
        lean2006FirstCycleCourseGroup.addDestinationAffinities(mecd2019SecondCycleCourseGroup);

        DegreeCurricularPlan lee2006 = DegreeCurricularPlan.readByNameAndDegreeSigla("LEE 2006", "LEE");
        CycleCourseGroup lee2006FirstCycleCourseGroup = lee2006.getFirstCycleCourseGroup();
        lee2006FirstCycleCourseGroup.addDestinationAffinities(megie2019SecondCycleCourseGroup);
        lee2006FirstCycleCourseGroup.addDestinationAffinities(mecd2019SecondCycleCourseGroup);

        DegreeCurricularPlan legi2006 = DegreeCurricularPlan.readByNameAndDegreeSigla("LEGI 2006", "LEGI");
        CycleCourseGroup legi2006FirstCycleCourseGroup = legi2006.getFirstCycleCourseGroup();
        legi2006FirstCycleCourseGroup.addDestinationAffinities(megie2019SecondCycleCourseGroup);
        legi2006FirstCycleCourseGroup.addDestinationAffinities(mecd2019SecondCycleCourseGroup);

        DegreeCurricularPlan legm2006 = DegreeCurricularPlan.readByNameAndDegreeSigla("LEGM 2006", "LEGM");
        CycleCourseGroup legm2006FirstCycleCourseGroup = legm2006.getFirstCycleCourseGroup();
        legm2006FirstCycleCourseGroup.addDestinationAffinities(megie2019SecondCycleCourseGroup);
        legm2006FirstCycleCourseGroup.addDestinationAffinities(mecd2019SecondCycleCourseGroup);

        DegreeCurricularPlan leica2006 = DegreeCurricularPlan.readByNameAndDegreeSigla("LEIC-A 2006", "LEIC-A");
        CycleCourseGroup leica2006FirstCycleCourseGroup = leica2006.getFirstCycleCourseGroup();
        leica2006FirstCycleCourseGroup.addDestinationAffinities(megie2019SecondCycleCourseGroup);
        leica2006FirstCycleCourseGroup.addDestinationAffinities(mecd2019SecondCycleCourseGroup);

        DegreeCurricularPlan leict2006 = DegreeCurricularPlan.readByNameAndDegreeSigla("LEIC-T 2006", "LEIC-T");
        CycleCourseGroup leict2006FirstCycleCourseGroup = leict2006.getFirstCycleCourseGroup();
        leict2006FirstCycleCourseGroup.addDestinationAffinities(megie2019SecondCycleCourseGroup);
        leict2006FirstCycleCourseGroup.addDestinationAffinities(mecd2019SecondCycleCourseGroup);

        DegreeCurricularPlan leti2006 = DegreeCurricularPlan.readByNameAndDegreeSigla("LERC 2006", "LERC");
        CycleCourseGroup leti2006FirstCycleCourseGroup = leti2006.getFirstCycleCourseGroup();
        leti2006FirstCycleCourseGroup.addDestinationAffinities(megie2019SecondCycleCourseGroup);
        leti2006FirstCycleCourseGroup.addDestinationAffinities(mecd2019SecondCycleCourseGroup);

        DegreeCurricularPlan lmac2006 = DegreeCurricularPlan.readByNameAndDegreeSigla("LMAC 2006", "LMAC");
        CycleCourseGroup lmac2006FirstCycleCourseGroup = lmac2006.getFirstCycleCourseGroup();
        lmac2006FirstCycleCourseGroup.addDestinationAffinities(megie2019SecondCycleCourseGroup);
        lmac2006FirstCycleCourseGroup.addDestinationAffinities(mecd2019SecondCycleCourseGroup);




        DegreeCurricularPlan mear2017 = DegreeCurricularPlan.readByNameAndDegreeSigla("MEAer 2017", "MEAer");
        CycleCourseGroup mear2017FirstCycleCourseGroup = mear2017.getFirstCycleCourseGroup();
        mear2017FirstCycleCourseGroup.addDestinationAffinities(megie2019SecondCycleCourseGroup);
        mear2017FirstCycleCourseGroup.addDestinationAffinities(mecd2019SecondCycleCourseGroup);

        DegreeCurricularPlan meambi2008 = DegreeCurricularPlan.readByNameAndDegreeSigla("MEAmbi 2008", "MEAmbi");
        CycleCourseGroup meambi2008FirstCycleCourseGroup = meambi2008.getFirstCycleCourseGroup();
        meambi2008FirstCycleCourseGroup.addDestinationAffinities(megie2019SecondCycleCourseGroup);
        meambi2008FirstCycleCourseGroup.addDestinationAffinities(mecd2019SecondCycleCourseGroup);

        DegreeCurricularPlan mebiol2006 = DegreeCurricularPlan.readByNameAndDegreeSigla("MEBiol 2006", "MEBiol");
        CycleCourseGroup mebiol2006FirstCycleCourseGroup = mebiol2006.getFirstCycleCourseGroup();
        mebiol2006FirstCycleCourseGroup.addDestinationAffinities(megie2019SecondCycleCourseGroup);
        mebiol2006FirstCycleCourseGroup.addDestinationAffinities(mecd2019SecondCycleCourseGroup);

        DegreeCurricularPlan mebiom2006 = DegreeCurricularPlan.readByNameAndDegreeSigla("MEBiom 2006", "MEBiom");
        CycleCourseGroup mebiom2006FirstCycleCourseGroup = mebiom2006.getFirstCycleCourseGroup();
        mebiom2006FirstCycleCourseGroup.addDestinationAffinities(megie2019SecondCycleCourseGroup);
        mebiom2006FirstCycleCourseGroup.addDestinationAffinities(mecd2019SecondCycleCourseGroup);

        DegreeCurricularPlan mec2016 = DegreeCurricularPlan.readByNameAndDegreeSigla("MEC 2016", "MEC");
        CycleCourseGroup mec2016FirstCycleCourseGroup = mec2016.getFirstCycleCourseGroup();
        mec2016FirstCycleCourseGroup.addDestinationAffinities(megie2019SecondCycleCourseGroup);
        mec2016FirstCycleCourseGroup.addDestinationAffinities(mecd2019SecondCycleCourseGroup);

        DegreeCurricularPlan meec2006 = DegreeCurricularPlan.readByNameAndDegreeSigla("MEEC 2006", "MEEC");
        CycleCourseGroup meec2006FirstCycleCourseGroup = meec2006.getFirstCycleCourseGroup();
        meec2006FirstCycleCourseGroup.addDestinationAffinities(megie2019SecondCycleCourseGroup);
        meec2006FirstCycleCourseGroup.addDestinationAffinities(mecd2019SecondCycleCourseGroup);

        DegreeCurricularPlan meft2017 = DegreeCurricularPlan.readByNameAndDegreeSigla("MEFT 2017", "MEFT");
        CycleCourseGroup meft2017FirstCycleCourseGroup = meft2017.getFirstCycleCourseGroup();
        meft2017FirstCycleCourseGroup.addDestinationAffinities(megie2019SecondCycleCourseGroup);
        meft2017FirstCycleCourseGroup.addDestinationAffinities(mecd2019SecondCycleCourseGroup);

        DegreeCurricularPlan mem2017 = DegreeCurricularPlan.readByNameAndDegreeSigla("MEM 2017", "MEM");
        CycleCourseGroup mem2017FirstCycleCourseGroup = mem2017.getFirstCycleCourseGroup();
        mem2017FirstCycleCourseGroup.addDestinationAffinities(megie2019SecondCycleCourseGroup);
        mem2017FirstCycleCourseGroup.addDestinationAffinities(mecd2019SecondCycleCourseGroup);

        DegreeCurricularPlan memec2006 = DegreeCurricularPlan.readByNameAndDegreeSigla("MEMec 2006", "MEMec");
        CycleCourseGroup memec2006FirstCycleCourseGroup = memec2006.getFirstCycleCourseGroup();
        memec2006FirstCycleCourseGroup.addDestinationAffinities(megie2019SecondCycleCourseGroup);
        memec2006FirstCycleCourseGroup.addDestinationAffinities(mecd2019SecondCycleCourseGroup);

        DegreeCurricularPlan meq2006 = DegreeCurricularPlan.readByNameAndDegreeSigla("MEQ 2006", "MEQ");
        CycleCourseGroup meq2006FirstCycleCourseGroup = meq2006.getFirstCycleCourseGroup();
        meq2006FirstCycleCourseGroup.addDestinationAffinities(megie2019SecondCycleCourseGroup);
        meq2006FirstCycleCourseGroup.addDestinationAffinities(mecd2019SecondCycleCourseGroup);

        printAffinities();
    }

    protected void printAffinities() {
        for (final DegreeCurricularPlan degreeCurricularPlan : DegreeCurricularPlan.readByDegreeTypesAndState(
                DegreeType.oneOf(DegreeType::isBolonhaDegree, DegreeType::isIntegratedMasterDegree),
                DegreeCurricularPlanState.ACTIVE)) {

            print(degreeCurricularPlan.getFirstCycleCourseGroup());
        }
    }

    private void print(final CycleCourseGroup firstCycleCourseGroup) {
        final StringBuilder builder = new StringBuilder();
        builder.append(firstCycleCourseGroup.getParentDegreeCurricularPlan().getName()).append("\t -> ");
        final Iterator<CycleCourseGroup> iter = firstCycleCourseGroup.getDestinationAffinitiesSet().iterator();
        while (iter.hasNext()) {
            builder.append(iter.next().getParentDegreeCurricularPlan().getName());
            if (iter.hasNext()) {
                builder.append(", ");
            }
        }
        taskLog(builder.toString());
    }
}