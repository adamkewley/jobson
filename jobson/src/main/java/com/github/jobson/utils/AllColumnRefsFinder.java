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

package com.github.jobson.utils;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.*;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.sequence.AlterSequence;
import net.sf.jsqlparser.statement.comment.Comment;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.schema.CreateSchema;
import net.sf.jsqlparser.statement.create.sequence.CreateSequence;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.AlterView;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.execute.Execute;
import net.sf.jsqlparser.statement.grant.Grant;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.replace.Replace;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.upsert.Upsert;
import net.sf.jsqlparser.statement.values.ValuesStatement;

import java.util.HashSet;
import java.util.Set;

public final class AllColumnRefsFinder implements StatementVisitor, SelectVisitor, SelectItemVisitor, ExpressionVisitor {

    private Set<String> columns;



    public Set<String> getAllColumnNamesThatAppearIn(Statement statement) {
        this.columns = new HashSet<>();

        statement.accept(this);

        return this.columns;
    }

    public void visit(Select select) {
        select.getSelectBody().accept(this);
    }

    public void visit(PlainSelect plainSelect) {
        for (SelectItem selectItem : plainSelect.getSelectItems()) {
            selectItem.accept(this);
        }

        if (plainSelect.getWhere() != null)
            plainSelect.getWhere().accept(this);
    }

    public void visit(SelectExpressionItem selectExpressionItem) {
        selectExpressionItem.getExpression().accept(this);
    }

    public void visit(AndExpression andExpression) {
        andExpression.getLeftExpression().accept(this);
        andExpression.getRightExpression().accept(this);
    }

    public void visit(OrExpression orExpression) {
        orExpression.getLeftExpression().accept(this);
        orExpression.getRightExpression().accept(this);
    }

    public void visit(EqualsTo equalsTo) {
        equalsTo.getLeftExpression().accept(this);
        equalsTo.getRightExpression().accept(this);
    }

    public void visit(GreaterThan greaterThan) {
        greaterThan.getLeftExpression().accept(this);
        greaterThan.getRightExpression().accept(this);
    }

    public void visit(MinorThan minorThan) {
        minorThan.getLeftExpression().accept(this);
        minorThan.getRightExpression().accept(this);
    }


    public void visit(Column column) {
        this.columns.add(column.getColumnName());
    }



    // These are blank because they are supported (and therefore shouldn't throw)
    // but do not contribute toward finding the column names.

    public void visit(AllColumns allColumns) {
    }

    public void visit(AllTableColumns allTableColumns) {
    }

    public void visit(Function function) {
    }

    public void visit(LongValue longValue) {
    }

    public void visit(SetOperationList setOperationList) {
    }

    @Override
    public void visit(BitwiseRightShift bitwiseRightShift) {

    }

    @Override
    public void visit(BitwiseLeftShift bitwiseLeftShift) {

    }

    public void visit(NullValue nullValue) {
    }

    public void visit(DoubleValue doubleValue) {
    }

    public void visit(StringValue stringValue) {
    }

    public void visit(InExpression inExpression) {
    }

    @Override
    public void visit(FullTextSearch fullTextSearch) {

    }


    // Unimplemented SQL features

    public void visit(SignedExpression signedExpression) {
        throw new UnsupportedSQLFeatureException("Feature SignedExpression not supported");
    }

    public void visit(JdbcParameter jdbcParameter) {
        throw new UnsupportedSQLFeatureException("Feature JdbcParameter not supported");
    }

    public void visit(JdbcNamedParameter jdbcNamedParameter) {
        throw new UnsupportedSQLFeatureException("Feature JdbcNamedParameter not supported");
    }

    public void visit(HexValue hexValue) {
        throw new UnsupportedSQLFeatureException("Feature HexValue not supported");
    }

    public void visit(DateValue dateValue) {
        throw new UnsupportedSQLFeatureException("Feature DateValue not supported");
    }

