package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Attends;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.IEnrolment;
import org.fenixedu.academic.domain.OptionalEnrolment;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEventWithPaymentPlan;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.OptionalCurricularCourse;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.studentCurriculum.Credits;
import org.fenixedu.academic.domain.studentCurriculum.CreditsDismissal;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroupFactory;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.domain.studentCurriculum.EnrolmentWrapper;
import org.fenixedu.academic.domain.studentCurriculum.Equivalence;
import org.fenixedu.academic.domain.studentCurriculum.ExtraCurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.OptionalDismissal;
import org.fenixedu.academic.domain.studentCurriculum.Substitution;
import org.fenixedu.academic.domain.studentCurriculum.TemporarySubstitution;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.YearMonthDay;

public class FixSCPSeparation extends CustomTask {
    
    private ExecutionYear currentYear = null;
    
    @Override
    public void runTask() throws Exception {
        currentYear = ExecutionYear.readCurrentExecutionYear();
        
        final YearMonthDay startDate = new YearMonthDay(2020, 9, 13);
        Bennu.getInstance().getRegistrationsSet().stream()
                .filter(r -> !r.getStartDate().isBefore(startDate))
                .filter(r -> r.getDegreeType().isBolonhaMasterDegree())
                .filter(r -> r.getSourceRegistration() != null)
                .filter(r -> r.getSourceRegistration().isConcluded())
                .filter(r -> r.getEnrolments(ExecutionSemester.readActualExecutionSemester()).isEmpty())
                .filter(r -> {
                    CycleCurriculumGroup secondCycle = r.getSourceRegistration().getLastStudentCurricularPlan().getSecondCycle();
                    if (secondCycle != null && secondCycle.hasEnrolment(ExecutionSemester.readActualExecutionSemester())
                            && secondCycle.getEnrolmentsBy(ExecutionSemester.readActualExecutionSemester()).size()
                                == secondCycle.getEnrolments().size()) {
                        return true;
                    } else {
                        return false;
                    }
                })
                .forEach(this::fix);
//        throw new RuntimeException("Dry run");
    }

    private void fix(final Registration registration) {
        taskLog("Student %s will be fixed%n", registration.getNumber());
        StudentCurricularPlan oldStudentCurricularPlan = registration.getSourceRegistration().getLastStudentCurricularPlan();
        CycleCurriculumGroup oldSecondCycle = oldStudentCurricularPlan.getSecondCycle();
        StudentCurricularPlan newStudentCurricularPlan = registration.getActiveStudentCurricularPlan();
        CycleCurriculumGroup newSecondCycle = newStudentCurricularPlan.getSecondCycle();

        taskLog("Moving %s%n", registration.getNumber());
        copyCycleCurriculumGroupsInformation(oldSecondCycle, newSecondCycle);
        moveExtraCurriculumGroupInformation(oldStudentCurricularPlan, newStudentCurricularPlan);
        moveExtraAttends(oldStudentCurricularPlan, newStudentCurricularPlan);
        tryRemoveOldSecondCycle(oldSecondCycle);

        if (registration.getLastStudentCurricularPlan().hasGratuityEvent(currentYear, GratuityEventWithPaymentPlan.class)) {
            //TODO tratar dívida se foi gerada propina para o 1ºciclo
            taskLog("$$$$Ver estes casos já com dívida!");
        }
    }

    private void copyCycleCurriculumGroupsInformation(final CycleCurriculumGroup oldSecondCycle,
                                                      final CycleCurriculumGroup newSecondCycle) {
        for (final CurriculumModule curriculumModule : oldSecondCycle.getCurriculumModulesSet()) {
            if (curriculumModule.isLeaf()) {
                copyCurricumLineInformation((CurriculumLine) curriculumModule, newSecondCycle);
            } else {
                copyCurriculumGroupsInformation((CurriculumGroup) curriculumModule, newSecondCycle);
            }
        }
    }

    private void copyCurriculumGroupsInformation(final CurriculumGroup source, final CurriculumGroup parent) {
        final CurriculumGroup destination;
        //test if source group still exists as part of destination DCP
        if (!groupIsStillValid(source)) {
            return;
        }
        if (parent.hasChildDegreeModule(source.getDegreeModule())) {
            destination = (CurriculumGroup) parent.getChildCurriculumModule(source.getDegreeModule());
        } else {
            destination = CurriculumGroupFactory.createGroup(parent, source.getDegreeModule());
        }

        for (final CurriculumModule curriculumModule : source.getCurriculumModulesSet()) {
            if (curriculumModule.isLeaf()) {
                copyCurricumLineInformation((CurriculumLine) curriculumModule, destination);
            } else {
                copyCurriculumGroupsInformation((CurriculumGroup) curriculumModule, destination);
            }
        }
    }

