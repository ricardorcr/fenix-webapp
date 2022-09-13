package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Attends;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.IEnrolment;
import org.fenixedu.academic.domain.OptionalEnrolment;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.degreeStructure.OptionalCurricularCourse;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.curriculum.ConclusionProcess;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
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
import org.fenixedu.academic.domain.studentCurriculum.OptionalDismissal;
import org.fenixedu.academic.domain.studentCurriculum.Substitution;
import org.fenixedu.academic.domain.studentCurriculum.TemporarySubstitution;
import org.fenixedu.academic.util.predicates.AndPredicate;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import pt.ist.fenixframework.FenixFramework;

public class SeparateSpecificRegistration extends CustomTask {

    @Override
    public void runTask() throws Exception {
        //TODO possibly check hasValidExternalSecondCycle
        final Registration registration = FenixFramework.getDomainObject("1409634036438872"); //87093
        registration.getStudentCurricularPlanStream()
                .filter(this::canSeparate)
                .forEach(this::createNewSecondCycle);
    }

    protected Registration createNewSecondCycle(final StudentCurricularPlan oldStudentCurricularPlan) {
        final Student student = oldStudentCurricularPlan.getRegistration().getStudent();
        final CycleCurriculumGroup oldSecondCycle = oldStudentCurricularPlan.getSecondCycle();
        final DegreeCurricularPlan degreeCurricularPlan = oldSecondCycle.getDegreeCurricularPlanOfDegreeModule();

        final Registration newRegistration = createRegistration(student, oldStudentCurricularPlan);
        final StudentCurricularPlan newStudentCurricularPlan =
                createStudentCurricularPlan(newRegistration, degreeCurricularPlan, oldSecondCycle.getCycleType());
        final CycleCurriculumGroup newSecondCycle = newStudentCurricularPlan.getSecondCycle();

        copyCycleCurriculumGroupsInformation(oldSecondCycle, newSecondCycle);
        moveAttends(oldStudentCurricularPlan, newStudentCurricularPlan);
        tryRemoveOldSecondCycle(oldSecondCycle);

        if (oldStudentCurricularPlan.getDegreeCurricularPlan().getDegreeType().isIntegratedMasterDegree()) {
            markOldRegistrationWithState(oldStudentCurricularPlan, RegistrationStateType.EXTERNAL_ABANDON);
        } else {
            markOldRegistrationWithState(oldStudentCurricularPlan, RegistrationStateType.CONCLUDED);
        }

        newRegistration.updateEnrolmentDate(ExecutionYear.readCurrentExecutionYear());
        return newRegistration;
    }

