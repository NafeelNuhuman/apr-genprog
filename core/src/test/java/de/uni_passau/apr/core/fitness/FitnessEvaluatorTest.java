package de.uni_passau.apr.core.fitness;

import de.uni_passau.apr.core.evaluator.EvaluationResult;
import de.uni_passau.apr.core.testrunner.TestResult;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class FitnessEvaluatorTest {

    @Test
    void ctor_wNegTMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new FitnessEvaluator(1.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new FitnessEvaluator(1.0, -5.0));
    }

    @Test
    void computeFitness_nullEval_returnsNegativeInfinity() {
        FitnessEvaluator fe = new FitnessEvaluator();
        assertEquals(Double.NEGATIVE_INFINITY, fe.computeFitness(null));
    }

    @Test
    void computeFitness_nullTestResult_returnsNegativeInfinity() throws Exception {
        FitnessEvaluator fe = new FitnessEvaluator();
        EvaluationResult eval = newEvalResultWithTestResult(null);
        assertEquals(Double.NEGATIVE_INFINITY, fe.computeFitness(eval));
    }

    @Test
    void computeFitness_basicScore_passedMinusBad_withDefaults() throws Exception {
        // defaults: wPosT=1, wNegT=10
        // run=10, skipped=2, failures=1, errors=1 => bad=2, passed=10-2-2=6
        // score = 6 - 20 = -14
        FitnessEvaluator fe = new FitnessEvaluator();

        TestResult tr = newTestResult(10, 1, 1, 2, false, false, 0);
        double score = fe.computeFitness(newEvalResultWithTestResult(tr));

        assertEquals(-14.0, score, 1e-9);
    }

    @Test
    void computeFitness_negativeCounts_areClampedToZero() throws Exception {
        // failures=-3 => 0, errors=-2=>0, testsRun=-5=>0, skipped=-7=>0 => passed=0 bad=0 => score=0
        FitnessEvaluator fe = new FitnessEvaluator();

        TestResult tr = newTestResult(-5, -3, -2, -7, false, false, 0);
        double score = fe.computeFitness(newEvalResultWithTestResult(tr));

        assertEquals(0.0, score, 1e-9);
    }

    @Test
    void computeFitness_timeout_penaltyApplied() throws Exception {
        // base: run=5 => passed=5 => 5
        // timeout => -1000 => -995
        FitnessEvaluator fe = new FitnessEvaluator();

        TestResult tr = newTestResult(5, 0, 0, 0, true, false, 0);
        double score = fe.computeFitness(newEvalResultWithTestResult(tr));

        assertEquals(-995.0, score, 1e-9);
    }

    @Test
    void computeFitness_nonZeroExitAndNoTests_penaltyApplied() throws Exception {
        // base 0, exit!=0 and testsRun==0 => -2000
        FitnessEvaluator fe = new FitnessEvaluator();

        TestResult tr = newTestResult(0, 0, 0, 0, false, false, 2);
        double score = fe.computeFitness(newEvalResultWithTestResult(tr));

        assertEquals(-2000.0, score, 1e-9);
    }

    @Test
    void computeFitness_exitZeroButNoTests_penaltyApplied() throws Exception {
        // base 0, exit==0 and testsRun==0 => -50
        FitnessEvaluator fe = new FitnessEvaluator();

        TestResult tr = newTestResult(0, 0, 0, 0, false, false, 0);
        double score = fe.computeFitness(newEvalResultWithTestResult(tr));

        assertEquals(-50.0, score, 1e-9);
    }

    @Test
    void computeFitness_perfectRepair_bonusApplied() throws Exception {
        // base: run=3 => 3, bonus +10000 => 10003
        FitnessEvaluator fe = new FitnessEvaluator();

        TestResult tr = newTestResult(3, 0, 0, 0, false, true, 0);
        double score = fe.computeFitness(newEvalResultWithTestResult(tr));

        assertEquals(10003.0, score, 1e-9);
    }

    @Test
    void computeFitness_bonusNotApplied_whenTimedOutEvenIfAllPassed() throws Exception {
        // base 3, timeout -1000 => -997, no bonus because timedOut
        FitnessEvaluator fe = new FitnessEvaluator();

        TestResult tr = newTestResult(3, 0, 0, 0, true, true, 0);
        double score = fe.computeFitness(newEvalResultWithTestResult(tr));

        assertEquals(-997.0, score, 1e-9);
    }

    @Test
    void computeFitness_customWeights_used() throws Exception {
        // wPosT=2, wNegT=5
        // run=10, skipped=1, failures=2, errors=1 => bad=3, passed=6 => 12 - 15 = -3
        FitnessEvaluator fe = new FitnessEvaluator(2.0, 5.0);

        TestResult tr = newTestResult(10, 2, 1, 1, false, false, 0);
        double score = fe.computeFitness(newEvalResultWithTestResult(tr));

        assertEquals(-3.0, score, 1e-9);
    }

    // ---------------------------------------------------------------------
    // Reflection helpers
    // ---------------------------------------------------------------------

    private static TestResult newTestResult(int testsRun, int failures, int errors, int skipped,
                                            boolean timedOut, boolean allPassed, int exitCode) throws Exception {
        TestResult tr = newInstance(TestResult.class);

        setInt(tr, "testsRun", testsRun, "setTestsRun");
        setInt(tr, "failures", failures, "setFailures");
        setInt(tr, "errors", errors, "setErrors");
        setInt(tr, "skipped", skipped, "setSkipped");
        setInt(tr, "exitCode", exitCode, "setExitCode");

        setBoolean(tr, "timedOut", timedOut, "setTimedOut");
        setBoolean(tr, "allPassed", allPassed, "setAllPassed");

        return tr;
    }

    private static EvaluationResult newEvalResultWithTestResult(TestResult tr) throws Exception {
        EvaluationResult er = newInstance(EvaluationResult.class);

        // try setter first, then field
        if (!tryInvokeSetter(er, "setTestResult", TestResult.class, tr)) {
            if (!trySetField(er, "testResult", tr)) {
                // Sometimes naming is different; fall back to scanning for a TestResult field
                Field f = findFirstFieldAssignable(er.getClass(), TestResult.class);
                if (f == null) {
                    throw new AssertionError("Could not set TestResult into EvaluationResult (no setTestResult and no TestResult field)");
                }
                f.setAccessible(true);
                f.set(er, tr);
            }
        }
        return er;
    }

    private static <T> T newInstance(Class<T> cls) throws Exception {
        try {
            Constructor<T> c = cls.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (NoSuchMethodException e) {
            throw new AssertionError(cls.getName() + " needs a no-arg constructor for this test approach. Paste the class and I’ll adapt.", e);
        }
    }

    private static void setInt(Object obj, String fieldName, int value, String setterName) throws Exception {
        if (tryInvokeSetter(obj, setterName, int.class, value)) return;
        if (trySetField(obj, fieldName, value)) return;

        // fallback: scan for an int field with matching-ish name
        Field f = findFieldByTypeAndNameHint(obj.getClass(), int.class, fieldName);
        if (f == null) {
            throw new AssertionError("Could not set int '" + fieldName + "' on " + obj.getClass().getName());
        }
        f.setAccessible(true);
        f.setInt(obj, value);
    }

    private static void setBoolean(Object obj, String fieldName, boolean value, String setterName) throws Exception {
        if (tryInvokeSetter(obj, setterName, boolean.class, value)) return;
        if (trySetField(obj, fieldName, value)) return;

        Field f = findFieldByTypeAndNameHint(obj.getClass(), boolean.class, fieldName);
        if (f == null) {
            throw new AssertionError("Could not set boolean '" + fieldName + "' on " + obj.getClass().getName());
        }
        f.setAccessible(true);
        f.setBoolean(obj, value);
    }

    private static boolean tryInvokeSetter(Object obj, String methodName, Class<?> paramType, Object arg) {
        try {
            Method m = obj.getClass().getMethod(methodName, paramType);
            m.invoke(obj, arg);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean trySetField(Object obj, String fieldName, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Field findFirstFieldAssignable(Class<?> cls, Class<?> type) {
        for (Field f : cls.getDeclaredFields()) {
            if (type.isAssignableFrom(f.getType())) return f;
        }
        return null;
    }

    private static Field findFieldByTypeAndNameHint(Class<?> cls, Class<?> type, String nameHint) {
        // try exact name first
        try {
            Field f = cls.getDeclaredField(nameHint);
            if (f.getType() == type) return f;
        } catch (NoSuchFieldException ignored) {}

        // else scan
        for (Field f : cls.getDeclaredFields()) {
            if (f.getType() == type && f.getName().toLowerCase().contains(nameHint.toLowerCase())) {
                return f;
            }
        }
        // last resort: any field of that primitive type
        for (Field f : cls.getDeclaredFields()) {
            if (f.getType() == type) return f;
        }
        return null;
    }
}