    private boolean groupIsStillValid(CurriculumGroup source) {
        ExecutionYear nowadays = ExecutionYear.readCurrentExecutionYear();
        if (source.getDegreeModule().getValidChildContexts(nowadays).size() > 0) {
            return true;
        }
        return source.getChildCurriculumGroups().stream().anyMatch(this::groupIsStillValid);
    }

    private void copyCurricumLineInformation(final CurriculumLine curriculumLine, final CurriculumGroup parent) {
        if (curriculumLine.isEnrolment()) {
            final Enrolment enrolment = (Enrolment) curriculumLine;
            if (enrolment.getExecutionPeriod().isAfterOrEquals(getExecutionPeriod())) {
                moveEnrolment(enrolment, parent);
            } else if (enrolment.isApproved()) {
                taskLog("### Ia criar uma substituição");
                //createSubstitutionForEnrolment((Enrolment) curriculumLine, parent);
            }
        } else if (curriculumLine.isDismissal()) {
            taskLog("### Tem uma dismissal este semestre?!");
//            if (curriculumLine.hasExecutionPeriod() && curriculumLine.getExecutionPeriod().isAfterOrEquals(getExecutionPeriod())) {
//                moveDismissal((Dismissal) curriculumLine, parent);
//            } else {
//                createDismissal((Dismissal) curriculumLine, parent);
//            }
        } else {
            throw new DomainException("error.unknown.curriculumLine");
        }
    }

    private void moveEnrolment(final Enrolment enrolment, final CurriculumGroup parent) {
        final CurriculumModule child = parent.getChildCurriculumModule(enrolment.getDegreeModule());
        if (child != null && child.isEnrolment()) {
            final Enrolment childEnrolment = (Enrolment) child;
            if (childEnrolment.getExecutionPeriod() == enrolment.getExecutionPeriod()) {
                throw new DomainException("error.SeparationCyclesManagement.enrolment.should.not.exist.for.same.executionPeriod");
            }
        }

        final Registration registration = parent.getStudentCurricularPlan().getRegistration();
        enrolment.setCurriculumGroup(parent);

        for (final Attends attend : enrolment.getAttendsSet()) {
            if (!registration.attends(attend.getExecutionCourse())) {
                attend.setRegistration(registration);
            }
        }
    }

    private void moveDismissal(final Dismissal dismissal, final CurriculumGroup parent) {
        if (!dismissal.getUsedInSeparationCycle()) {
            if (!curriculumGroupHasSimilarDismissal(parent, dismissal)) {
                dismissal.setCurriculumGroup(parent);
                dismissal.getCredits().setStudentCurricularPlan(parent.getStudentCurricularPlan());
            } else {
                dismissal.setUsedInSeparationCycle(true);
            }
        }
    }

    private void createSubstitutionForEnrolment(final Enrolment enrolment, final CurriculumGroup parent) {
        if (enrolment.getUsedInSeparationCycle() || parent.hasChildDegreeModule(enrolment.getDegreeModule())) {
            // TODO: temporary
            enrolment.setUsedInSeparationCycle(true);
            return;
        }

        enrolment.setUsedInSeparationCycle(true);

        if (enrolment.isOptional()) {
            final OptionalEnrolment optional = (OptionalEnrolment) enrolment;
            if (parent.hasChildDegreeModule(optional.getOptionalCurricularCourse())) {
                return;
            }
            final Substitution substitution = createSubstitution(enrolment, parent);
            createNewOptionalDismissal(substitution, parent, enrolment, optional.getOptionalCurricularCourse(),
                    optional.getEctsCredits());
        } else {
            createNewDismissal(createSubstitution(enrolment, parent), parent, enrolment);
        }
    }

    private Substitution createSubstitution(final Enrolment enrolment, final CurriculumGroup parent) {
        final Substitution substitution = new Substitution();
        substitution.setStudentCurricularPlan(parent.getStudentCurricularPlan());
        substitution.setExecutionPeriod(getExecutionPeriod());
        EnrolmentWrapper.create(substitution, enrolment);
        return substitution;
    }

