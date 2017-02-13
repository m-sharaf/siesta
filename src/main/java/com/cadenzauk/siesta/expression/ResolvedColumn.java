/*
 * Copyright (c) 2017 Cadenza United Kingdom Limited.
 *
 * All rights reserved.   May not be used without permission.
 */

package com.cadenzauk.siesta.expression;

import com.cadenzauk.core.function.MethodReference;
import com.cadenzauk.core.reflect.MethodUtil;
import com.cadenzauk.siesta.Alias;
import com.cadenzauk.siesta.Scope;
import com.cadenzauk.siesta.TypedExpression;
import com.cadenzauk.siesta.catalog.Column;
import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Method;

public class ResolvedColumn<T,R> implements TypedExpression<T> {
    private final Alias<R> alias;
    private final Column<T,R> column;

    private ResolvedColumn(Alias<R> alias, Column<T,R> column) {
        this.alias = alias;
        this.column = column;
    }

    @Override
    public String sql(Scope scope) {
        return alias.inSelectClauseSql(column);
    }

    @Override
    public String label(Scope scope) {
        return alias.inSelectClauseLabel(column);
    }

    @Override
    public RowMapper<T> rowMapper(String label) {
        return (rs, i) -> column.dataType().get(rs, label).orElse(null);
    }

    public static <T, R> ResolvedColumn<T,R> of(Alias<R> alias, MethodReference<R,T> getterReference) {
        Method method = MethodUtil.fromReference(alias.table().rowClass(), getterReference);
        String name = alias.table().catalog().namingStrategy().columnName(method.getName());
        return new ResolvedColumn<>(alias, Column.of(name, alias.table().rowClass()));
    }
}