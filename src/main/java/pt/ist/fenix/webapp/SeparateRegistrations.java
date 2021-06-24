/**
 * Copyright © 2013 Instituto Superior Técnico
 * <p>
 * This file is part of FenixEdu IST Integration.
 * <p>
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenix.webapp;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Locale;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degree.degreeCurricularPlan.DegreeCurricularPlanState;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.curriculum.ConclusionProcess;
import org.fenixedu.academic.domain.student.curriculum.RegistrationConclusionProcess;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.I18N;

import pt.ist.fenixedu.integration.domain.student.SeparationCyclesManagement;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class SeparateRegistrations extends CustomTask {

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    public void runTask() {
        I18N.setLocale(new Locale("pt", "PT"));
        for (final DegreeCurricularPlan degreeCurricularPlan : getDegreeCurricularPlans()) {
            taskLog("Processing DCP: %s%n", degreeCurricularPlan.getName());

            for (final StudentCurricularPlan studentCurricularPlan : degreeCurricularPlan.getStudentCurricularPlansSet()) {

                if (canSeparate(studentCurricularPlan)) {
                    taskLog("Separating Student: %s%n", studentCurricularPlan.getRegistration().getStudent().getNumber());

                    try {
                        separateStudentProcedure(studentCurricularPlan);
                    } catch (Exception e) { //abort transaction and continue
                        taskLog("Separating students with rules %s %s%n",
                                studentCurricularPlan.getRegistration().getStudent().getNumber(), e);
                        ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
                        PrintStream error = new PrintStream(errorOut);
                        e.printStackTrace(error);
                        taskLog(new String(errorOut.toByteArray()));
                    }
                }
            }
        }
    }

    private boolean studentAlreadyHasNewRegistration(final StudentCurricularPlan studentCurricularPlan) {
        final Student student = studentCurricularPlan.getRegistration().getStudent();
        return student.hasRegistrationFor(studentCurricularPlan.getSecondCycle().getDegreeCurricularPlanOfDegreeModule());
    }

    private boolean canSeparate(final StudentCurricularPlan scp) {
        CycleCurriculumGroup firstCycle = scp.getFirstCycle();
        ConclusionProcess conclusionProcess = firstCycle != null ? firstCycle.getConclusionProcess() : null;
        return  hasFirstCycleConcluded(firstCycle) && hasExternalSecondCycle(scp)
                && !studentAlreadyHasNewRegistration(scp)
                && (scp.isActive() || (conclusionProcess != null && conclusionProcess.isActive()));
    }

    private List<DegreeCurricularPlan> getDegreeCurricularPlans() {
        return DegreeCurricularPlan.readByDegreeTypesAndState(
                DegreeType.oneOf(DegreeType::isBolonhaDegree, DegreeType::isIntegratedMasterDegree),
                DegreeCurricularPlanState.ACTIVE);
    }

    private boolean hasFirstCycleConcluded(final CycleCurriculumGroup firstCycle) {
        return firstCycle != null && firstCycle.isConcluded();
    }

    private boolean hasExternalSecondCycle(final StudentCurricularPlan studentCurricularPlan) {
        final CycleCurriculumGroup secondCycle = studentCurricularPlan.getSecondCycle();
        return secondCycle != null && secondCycle.isExternal() && secondCycle.hasAnyCurriculumLines()
                && hasActiveExecutionDegree(secondCycle);
    }

    private boolean hasActiveExecutionDegree(final CycleCurriculumGroup secondCycle) {
        return secondCycle.getDegreeModule().getParentDegreeCurricularPlan()
                .getExecutionDegreeByAcademicInterval(ExecutionYear.readCurrentExecutionYear().getAcademicInterval()) != null;
    }

    @Atomic(mode = TxMode.WRITE)
    private void separateStudentProcedure(StudentCurricularPlan studentCurricularPlan) {
        FenixFramework.atomic(() -> {
            new SeparationCyclesManagement().separateSecondCycle(studentCurricularPlan);
        });
    }
}