    public void visit(TimeValue timeValue) {
        throw new UnsupportedSQLFeatureException("Feature TimeValue not supported");
    }

    public void visit(TimestampValue timestampValue) {
        throw new UnsupportedSQLFeatureException("Feature TimestampValue not supported");
    }

    public void visit(Parenthesis parenthesis) {
        throw new UnsupportedSQLFeatureException("Feature Parenthesis not supported");
    }

    public void visit(Addition addition) {
        throw new UnsupportedSQLFeatureException("Feature Addition not supported");
    }

    public void visit(Division division) {
        throw new UnsupportedSQLFeatureException("Feature Division not supported");
    }

    @Override
    public void visit(IntegerDivision integerDivision) {
        throw new UnsupportedSQLFeatureException("Feature IntegerDivision not supported");
    }

    public void visit(Multiplication multiplication) {
        throw new UnsupportedSQLFeatureException("Feature Multiplication not supported");
    }

    public void visit(Subtraction subtraction) {
        throw new UnsupportedSQLFeatureException("Feature Subtraction not supported");
    }

    public void visit(Between between) {
        throw new UnsupportedSQLFeatureException("Feature Between not supported");
    }

    public void visit(GreaterThanEquals greaterThanEquals) {
        throw new UnsupportedSQLFeatureException("Feature GreaterThanEquals not supported");
    }

    public void visit(IsNullExpression isNullExpression) {
        throw new UnsupportedSQLFeatureException("Feature IsNullExpression not supported");
    }

    @Override
    public void visit(IsBooleanExpression isBooleanExpression) {
        throw new UnsupportedSQLFeatureException("Feature IsBooleanExpression not supported");
    }

    public void visit(LikeExpression likeExpression) {
        throw new UnsupportedSQLFeatureException("Feature LikeExpression not supported");
    }


    public void visit(MinorThanEquals minorThanEquals) {
        throw new UnsupportedSQLFeatureException("Feature MinorThanEquals not supported");
    }

    public void visit(NotEqualsTo notEqualsTo) {
        throw new UnsupportedSQLFeatureException("Feature NotEqualsTo not supported");
    }

    public void visit(WithItem withItem) {
        throw new UnsupportedSQLFeatureException("Feature WithItem not supported");
    }

    @Override
    public void visit(Comment comment) {

    }

    @Override
    public void visit(Commit commit) {
        throw new UnsupportedSQLFeatureException("Feature Commit not supported");
    }

    public void visit(Delete delete) {
        throw new UnsupportedSQLFeatureException("Feature Delete not supported");
    }

    public void visit(Update update) {
        throw new UnsupportedSQLFeatureException("Feature Update not supported");
    }

    public void visit(Insert insert) {
        throw new UnsupportedSQLFeatureException("Feature Insert not supported");
    }

    public void visit(Replace replace) {
        throw new UnsupportedSQLFeatureException("Feature Replace not supported");
    }

    public void visit(Drop drop) {
        throw new UnsupportedSQLFeatureException("Feature Drop not supported");
    }

    public void visit(Truncate truncate) {
        throw new UnsupportedSQLFeatureException("Feature Truncate not supported");
    }

    public void visit(CreateIndex createIndex) {
        throw new UnsupportedSQLFeatureException("Feature CreateIndex not supported");
    }

    @Override
    public void visit(CreateSchema createSchema) {
        throw new UnsupportedSQLFeatureException("Feature CreateSchema not supported");
    }

    public void visit(CreateTable createTable) {
        throw new UnsupportedSQLFeatureException("Feature CreateTable not supported");
    }

    public void visit(CreateView createView) {
        throw new UnsupportedSQLFeatureException("Feature CreateView not supported");
    }

    public void visit(AlterView alterView) {
        throw new UnsupportedSQLFeatureException("Feature AlterView not supported");
    }

    public void visit(Alter alter) {
        throw new UnsupportedSQLFeatureException("Feature Alter not supported");
    }

