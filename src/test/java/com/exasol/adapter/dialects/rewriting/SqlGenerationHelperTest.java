package com.exasol.adapter.dialects.rewriting;

import static com.exasol.adapter.dialects.rewriting.SqlGenerationHelper.selectListRequiresCasts;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exasol.adapter.metadata.*;
import com.exasol.adapter.sql.*;

@ExtendWith(MockitoExtension.class)
class SqlGenerationHelperTest {
    @Mock
    Predicate<SqlNode> predicate;
    @Mock
    SqlSelectList selectList;
    @Mock
    SqlStatementSelect select;

    @BeforeEach
    void setup() {
        when(selectList.getParent()).thenReturn(select);
        when(select.getFromClause()).thenReturn(new SqlTable("TEST",
                new TableMetadata("TEST", "", List.of(
                        createColumnMetadata("FIRST"),
                        createColumnMetadata("SECOND")), "")));
    }

    @Test
    void testSelectListRequiresCastsShortCircuitsAfterFirstMatch() {
        final AtomicInteger invocations = new AtomicInteger();
        when(predicate.test(any())).thenAnswer(invocation -> {
            final SqlColumn column = invocation.getArgument(0);
            if (invocations.getAndIncrement() == 0) {
                assertThat(column.getName(), equalTo("FIRST"));
                return true;
            }
            throw new AssertionError("selectListRequiresCasts() must stop after the first matching column");
        });

        final boolean actual = selectListRequiresCasts(selectList, predicate);

        assertAll(
                () -> assertThat(actual, is(true)),
                () -> verify(predicate, times(1)).test(any()));
    }

    @Test
    void testSelectListRequiresCastsChecksAllColumnsWhenNoneMatch() {
        when(predicate.test(any())).thenReturn(false);

        final boolean actual = selectListRequiresCasts(selectList, predicate);

        assertAll(
                () -> assertThat(actual, is(false)),
                () -> verify(predicate, times(2)).test(any()));
    }

    private ColumnMetadata createColumnMetadata(final String columnName) {
        return ColumnMetadata.builder().name(columnName).type(DataType.createBool()).build();
    }
}
