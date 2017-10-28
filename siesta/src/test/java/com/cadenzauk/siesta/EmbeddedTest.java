/*
 * Copyright (c) 2017 Cadenza United Kingdom Limited
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.cadenzauk.siesta;

import com.cadenzauk.core.MockitoTest;
import com.cadenzauk.siesta.dialect.AnsiDialect;
import com.cadenzauk.siesta.grammar.expression.Aggregates;
import com.cadenzauk.siesta.model.MoneyAmount;
import com.cadenzauk.siesta.model.PartRow;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import static com.cadenzauk.siesta.grammar.expression.TypedExpression.column;
import static com.cadenzauk.siesta.model.TestDatabase.testDatabase;
import static org.apache.commons.lang3.ArrayUtils.toArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;

public class EmbeddedTest extends MockitoTest {
    @Mock
    private SqlExecutor sqlExecutor;

    @Captor
    private ArgumentCaptor<String> sql;

    @Captor
    private ArgumentCaptor<Object[]> args;

    @Test
    void embeddedInWhereClause() {
        Database database = testDatabase(new AnsiDialect());

        database.from(PartRow.class)
            .select(Aggregates.count())
            .where(PartRow::purchasePrice).dot(MoneyAmount::currency).isEqualTo("USD")
            .or(PartRow::retailPrice).dot(MoneyAmount::currency).isEqualTo("NZD")
            .optional(sqlExecutor);

        Mockito.verify(sqlExecutor).query(sql.capture(), args.capture(), any());
        assertThat(sql.getValue(), is("select count(*) as count_1 from SIESTA.PART PART where PART.PURCHASE_PRICE_CURRENCY = ? or PART.RETAIL_PRICE_CURRENCY = ?"));
        assertThat(args.getValue(), is(toArray("USD", "NZD")));
    }

    @Test
    void embeddedInProjection() {
        Database database = testDatabase(new AnsiDialect());

        database.from(PartRow.class)
            .select(PartRow::purchasePrice, "pp")
            .comma(PartRow::retailPrice, "rp")
            .comma(column(PartRow::purchasePrice).dot(MoneyAmount::currency), "pc")
            .comma(column(PartRow::retailPrice).dot(MoneyAmount::currency), "rc")
            .where(PartRow::partId).isEqualTo(4L)
            .optional(sqlExecutor);

        Mockito.verify(sqlExecutor).query(sql.capture(), args.capture(), any());
        assertThat(sql.getValue(), is(
            "select PART.PURCHASE_PRICE_AMOUNT as pp_AMOUNT, " +
                "PART.PURCHASE_PRICE_CURRENCY as pp_CURRENCY, " +
                "PART.RETAIL_PRICE_AMOUNT as rp_AMOUNT, " +
                "PART.RETAIL_PRICE_CURRENCY as rp_CURRENCY, " +
                "PART.PURCHASE_PRICE_CURRENCY as pc, " +
                "PART.RETAIL_PRICE_CURRENCY as rc " +
                "from SIESTA.PART PART " +
                "where PART.PART_ID = ?"));
        assertThat(args.getValue(), is(toArray(4L)));
    }
}
