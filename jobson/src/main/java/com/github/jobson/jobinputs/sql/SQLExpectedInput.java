/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.github.jobson.jobinputs.sql;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jobson.jobinputs.JobExpectedInput;
import com.github.jobson.jobinputs.JobExpectedInputId;
import com.github.jobson.utils.ValidationError;
import io.swagger.v3.oas.annotations.media.Schema;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.statement.Statement;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.github.jobson.Helpers.commaSeparatedList;
import static com.github.jobson.Helpers.randomElementIn;
import static com.github.jobson.utils.SQLUtils.columnRefsIn;
import static com.github.jobson.utils.SQLUtils.tableRefsIn;
import static com.google.common.collect.Sets.difference;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static net.sf.jsqlparser.parser.CCJSqlParserUtil.parse;

@Schema(description = "Schema for an input that requires clients to construct an SQL query")
public final class SQLExpectedInput extends JobExpectedInput<SQLInput> {

    @JsonProperty
    @NotNull
    @NotEmpty
    private List<TableSchema> tables;



    /**
     * @deprecated Used by JSON deserializer
     */
    public SQLExpectedInput() {}

    public SQLExpectedInput(
            JobExpectedInputId id,
            String name,
            String description,
            List<TableSchema> tables) {

        super(id, name, description, Optional.empty());
        this.tables = tables;
    }


    public List<TableSchema> getTables() {
        return this.tables;
    }


    @Override
    public Class<SQLInput> getExpectedInputClass() {
        return SQLInput.class;
    }

    @Override
    public Optional<List<ValidationError>> validate(SQLInput input) {
        final Statement statement;
        try {
            statement = parse(input.getValue());
        } catch (JSQLParserException e) {
            return Optional.of(singletonList(ValidationError.of("Query cannot be parsed as SQL")));
        }

        final Set<String> tablesInQuery = tableRefsIn(statement);
        final Set<String> tablesInSchema = idsOfAllTablesInThisExpectedInput();
        final Set<String> nonExistentTablesInQuery = difference(tablesInQuery, tablesInSchema);

        if (nonExistentTablesInQuery.isEmpty()) {
            final Set<String> columnsInAllReferencedTables =
                    tablesInQuery.stream().flatMap(queryTable ->
                            this.tables
                                    .stream()
                                    .filter(schema -> schema.getId().equals(queryTable))
                                    .findFirst()
                                    .map(schema -> schema.getColumns().stream().map(ColumnSchema::getId))
                                    .get())
                            .collect(toSet());

            final Set<String> columnsReferencedInQuery = columnRefsIn(statement);

            final Set<String> columnsNotInSchema =
                    difference(columnsReferencedInQuery, columnsInAllReferencedTables);

            if (columnsNotInSchema.isEmpty()) {
                return Optional.empty();
            } else {
                final String errorMessage = format(
                        "Columns referenced in query (%s) do not exist. Available columns: %s",
                        commaSeparatedList(columnsNotInSchema),
                        commaSeparatedList(columnsInAllReferencedTables));
                return Optional.of(singletonList(ValidationError.of(errorMessage)));
            }
        } else {
            final String errorMessage = format(
                    "Tables referenced in query (%s) do not exist. Available tables: %s",
                    commaSeparatedList(nonExistentTablesInQuery),
                    commaSeparatedList(tablesInSchema));
            return Optional.of(singletonList(ValidationError.of(errorMessage)));
        }
    }

    @Override
    public SQLInput generateExampleInput() {
        final TableSchema table = randomElementIn(tables);
        final String cols = commaSeparatedList(table.getColumns().stream().map(ColumnSchema::getId).collect(toList()));
        final String query = format("select %s from %s;", cols, table.getId());
        return new SQLInput(query);
    }

    @JsonIgnore
    private Set<String> idsOfAllTablesInThisExpectedInput() {
        return this.tables.stream().map(TableSchema::getId).collect(toSet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SQLExpectedInput that = (SQLExpectedInput) o;

        return tables != null ? tables.equals(that.tables) : that.tables == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (tables != null ? tables.hashCode() : 0);
        return result;
    }
}