    private Dismissal createNewDismissal(final Credits credits, final CurriculumGroup parent, final CurriculumLine curriculumLine) {

        final CurricularCourse curricularCourse = curriculumLine.getCurricularCourse();

        if (!hasCurricularCourseToDismissal(parent, curricularCourse) && !hasResponsibleForCreation(curriculumLine)) {
            throw new DomainException("error.SeparationCyclesManagement.parent.doesnot.have.curricularCourse.to.dismissal");
        }

        final Dismissal dismissal = new Dismissal();
        dismissal.setCredits(credits);
        dismissal.setCurriculumGroup(parent);
        dismissal.setCurricularCourse(curricularCourse);

        return dismissal;
    }

    private OptionalDismissal createNewOptionalDismissal(final Credits credits, final CurriculumGroup parent,
                                                         final CurriculumLine curriculumLine, final OptionalCurricularCourse curricularCourse, final Double ectsCredits) {

        if (ectsCredits == null || ectsCredits == 0) {
            throw new DomainException("error.OptionalDismissal.invalid.credits");
        }

        if (!hasCurricularCourseToDismissal(parent, curricularCourse) && !hasResponsibleForCreation(curriculumLine)) {
            throw new DomainException("error.SeparationCyclesManagement.parent.doesnot.have.curricularCourse.to.dismissal");
        }

        final OptionalDismissal dismissal = new OptionalDismissal();
        dismissal.setCredits(credits);
        dismissal.setCurriculumGroup(parent);
        dismissal.setCurricularCourse(curricularCourse);
        dismissal.setEctsCredits(ectsCredits);

        return dismissal;
    }

    private boolean hasResponsibleForCreation(final CurriculumLine line) {
        return line.hasCreatedBy();
    }

    private boolean hasCurricularCourseToDismissal(final CurriculumGroup curriculumGroup, final CurricularCourse curricularCourse) {
        final CourseGroup degreeModule = curriculumGroup.getDegreeModule();
        return degreeModule.getChildContexts(CurricularCourse.class).stream()
                .map(context -> (CurricularCourse) context.getChildDegreeModule())
                .anyMatch(each -> each.isEquivalent(curricularCourse) && !curriculumGroup.hasChildDegreeModule(degreeModule));
    }

    private void createDismissal(final Dismissal dismissal, final CurriculumGroup parent) {
        if (dismissal.getUsedInSeparationCycle() || curriculumGroupHasSimilarDismissal(parent, dismissal)) {
            // TODO: temporary
            dismissal.setUsedInSeparationCycle(true);
            return;
        }

        dismissal.setUsedInSeparationCycle(true);
        final Credits credits = dismissal.getCredits();

        final Credits newCredits;
        if (credits.isTemporary()) {
            newCredits = new TemporarySubstitution();

        } else if (credits.isSubstitution()) {
            newCredits = new Substitution();

        } else if (credits.isEquivalence()) {
            final Equivalence equivalence = (Equivalence) credits;
            final Equivalence newEquivalence = new Equivalence();
            newEquivalence.setGrade(equivalence.getGrade());
            newCredits = newEquivalence;

        } else {
            newCredits = new Credits();
        }

        newCredits.setStudentCurricularPlan(parent.getStudentCurricularPlan());
        newCredits.setExecutionPeriod(getExecutionPeriod());
        newCredits.setGivenCredits(credits.getGivenCredits());

        for (final IEnrolment enrolment : credits.getIEnrolments()) {
            EnrolmentWrapper.create(newCredits, enrolment);
        }

        if (dismissal.hasCurricularCourse()) {
            if (dismissal instanceof OptionalDismissal) {
                final OptionalDismissal optionalDismissal = (OptionalDismissal) dismissal;
                createNewOptionalDismissal(newCredits, parent, dismissal, optionalDismissal.getCurricularCourse(),
                        optionalDismissal.getEctsCredits());

            } else {
                createNewDismissal(newCredits, parent, dismissal);
            }
        } else if (dismissal.isCreditsDismissal()) {
            final CreditsDismissal creditsDismissal = (CreditsDismissal) dismissal;
            new CreditsDismissal(newCredits, parent, creditsDismissal.getNoEnrolCurricularCoursesSet());
        } else {
            throw new DomainException("error.unknown.dismissal.type");
        }
    }

    private boolean curriculumGroupHasSimilarDismissal(final CurriculumGroup curriculumGroup, final Dismissal dismissal) {
        return curriculumGroup.getChildDismissals().stream().anyMatch(each -> each.isSimilar(dismissal));
    }