    public void visit(Statements statements) {
        throw new UnsupportedSQLFeatureException("Feature Statements not supported");
    }

    public void visit(Execute execute) {
        throw new UnsupportedSQLFeatureException("Feature Execute not supported");
    }

    public void visit(SetStatement setStatement) {
        throw new UnsupportedSQLFeatureException("Feature SetStatement not supported");
    }

    @Override
    public void visit(ShowColumnsStatement showColumnsStatement) {
        throw new UnsupportedSQLFeatureException("Feature ShowColumnsStatement not supported");
    }

    public void visit(Merge merge) {
        throw new UnsupportedSQLFeatureException("Feature Merge not supported");
    }

    public void visit(SubSelect subSelect) {
        throw new UnsupportedSQLFeatureException("Feature SubSelect not supported");
    }

    public void visit(CaseExpression caseExpression) {
        throw new UnsupportedSQLFeatureException("Feature CaseExpression not supported");
    }

    public void visit(WhenClause whenClause) {
        throw new UnsupportedSQLFeatureException("Feature WhenClause not supported");
    }

    public void visit(ExistsExpression existsExpression) {
        throw new UnsupportedSQLFeatureException("Feature ExistsExpression not supported");
    }

    public void visit(AllComparisonExpression allComparisonExpression) {
        throw new UnsupportedSQLFeatureException("Feature AllComparisonExpression not supported");
    }

    public void visit(AnyComparisonExpression anyComparisonExpression) {
        throw new UnsupportedSQLFeatureException("Feature AnyComparisonExpression not supported");
    }

    public void visit(Concat concat) {
        throw new UnsupportedSQLFeatureException("Feature Concat not supported");
    }

    public void visit(Matches matches) {
        throw new UnsupportedSQLFeatureException("Feature Matches not supported");
    }

    public void visit(BitwiseAnd bitwiseAnd) {
        throw new UnsupportedSQLFeatureException("Feature BitwiseAnd not supported");
    }

    public void visit(BitwiseOr bitwiseOr) {
        throw new UnsupportedSQLFeatureException("Feature BitwiseOr not supported");
    }

    public void visit(BitwiseXor bitwiseXor) {
        throw new UnsupportedSQLFeatureException("Feature BitwiseXor not supported");
    }

    public void visit(CastExpression castExpression) {
        throw new UnsupportedSQLFeatureException("Feature CastExpression not supported");
    }

    public void visit(Modulo modulo) {
        throw new UnsupportedSQLFeatureException("Feature Modulo not supported");
    }

    public void visit(AnalyticExpression analyticExpression) {
        throw new UnsupportedSQLFeatureException("Feature AnalyticExpression not supported");
    }

    public void visit(ExtractExpression extractExpression) {
        throw new UnsupportedSQLFeatureException("Feature ExtractExpression not supported");
    }

    public void visit(IntervalExpression intervalExpression) {
        throw new UnsupportedSQLFeatureException("Feature IntervalExpression not supported");
    }

    public void visit(OracleHierarchicalExpression oracleHierarchicalExpression) {
        throw new UnsupportedSQLFeatureException("Feature OracleHierarchicalExpression not supported");
    }

    public void visit(RegExpMatchOperator regExpMatchOperator) {
        throw new UnsupportedSQLFeatureException("Feature RegExpMatchOperator not supported");
    }

    public void visit(JsonExpression jsonExpression) {
        throw new UnsupportedSQLFeatureException("Feature JsonExpression not supported");
    }

    @Override
    public void visit(JsonOperator jsonOperator) {
        throw new UnsupportedSQLFeatureException("Feature JsonOperator not supported");
    }

    public void visit(RegExpMySQLOperator regExpMySQLOperator) {
        throw new UnsupportedSQLFeatureException("Feature RegExpMySQLOperator not supported");
    }