    private void moveAttends(final StudentCurricularPlan oldStudentCurricularPlan,
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

    private Registration createRegistration(final Student student, final StudentCurricularPlan sourceStudentCurricularPlan) {

        final CycleCurriculumGroup oldSecondCycle = sourceStudentCurricularPlan.getSecondCycle();
        Registration registration = student.getActiveRegistrationFor(oldSecondCycle.getDegreeCurricularPlanOfDegreeModule());

        if (registration != null) {
            return registration;
        }

        Degree degree = oldSecondCycle.getDegreeCurricularPlanOfDegreeModule().getDegree();
        registration = new Registration(student.getPerson(), student.getNumber(), degree);
        //RAIDES info has changed
//        registration.setStudentCandidacy(studentCandidacy);
//        PersonalIngressionData personalIngressionData =
//                student.getPersonalIngressionDataByExecutionYear(registration.getRegistrationYear());
//        if (personalIngressionData == null) {
//            new PersonalIngressionData(student, registration.getRegistrationYear(),
//                    studentCandidacy.getPrecedentDegreeInformation());
//        } else {
//            personalIngressionData.addPrecedentDegreesInformations(studentCandidacy.getPrecedentDegreeInformation());
//        }
//        registration.addPrecedentDegreesInformations(studentCandidacy.getPrecedentDegreeInformation());

        registration.setStartDate(getBeginDate(sourceStudentCurricularPlan, getCurrentExecutionPeriod()));
        RegistrationState activeState = registration.getActiveState();
        activeState.setStateDate(getBeginDate(sourceStudentCurricularPlan, getCurrentExecutionPeriod()));
        activeState.setResponsiblePerson(null);
        registration.setSourceRegistration(sourceStudentCurricularPlan.getRegistration());
        registration.setRegistrationProtocol(sourceStudentCurricularPlan.getRegistration().getRegistrationProtocol());
        registration.setRegistrationYear(ExecutionYear.readCurrentExecutionYear());

        return registration;
    }

    private YearMonthDay getBeginDate(final StudentCurricularPlan sourceStudentCurricularPlan,
                                      final ExecutionSemester executionSemester) {

        if (!sourceStudentCurricularPlan.getFirstCycle().isConcluded()) {
            throw new DomainException("error.SeparationCyclesManagement.source.studentCurricularPlan.is.not.concluded");
        }

        final YearMonthDay conclusionDate = sourceStudentCurricularPlan.getFirstCycle().calculateConclusionDate();
        final YearMonthDay stateDate = conclusionDate != null ? conclusionDate.plusDays(1) : new YearMonthDay().plusDays(1);

        return executionSemester.getBeginDateYearMonthDay().isBefore(stateDate) ? stateDate : executionSemester
                .getBeginDateYearMonthDay();
    }

    private StudentCurricularPlan createStudentCurricularPlan(final Registration registration,
                                                              final DegreeCurricularPlan degreeCurricularPlan, CycleType cycleType) {

        StudentCurricularPlan result = registration.getStudentCurricularPlan(degreeCurricularPlan);
        if (result != null) {
            return result;
        }

        result =
                StudentCurricularPlan.createWithEmptyStructure(registration, degreeCurricularPlan, cycleType,
                        registration.getStartDate());

        // set ingression after create studentcurricularPlan
        registration.setIngressionType(IngressionType.findByPredicate(IngressionType::isDirectAccessFrom1stCycle).orElse(null));

        return result;
    }

    private void copyCycleCurriculumGroupsInformation(final CycleCurriculumGroup oldSecondCycle,
                                                      final CycleCurriculumGroup newSecondCycle) {
        for (final CurriculumModule curriculumModule : oldSecondCycle.getCurriculumModulesSet()) {
            if (curriculumModule.isLeaf()) {
                copyCurriculumLineInformation((CurriculumLine) curriculumModule, newSecondCycle);
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
        if (source.getName().getContent().equals("Minor")) {
            source.setCurriculumGroup(parent);
            return;
        }
        if (parent.hasChildDegreeModule(source.getDegreeModule())) {
            destination = (CurriculumGroup) parent.getChildCurriculumModule(source.getDegreeModule());
        } else {
            destination = CurriculumGroupFactory.createGroup(parent, source.getDegreeModule());
        }

        for (final CurriculumModule curriculumModule : source.getCurriculumModulesSet()) {
            if (curriculumModule.isLeaf()) {
                copyCurriculumLineInformation((CurriculumLine) curriculumModule, destination);
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

    private void copyCurriculumLineInformation(final CurriculumLine curriculumLine, final CurriculumGroup parent) {
        if (curriculumLine.isEnrolment()) {
            final Enrolment enrolment = (Enrolment) curriculumLine;
            if (enrolment.isApproved()) {
                createSubstitutionForEnrolment((Enrolment) curriculumLine, parent);
            } else if (enrolment.getExecutionPeriod().isCurrent() && enrolment.isActive()) {
                moveEnrolment((Enrolment) curriculumLine, parent);
            }
        } else if (curriculumLine.isDismissal()) {
            createDismissal((Dismissal) curriculumLine, parent);
        } else {
            throw new DomainException("error.unknown.curriculumLine");
        }
    }

    private void tryRemoveOldSecondCycle(final CycleCurriculumGroup oldSecondCycle) {
        if (canRemoveOldSecondCycle(oldSecondCycle)) {
            deleteCurriculumModules(oldSecondCycle);
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
            if (curriculumLine.isEnrolment() || curriculumLine.isDismissal()) {
                return false;
            } else if (!curriculumLine.isDismissal()) {
                throw new DomainException("error.unknown.curriculum.line");
            }
        }
        return true;
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
        substitution.setExecutionPeriod(getCurrentExecutionPeriod());
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
        newCredits.setExecutionPeriod(getCurrentExecutionPeriod());
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

        dismissal.delete();
    }

    private boolean curriculumGroupHasSimilarDismissal(final CurriculumGroup curriculumGroup, final Dismissal dismissal) {
        return curriculumGroup.getChildDismissals().stream().anyMatch(each -> each.isSimilar(dismissal));
    }

    private void markOldRegistrationWithState(final StudentCurricularPlan oldStudentCurricularPlan, RegistrationStateType
            stateType) {
        if (oldStudentCurricularPlan.getRegistration().hasState(stateType)) {
            return;
        }

        LocalDate stateDate = new LocalDate();
        if (stateDate.isAfter(getCurrentExecutionYear().getEndDateYearMonthDay())) {
            stateDate = getCurrentExecutionYear().getEndDateYearMonthDay().toLocalDate();
        }

        final RegistrationState state =
                RegistrationState.createRegistrationState(oldStudentCurricularPlan.getRegistration(), null,
                        stateDate.toDateTimeAtStartOfDay(), stateType);
        state.setResponsiblePerson(null);
    }

    protected ExecutionSemester getCurrentExecutionPeriod() {
        return ExecutionSemester.readActualExecutionSemester();
    }

    private ExecutionYear getCurrentExecutionYear() {
        return getCurrentExecutionPeriod().getExecutionYear();
    }

    private boolean studentAlreadyHasNewRegistration(final StudentCurricularPlan studentCurricularPlan) {
        final Student student = studentCurricularPlan.getRegistration().getStudent();
        return student.hasRegistrationFor(studentCurricularPlan.getSecondCycle().getDegreeCurricularPlanOfDegreeModule().getDegree());
    }

    private boolean canSeparate(final StudentCurricularPlan scp) {
        taskLog(scp.getName());
        CycleCurriculumGroup firstCycle = scp.getFirstCycle();
        ConclusionProcess conclusionProcess = firstCycle != null ? firstCycle.getConclusionProcess() : null;

        taskLog("#%s%n", hasFirstCycleConcluded(firstCycle));
        taskLog("#%s%n", hasValidExternalSecondCycle(scp));
        taskLog("#%s%n", !studentAlreadyHasNewRegistration(scp));
        taskLog("#%s%n", scp.isActive() || (conclusionProcess != null && conclusionProcess.isActive()));
        return hasFirstCycleConcluded(firstCycle) && hasValidExternalSecondCycle(scp)
                && !studentAlreadyHasNewRegistration(scp)
                && (scp.isActive() || (conclusionProcess != null && conclusionProcess.isActive()));
    }

    private boolean hasFirstCycleConcluded(final CycleCurriculumGroup firstCycle) {
        return firstCycle != null && firstCycle.isConcluded();
    }

    private boolean hasValidExternalSecondCycle(final StudentCurricularPlan studentCurricularPlan) {
        final CycleCurriculumGroup secondCycle = studentCurricularPlan.getSecondCycle();
        return secondCycle != null && secondCycle.isExternal()
                && (secondCycle.hasEnrolment(ExecutionSemester.readActualExecutionSemester()) //this case does not have but it's to separate
                    || hasDismissal(ExecutionSemester.readActualExecutionSemester(), secondCycle))
                && hasActiveExecutionDegree(secondCycle);
    }

    private boolean hasActiveExecutionDegree(final CycleCurriculumGroup secondCycle) {
        return !secondCycle.getDegreeModule().getDegree()
                .getExecutionDegreesForExecutionYear(ExecutionYear.readCurrentExecutionYear()).isEmpty();
    }

    public boolean hasDismissal(final ExecutionSemester executionSemester, final CurriculumGroup group) {
        final AndPredicate<CurriculumModule> andPredicate = new AndPredicate<CurriculumModule>();
        andPredicate.add(new CurriculumModule.CurriculumModulePredicateByType(Dismissal.class));
        andPredicate.add(new CurriculumModule.CurriculumModulePredicateByExecutionSemester(executionSemester));

        return group.hasAnyCurriculumModules(andPredicate);
    }
}
