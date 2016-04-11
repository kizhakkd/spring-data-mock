package com.mmnaseri.utils.spring.data.domain.impl.matchers;

import com.mmnaseri.utils.spring.data.domain.Parameter;
import com.mmnaseri.utils.spring.data.domain.impl.ImmutableParameter;
import com.mmnaseri.utils.spring.data.error.InvalidArgumentException;
import org.testng.annotations.Test;

/**
 * @author Mohammad Milad Naseri (m.m.naseri@gmail.com)
 * @since 1.0 (4/10/16)
 */
public class AbstractBinaryComparableMatcherTest {

    @Test(expectedExceptions = InvalidArgumentException.class, expectedExceptionsMessageRegExp = ".*x.y.z.*")
    public void testWhenValueIsNotComparable() throws Exception {
        final MockBinaryComparableMatcher matcher = new MockBinaryComparableMatcher();
        matcher.matches(new ImmutableParameter("x.y.z", null, null, null), new Object(), 1, 2);
    }

    @Test(expectedExceptions = InvalidArgumentException.class, expectedExceptionsMessageRegExp = ".*x.y.z.*")
    public void testWhenFirstParameterIsNotComparable() throws Exception {
        final MockBinaryComparableMatcher matcher = new MockBinaryComparableMatcher();
        matcher.matches(new ImmutableParameter("x.y.z", null, null, null), 1, new Object(), 2);
    }

    @Test(expectedExceptions = InvalidArgumentException.class, expectedExceptionsMessageRegExp = ".*x.y.z.*")
    public void testWhenSecondParameterIsNotComparable() throws Exception {
        final MockBinaryComparableMatcher matcher = new MockBinaryComparableMatcher();
        matcher.matches(new ImmutableParameter("x.y.z", null, null, null), 1, 2, new Object());
    }

    @Test
    public void testWhenAllAreComparable() throws Exception {
        final MockBinaryComparableMatcher matcher = new MockBinaryComparableMatcher();
        matcher.matches(new ImmutableParameter("x.y.z", null, null, null), 1, (Object) 2, 3);
    }

    private static class MockBinaryComparableMatcher extends AbstractBinaryComparableMatcher {

        @Override
        protected boolean matches(Parameter parameter, Comparable value, Comparable first, Comparable second) {
            return false;
        }
    }

}