    public void visit(UserVariable userVariable) {
        throw new UnsupportedSQLFeatureException("Feature UserVariable not supported");
    }

    public void visit(NumericBind numericBind) {
        throw new UnsupportedSQLFeatureException("Feature NumericBind not supported");
    }

    public void visit(KeepExpression keepExpression) {
        throw new UnsupportedSQLFeatureException("Feature KeepExpression not supported");
    }

    public void visit(MySQLGroupConcat mySQLGroupConcat) {
        throw new UnsupportedSQLFeatureException("Feature MySQLGroupConcat not supported");
    }

    @Override
    public void visit(ValueListExpression valueListExpression) {
    }

    public void visit(RowConstructor rowConstructor) {
        throw new UnsupportedSQLFeatureException("Feature RowConstructor not supported");
    }

    public void visit(OracleHint oracleHint) {
        throw new UnsupportedSQLFeatureException("Feature OracleHint not supported");
    }

    public void visit(TimeKeyExpression timeKeyExpression) {
        throw new UnsupportedSQLFeatureException("Feature TimeKeyExpression not supported");
    }

    public void visit(DateTimeLiteralExpression dateTimeLiteralExpression) {
        throw new UnsupportedSQLFeatureException("Feature DateTimeLiteralExpression not supported");
    }

    @Override
    public void visit(NotExpression notExpression) {
        throw new UnsupportedSQLFeatureException("Feature NotExpression not supported");
    }

    @Override
    public void visit(NextValExpression nextValExpression) {
        throw new UnsupportedSQLFeatureException("Feature NextValExpression not supported");
    }

    @Override
    public void visit(CollateExpression collateExpression) {
        throw new UnsupportedSQLFeatureException("Feature CollateExpression not supported");
    }

    @Override
    public void visit(SimilarToExpression similarToExpression) {
        throw new UnsupportedSQLFeatureException("Feature SimilarToExpression not supported");
    }

    @Override
    public void visit(ArrayExpression arrayExpression) {
        throw new UnsupportedSQLFeatureException("Feature ArrayExpression not supported");
    }

    @Override
    public void visit(Upsert upsert) {
        throw new UnsupportedSQLFeatureException("Feature Upsert not supported");
    }

    @Override
    public void visit(UseStatement useStatement) {
        throw new UnsupportedSQLFeatureException("Feature UseStatement not supported");
    }

    @Override
    public void visit(Block block) {
        throw new UnsupportedSQLFeatureException("Feature Block not supported");
    }

    @Override
    public void visit(ValuesStatement valuesStatement) {
        throw new UnsupportedSQLFeatureException("Feature ValuesStatement not supported");
    }

    @Override
    public void visit(DescribeStatement describeStatement) {
        throw new UnsupportedSQLFeatureException("Feature DescribeStatement not supported");
    }

    @Override
    public void visit(ExplainStatement explainStatement) {
        throw new UnsupportedSQLFeatureException("Feature ExplainStatement not supported");
    }

    @Override
    public void visit(ShowStatement showStatement) {
        throw new UnsupportedSQLFeatureException("Feature ShowStatement not supported");
    }

    @Override
    public void visit(DeclareStatement declareStatement) {
        throw new UnsupportedSQLFeatureException("Feature DeclareStatement not supported");
    }

    @Override
    public void visit(Grant grant) {
        throw new UnsupportedSQLFeatureException("Feature Grant not supported");
    }

    @Override
    public void visit(CreateSequence createSequence) {
        throw new UnsupportedSQLFeatureException("Feature CreateSequence not supported");
    }

    @Override
    public void visit(AlterSequence alterSequence) {
        throw new UnsupportedSQLFeatureException("Feature AlterSequence not supported");
    }

    @Override
    public void visit(CreateFunctionalStatement createFunctionalStatement) {
        throw new UnsupportedSQLFeatureException("Feature CreateFunctionalStatement not supported");
    }
}
