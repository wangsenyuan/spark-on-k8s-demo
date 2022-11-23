package com.wsy.demo.sql

import org.apache.spark.sql.Column
import org.apache.spark.sql.catalyst.analysis.TypeCheckResult
import org.apache.spark.sql.catalyst.expressions.{Add, AggregateWindowFunction, And, AttributeReference, EqualNullSafe, Expression, If, Literal}
import org.apache.spark.sql.types.{BooleanType, DataType, IntegerType}

case class FirstPredicateAggregateWindowFunction(predicate: Expression,
                                                 valueBeforePredicate: Expression,
                                                 valueAfterPredicate: Expression)
  extends AggregateWindowFunction {

  override def children: Seq[Expression] =
    predicate :: valueBeforePredicate :: valueAfterPredicate :: Nil

  /**
   * Value dataTypes of 'valueBeforePredicate' and 'valueAfterPredicate' should be the same.
   * valueBeforePredicate = lit(1), valueAfterPredicate = lit(2) is correct. Same int type.
   * valueBeforePredicate = lit(1), valueAfterPredicate = lit(true) is incorrect. Different types.
   *
   * 'predicate' expression should return boolean type.
   * col("...") === 1 is correct.
   * lit(1) is incorrect.
   */
  override def checkInputDataTypes(): TypeCheckResult =
    (predicate.dataType, valueBeforePredicate.dataType, valueAfterPredicate.dataType) match {
      case (BooleanType, trueType, falseType) if falseType == trueType =>
        TypeCheckResult.TypeCheckSuccess
      case (BooleanType, trueType, falseType) if falseType != trueType =>
        TypeCheckResult.TypeCheckFailure("Returned DataType should be the same for trueValue and falseValue!")
      case _ =>
        TypeCheckResult.TypeCheckFailure("Predicate DataType should be BooleanType!")
    }

  /**
   * DataType of adding or modifying column.
   * For example: .withColumn("This column type", ...)
   */
  override def dataType: DataType = valueBeforePredicate.dataType

  /* zero value constant */
  private val zero: Literal = Literal(0)

  /* one value constant */
  private val one: Literal = Literal(1)

  /**
   * Internal buffer to keep counter.
   * If predicate expression returns true then increment counter
   */
  private val counter: AttributeReference =
    AttributeReference("counter", IntegerType, nullable = false)()

  /* Internal buffer to keep 'valueBeforePredicate' */
  private lazy val valueBefore: AttributeReference =
    AttributeReference("valueBefore", valueBeforePredicate.dataType, nullable = true)()

  /* Internal buffer to keep 'valueAfterPredicate' */
  private lazy val valueAfter: AttributeReference =
    AttributeReference("valueAfter", valueAfterPredicate.dataType, nullable = true)()


  /* Initial value of 'counter' column */
  override val initialValues: Seq[Expression] = zero :: Nil

  /**
   * Updating 'counter' column if predicate expression returns true.
   * Notice that counter can be updated only once because when counter
   * become = 1 then 'EqualNullSafe(counter, zero)' is always false.
   */
  override val updateExpressions: Seq[Expression] =
    If(
      predicate = And(predicate, EqualNullSafe(counter, zero)),
      trueValue = Add(counter, one), // counter += 1
      falseValue = counter) :: valueBeforePredicate :: valueAfterPredicate :: Nil


  /* List of internal columns to store in buffer */
  override lazy val aggBufferAttributes: List[AttributeReference] =
    counter :: valueBefore :: valueAfter :: Nil

  /* If counter == 1 then predicate expression was matched and other values should have */
  override lazy val evaluateExpression: Expression =
    If(EqualNullSafe(counter, one), valueBefore, valueAfter)

}

object FirstPredicateAggregateWindowFunction {

  def first_predicate(predicateExp: Column,
                      firstSatisfied: Column,
                      other: Column): Column = {
    new Column(FirstPredicateAggregateWindowFunction(
      predicateExp.expr,
      firstSatisfied.expr,
      other.expr))
  }

}
