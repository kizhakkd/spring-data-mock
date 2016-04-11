package com.mmnaseri.utils.spring.data.domain.impl;

import com.mmnaseri.utils.spring.data.domain.Parameter;
import com.mmnaseri.utils.spring.data.domain.model.Address;
import com.mmnaseri.utils.spring.data.domain.model.Person;
import com.mmnaseri.utils.spring.data.query.NullHandling;
import com.mmnaseri.utils.spring.data.query.PageParameterExtractor;
import com.mmnaseri.utils.spring.data.query.SortDirection;
import com.mmnaseri.utils.spring.data.query.impl.*;
import com.mmnaseri.utils.spring.data.store.DataStore;
import com.mmnaseri.utils.spring.data.store.impl.MemoryDataStore;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author Mohammad Milad Naseri (m.m.naseri@gmail.com)
 * @since 1.0 (4/10/16)
 */
public class SelectDataStoreOperationTest {

    private DataStore<String, Person> dataStore;

    @BeforeMethod
    public void setUp() throws Exception {
        dataStore = new MemoryDataStore<>(Person.class);
        dataStore.save("k1", new Person().setId("k1").setFirstName("Milad").setLastName("Naseri").setAddress(new Address().setCity("Tehran")).setAge(10));
        dataStore.save("k2", new Person().setId("k2").setFirstName("Zohreh").setLastName("Sadeghi").setAddress(new Address().setCity("Seattle")).setAge(12));
        dataStore.save("k3", new Person().setId("k3").setFirstName("Milad").setLastName("Naseri").setAddress(new Address().setCity("Seattle")).setAge(40));
        dataStore.save("k4", new Person().setId("k4").setFirstName("Zohreh").setLastName("Sadeghi").setAddress(new Address().setCity("Amol")).setAge(25));
    }

    @Test
    public void testSimpleSelection() throws Exception {
        final List<List<Parameter>> branches = new ArrayList<>();
        final DefaultOperatorContext operatorContext = new DefaultOperatorContext();
        branches.add(Arrays.asList(
                new ImmutableParameter("firstName", Collections.emptySet(), new int[]{0}, operatorContext.getBySuffix("Is")),
                new ImmutableParameter("lastName", Collections.emptySet(), new int[]{1}, operatorContext.getBySuffix("Is"))
        ));
        branches.add(Collections.singletonList(
                new ImmutableParameter("address.city", Collections.emptySet(), new int[]{2}, operatorContext.getBySuffix("Is"))
        ));
        branches.add(Collections.singletonList(
                new ImmutableParameter("age", Collections.emptySet(), new int[]{3}, operatorContext.getBySuffix("GreaterThan"))
        ));
        final DefaultQueryDescriptor descriptor = new DefaultQueryDescriptor(false, null, 0, null, null, branches, null, null);
        final SelectDataStoreOperation<String, Person> operation = new SelectDataStoreOperation<>(descriptor);
        final List<Person> selected = operation.execute(dataStore, null, new ImmutableInvocation(Sample.class.getMethod("findByFirstNameAndLastNameOrAddressCityOrAgeGreaterThan", String.class, String.class, String.class, Integer.class), new Object[]{"Milad", "Naseri", "Tabriz", 100}));
        assertThat(selected, is(notNullValue()));
        assertThat(selected, hasSize(2));
        assertThat(selected.get(0).getId(), isIn(Arrays.asList("k1", "k3")));
        assertThat(selected.get(1).getId(), isIn(Arrays.asList("k1", "k3")));
    }

    @Test
    public void testSorting() throws Exception {
        final ImmutableOrder second = new ImmutableOrder(SortDirection.ASCENDING, "lastName", NullHandling.DEFAULT);
        final ImmutableOrder first = new ImmutableOrder(SortDirection.ASCENDING, "address.city", NullHandling.DEFAULT);
        final ImmutableSort sort = new ImmutableSort(Arrays.asList(first, second));
        final WrappedSortParameterExtractor sortExtractor = new WrappedSortParameterExtractor(sort);
        final DefaultQueryDescriptor descriptor = new DefaultQueryDescriptor(false, null, 0, null, sortExtractor, Collections.emptyList(), null, null);
        final SelectDataStoreOperation<String, Person> operation = new SelectDataStoreOperation<>(descriptor);
        final List<Person> selected = operation.execute(dataStore, null, new ImmutableInvocation(Sample.class.getMethod("findAll"), new Object[]{}));
        assertThat(selected, hasSize(4));
        assertThat(selected, containsInAnyOrder(dataStore.retrieveAll().toArray()));
        assertThat(selected.get(0).getId(), is("k3"));
        assertThat(selected.get(1).getId(), is("k1"));
        assertThat(selected.get(2).getId(), is("k4"));
        assertThat(selected.get(3).getId(), is("k2"));
    }

