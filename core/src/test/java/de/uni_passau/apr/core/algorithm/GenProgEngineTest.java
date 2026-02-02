package de.uni_passau.apr.core.algorithm;

import de.uni_passau.apr.core.crossover.SingleEditCrossover;
import de.uni_passau.apr.core.evaluator.EvaluationResult;
import de.uni_passau.apr.core.evaluator.Evaluator;
import de.uni_passau.apr.core.fitness.FitnessEvaluator;
import de.uni_passau.apr.core.mutation.SingleEditMutator;
import de.uni_passau.apr.core.selection.PopulationInitializer;
import de.uni_passau.apr.core.testrunner.TestResult;
import org.junit.jupiter.api.Test;

import java.lang.reflect.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GenProgEngineTest {

    @Test
    void ctor_nullDependencies_throwNpe() throws Exception {
        PopulationInitializer popInit = dummyNonNull(PopulationInitializer.class);
        FitnessEvaluator fit = dummyNonNull(FitnessEvaluator.class);
        Evaluator eval = dummyNonNull(Evaluator.class);
        SingleEditCrossover cross = dummyNonNull(SingleEditCrossover.class);
        SingleEditMutator mut = dummyNonNull(SingleEditMutator.class);

        assertThrows(NullPointerException.class, () -> new GenProgEngine(null, fit, eval, cross, mut));
        assertThrows(NullPointerException.class, () -> new GenProgEngine(popInit, null, eval, cross, mut));
        assertThrows(NullPointerException.class, () -> new GenProgEngine(popInit, fit, null, cross, mut));
        assertThrows(NullPointerException.class, () -> new GenProgEngine(popInit, fit, eval, null, mut));
        assertThrows(NullPointerException.class, () -> new GenProgEngine(popInit, fit, eval, cross, null));
    }

    @Test
    void run_nullArgs_throwNpeWithBenchmarkMessage() throws Exception {
        GenProgEngine engine = new GenProgEngine(
                dummyNonNull(PopulationInitializer.class),
                dummyNonNull(FitnessEvaluator.class),
                dummyNonNull(Evaluator.class),
                dummyNonNull(SingleEditCrossover.class),
                dummyNonNull(SingleEditMutator.class)
        );

        NullPointerException ex = assertThrows(NullPointerException.class, () -> engine.run(null, null));
        assertEquals("benchmark", ex.getMessage());
    }

    @Test
    void isSuccessful_nullEval_false() throws Exception {
        GenProgEngine engine = newEngine();
        assertFalse(engine.isSuccessful(null));
    }

    @Test
    void isSuccessful_evalWithNullTestResult_false() throws Exception {
        GenProgEngine engine = newEngine();

        EvaluationResult eval = newEvaluationResultWithTestResult(null);
        assertFalse(engine.isSuccessful(eval));
    }

    @Test
    void isSuccessful_allPassedAndNotTimedOut_true() throws Exception {
        GenProgEngine engine = newEngine();

        TestResult tr = newTestResult(true, false);
        EvaluationResult eval = newEvaluationResultWithTestResult(tr);

        assertTrue(engine.isSuccessful(eval));
    }

    @Test
    void isSuccessful_notAllPassed_false() throws Exception {
        GenProgEngine engine = newEngine();

        TestResult tr = newTestResult(false, false);
        EvaluationResult eval = newEvaluationResultWithTestResult(tr);

        assertFalse(engine.isSuccessful(eval));
    }

    @Test
    void isSuccessful_timedOut_falseEvenIfAllPassed() throws Exception {
        GenProgEngine engine = newEngine();

        TestResult tr = newTestResult(true, true);
        EvaluationResult eval = newEvaluationResultWithTestResult(tr);

        assertFalse(engine.isSuccessful(eval));
    }

    // ---------------------------------------------------------------------
    // Engine creation (needs non-null deps only)
    // ---------------------------------------------------------------------

    private static GenProgEngine newEngine() throws Exception {
        return new GenProgEngine(
                dummyNonNull(PopulationInitializer.class),
                dummyNonNull(FitnessEvaluator.class),
                dummyNonNull(Evaluator.class),
                dummyNonNull(SingleEditCrossover.class),
                dummyNonNull(SingleEditMutator.class)
        );
    }

    /**
     * Create a non-null instance of any type without Mockito:
     * - if it's an interface: dynamic proxy returning defaults
     * - else: try any accessible constructor with default args
     * - else: Unsafe.allocateInstance (bypasses constructors)
     */
    @SuppressWarnings("unchecked")
    private static <T> T dummyNonNull(Class<T> type) throws Exception {
        if (type.isInterface()) {
            InvocationHandler h = (proxy, method, args) -> defaultValue(method.getReturnType());
            return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, h);
        }

        // Try constructors (even if no no-arg)
        for (Constructor<?> c : type.getDeclaredConstructors()) {
            try {
                c.setAccessible(true);
                Object[] args = Arrays.stream(c.getParameterTypes())
                        .map(GenProgEngineTest::defaultValue)
                        .toArray();
                return (T) c.newInstance(args);
            } catch (Throwable ignored) {
                // keep trying
            }
        }

        // Last resort: bypass constructors
        return (T) unsafeAllocate(type);
    }

    // ---------------------------------------------------------------------
    // Build TestResult + EvaluationResult without Mockito
    // ---------------------------------------------------------------------

    private static TestResult newTestResult(boolean allPassed, boolean timedOut) throws Exception {
        Class<?> cls = TestResult.class;

        if (cls.isInterface()) {
            return (TestResult) Proxy.newProxyInstance(
                    cls.getClassLoader(),
                    new Class<?>[]{cls},
                    (proxy, method, args) -> {
                        if (method.getName().equals("isAllPassed")) return allPassed;
                        if (method.getName().equals("isTimedOut")) return timedOut;
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        Object inst = bestEffortConstruct(cls);

        // Try setters (common patterns)
        callIfExists(inst, "setAllPassed", boolean.class, allPassed);
        callIfExists(inst, "setTimedOut", boolean.class, timedOut);

        // Try fields (common patterns)
        setBooleanFieldIfPresent(inst, "allPassed", allPassed);
        setBooleanFieldIfPresent(inst, "timedOut", timedOut);

        // If the class has the methods, verify we achieved the desired behavior.
        if (hasNoArgMethod(cls, "isAllPassed") && hasNoArgMethod(cls, "isTimedOut")) {
            boolean ap = (boolean) cls.getMethod("isAllPassed").invoke(inst);
            boolean to = (boolean) cls.getMethod("isTimedOut").invoke(inst);
            if (ap != allPassed || to != timedOut) {
                throw new AssertionError("Couldn't configure TestResult to desired flags. " +
                        "Field/setter names differ. Paste TestResult and I’ll tailor this.");
            }
        }

        return (TestResult) inst;
    }

    private static EvaluationResult newEvaluationResultWithTestResult(TestResult tr) throws Exception {
        Class<?> cls = EvaluationResult.class;

        if (cls.isInterface()) {
            return (EvaluationResult) Proxy.newProxyInstance(
                    cls.getClassLoader(),
                    new Class<?>[]{cls},
                    (proxy, method, args) -> {
                        if (method.getName().equals("getTestResult")) return tr;
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        Object inst = bestEffortConstruct(cls);

        // Try setter
        callIfExists(inst, "setTestResult", TestResult.class, tr);

        // Try field
        setObjectFieldIfPresent(inst, "testResult", tr);

        // Verify if getter exists
        if (hasNoArgMethod(cls, "getTestResult")) {
            Object got = cls.getMethod("getTestResult").invoke(inst);
            if (got != tr) {
                throw new AssertionError("Couldn't configure EvaluationResult.getTestResult(). " +
                        "Field/setter names differ. Paste EvaluationResult and I’ll tailor this.");
            }
        }

        return (EvaluationResult) inst;
    }

    // ---------------------------------------------------------------------
    // Reflection utilities
    // ---------------------------------------------------------------------

    private static Object bestEffortConstruct(Class<?> cls) throws Exception {
        // Try constructors with default args
        for (Constructor<?> c : cls.getDeclaredConstructors()) {
            try {
                c.setAccessible(true);
                Object[] args = Arrays.stream(c.getParameterTypes())
                        .map(GenProgEngineTest::defaultValue)
                        .toArray();
                return c.newInstance(args);
            } catch (Throwable ignored) {
            }
        }
        // Bypass constructors
        return unsafeAllocate(cls);
    }

    private static boolean hasNoArgMethod(Class<?> cls, String name) {
        try {
            cls.getMethod(name);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static void callIfExists(Object target, String name, Class<?> paramType, Object arg) {
        try {
            Method m = target.getClass().getMethod(name, paramType);
            m.invoke(target, arg);
            return;
        } catch (Exception ignored) { }
        try {
            Method m = target.getClass().getDeclaredMethod(name, paramType);
            m.setAccessible(true);
            m.invoke(target, arg);
        } catch (Exception ignored) { }
    }

    private static void setBooleanFieldIfPresent(Object target, String fieldName, boolean value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            if (f.getType() == boolean.class) {
                f.setBoolean(target, value);
            }
        } catch (Exception ignored) { }
    }

    private static void setObjectFieldIfPresent(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception ignored) { }
    }

    private static Object defaultValue(Class<?> ret) {
        if (ret == void.class) return null;
        if (!ret.isPrimitive()) {
            // Reasonable defaults for common reference types:
            if (ret == String.class) return "";
            if (ret == List.class) return List.of();
            if (ret == Map.class) return Map.of();
            if (ret == Set.class) return Set.of();
            return null;
        }
        if (ret == boolean.class) return false;
        if (ret == byte.class) return (byte) 0;
        if (ret == short.class) return (short) 0;
        if (ret == int.class) return 0;
        if (ret == long.class) return 0L;
        if (ret == float.class) return 0f;
        if (ret == double.class) return 0d;
        if (ret == char.class) return '\0';
        return null;
    }

    /**
     * Allocate an instance without calling constructors.
     */
    private static Object unsafeAllocate(Class<?> cls) throws Exception {
        // sun.misc.Unsafe works on many JDK 17 setups; if your IDE blocks it, paste the error and I’ll adapt.
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Object unsafe = theUnsafe.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        return allocateInstance.invoke(unsafe, cls);
    }
}