    private void moveExtraCurriculumGroupInformation(final StudentCurricularPlan oldStudentCurricularPlan,
                                                     final StudentCurricularPlan newStudentCurricularPlan) {

        final ExtraCurriculumGroup oldExtraCurriculumGroup = oldStudentCurricularPlan.getExtraCurriculumGroup();
        if (oldExtraCurriculumGroup != null) {
            final ExtraCurriculumGroup newExtraCurriculumGroup = newStudentCurricularPlan.getExtraCurriculumGroup();
            if (newExtraCurriculumGroup == null) {
                throw new DomainException("error.invalid.newExtraCurriculumGroup");
            }
            for (final CurriculumModule curriculumModule : oldExtraCurriculumGroup.getCurriculumModulesSet()) {
                if (curriculumModule.isCurriculumLine()) {
                    final CurriculumLine curriculumLine = (CurriculumLine) curriculumModule;
                    if (!curriculumLine.hasExecutionPeriod()
                            || curriculumLine.getExecutionPeriod().isBefore(getExecutionPeriod())) {
                        continue;
                    }
                }
                taskLog("### Ia fazer set extraGroup");
//                curriculumModule.setCurriculumGroup(newExtraCurriculumGroup);
            }

            for (final CurriculumLine curriculumLine : newExtraCurriculumGroup.getAllCurriculumLines()) {
                if (curriculumLine.isDismissal()) {
                    final Dismissal dismissal = (Dismissal) curriculumLine;
                    taskLog("### Ia mudar dismissal extraGroup");
//                    dismissal.getCredits().setStudentCurricularPlan(newStudentCurricularPlan);
                }
            }
        }
    }

    private void tryRemoveOldSecondCycle(final CycleCurriculumGroup oldSecondCycle) {
        if (canRemoveOldSecondCycle(oldSecondCycle)) {
            deleteCurriculumModules(oldSecondCycle);
        } else {
            taskLog("### Não pode remover 2 ciclo....");
        }
    }

    protected void deleteCurriculumModules(final CurriculumModule curriculumModule) {
        if (curriculumModule == null) {
            return;
        }
        if (!curriculumModule.isLeaf()) {
            final CurriculumGroup curriculumGroup = (CurriculumGroup) curriculumModule;
            for (; !curriculumGroup.getCurriculumModulesSet().isEmpty();) {
                deleteCurriculumModules(curriculumGroup.getCurriculumModulesSet().iterator().next());
            }
            curriculumGroup.delete();
        } else if (curriculumModule.isDismissal()) {
            curriculumModule.delete();
        } else {
            throw new DomainException("error.can.only.remove.groups.and.dismissals");
        }
    }

    private boolean canRemoveOldSecondCycle(final CycleCurriculumGroup oldSecondCycle) {
        for (final CurriculumLine curriculumLine : oldSecondCycle.getAllCurriculumLines()) {
            if (curriculumLine.isEnrolment()) {
                return false;
            } else if (!curriculumLine.isDismissal()) {
                throw new DomainException("error.unknown.curriculum.line");
            }
        }
        return true;
    }

    private void moveExtraAttends(final StudentCurricularPlan oldStudentCurricularPlan,
                                  final StudentCurricularPlan newStudentCurricularPlan) {
        oldStudentCurricularPlan.getRegistration().getAssociatedAttendsSet().stream()
                .filter(attend -> !belongsTo(oldStudentCurricularPlan, attend))
                .filter(attend -> isToMoveAttendsFrom(oldStudentCurricularPlan, newStudentCurricularPlan, attend))
                .filter(attend -> !newStudentCurricularPlan.getRegistration().attends(attend.getExecutionCourse()))
                .forEach(attend -> attend.setRegistration(newStudentCurricularPlan.getRegistration()));
    }

    private boolean belongsTo(final StudentCurricularPlan studentCurricularPlan, final Attends attend) {
        return attend.getExecutionCourse().getAssociatedCurricularCoursesSet().stream()
                .anyMatch(curricularCourse -> studentCurricularPlan.getDegreeCurricularPlan().hasDegreeModule(curricularCourse));
    }

    private boolean isToMoveAttendsFrom(final StudentCurricularPlan oldStudentCurricularPlan,
                                        final StudentCurricularPlan newStudentCurricularPlan, final Attends attend) {

        if (attend.getEnrolment() != null) {
            return !oldStudentCurricularPlan.hasEnrolments(attend.getEnrolment())
                    && newStudentCurricularPlan.hasEnrolments(attend.getEnrolment());
        }

        return !attend.getExecutionPeriod().isBefore(newStudentCurricularPlan.getStartExecutionPeriod());
    }

    protected ExecutionSemester getExecutionPeriod() {
        return ExecutionSemester.readActualExecutionSemester();
    }

    private ExecutionYear getExecutionYear() {
        return getExecutionPeriod().getExecutionYear();
    }
}