    @Test
    public void testPagingWhenPageStartIsBeyondSelection() throws Exception {
        final List<List<Parameter>> branches = new ArrayList<>();
        final PageParameterExtractor pageExtractor = new PageablePageParameterExtractor(0);
        final DefaultQueryDescriptor descriptor = new DefaultQueryDescriptor(false, null, 0, pageExtractor, null, branches, null, null);
        final SelectDataStoreOperation<String, Person> operation = new SelectDataStoreOperation<>(descriptor);
        final List<Person> selected = operation.execute(dataStore, null, new ImmutableInvocation(Sample.class.getMethod("findAll", Pageable.class), new Object[]{new PageRequest(2, 10)}));
        assertThat(selected, is(empty()));
    }

    @Test
    public void testPagingWhenLastPageIsNotFull() throws Exception {
        final PageParameterExtractor pageExtractor = new PageablePageParameterExtractor(0);
        final DefaultQueryDescriptor descriptor = new DefaultQueryDescriptor(false, null, 0, pageExtractor, null, Collections.emptyList(), null, null);
        final SelectDataStoreOperation<String, Person> operation = new SelectDataStoreOperation<>(descriptor);
        final List<Person> selected = operation.execute(dataStore, null, new ImmutableInvocation(Sample.class.getMethod("findAll", Pageable.class), new Object[]{new PageRequest(1, 3)}));
        assertThat(selected, hasSize(1));
    }

    @Test
    public void testLimitingTheResult() throws Exception {
        for (int limit = 1; limit < 10; limit++) {
            final DefaultQueryDescriptor descriptor = new DefaultQueryDescriptor(false, null, limit, null, null, Collections.emptyList(), null, null);
            final SelectDataStoreOperation<String, Person> operation = new SelectDataStoreOperation<>(descriptor);
            final List<Person> selected = operation.execute(dataStore, null, new ImmutableInvocation(Sample.class.getMethod("findAll"), new Object[]{}));
            assertThat(selected, hasSize(Math.min(limit, dataStore.retrieveAll().size())));
        }
    }

    @Test
    public void testLoadingDistinctValues() throws Exception {
        dataStore.save("k5", new Person().setId("k1"));
        //not distinct
        final DefaultQueryDescriptor descriptor = new DefaultQueryDescriptor(false, null, 0, null, null, Collections.emptyList(), null, null);
        final SelectDataStoreOperation<String, Person> operation = new SelectDataStoreOperation<>(descriptor);
        final List<Person> selected = operation.execute(dataStore, null, new ImmutableInvocation(Sample.class.getMethod("findAll"), new Object[]{}));
        assertThat(selected, hasSize(5));
        //distinct
        final DefaultQueryDescriptor descriptorDistinct = new DefaultQueryDescriptor(true, null, 0, null, null, Collections.emptyList(), null, null);
        final SelectDataStoreOperation<String, Person> operationDistinct = new SelectDataStoreOperation<>(descriptorDistinct);
        final List<Person> selectedDistinct = operationDistinct.execute(dataStore, null, new ImmutableInvocation(Sample.class.getMethod("findAll"), new Object[]{}));
        assertThat(selectedDistinct, hasSize(4));
    }

    @Test
    public void testToString() throws Exception {
        final DefaultQueryDescriptor descriptor = new DefaultQueryDescriptor(false, null, 0, null, null, Collections.emptyList(), null, null);
        final SelectDataStoreOperation<String, Person> operation = new SelectDataStoreOperation<>(descriptor);
        assertThat(operation.toString(), is(descriptor.toString()));
    }

    private interface Sample {

        void findByFirstNameAndLastNameOrAddressCityOrAgeGreaterThan(String firstName, String lastName, String city, Integer age);

        void findAll();

        void findAll(Pageable pageable);

    }

}