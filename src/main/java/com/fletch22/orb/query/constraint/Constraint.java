package com.fletch22.orb.query.constraint;

import java.util.List;

import com.fletch22.orb.cache.local.CacheEntry;
import com.fletch22.orb.query.CriteriaAggregate;
import com.fletch22.orb.query.RelationshipOperator;
import com.fletch22.orb.query.constraint.aggregate.Aggregate;
import com.googlecode.cqengine.query.Query;

public abstract class Constraint {
	
	public abstract Constraint[] getConstraints();
	
	public Constraint() {
		super();
	}
	
	public static Constraint eq(String attributeName, String operativeValue) {
		ConstraintDetailsSingleValue constraintDetails = new ConstraintDetailsSingleValue();
		
		constraintDetails.relationshipOperator = RelationshipOperator.EQUALS;
		constraintDetails.attributeName = attributeName;
		constraintDetails.operativeValue = operativeValue;
		
		return constraintDetails;
	}
	
	public static Constraint in(String attributeName, List<String> operativeList) {
		ConstraintDetailsList constraintDetailsList = new ConstraintDetailsList();
		
		constraintDetailsList.relationshipOperator = RelationshipOperator.IN;
		constraintDetailsList.attributeName = attributeName;
		constraintDetailsList.operativeValueList = operativeList;
		
		return constraintDetailsList;
	}
	
	public static Constraint is(String attributeName, Aggregate aggregate, CriteriaAggregate criteriaForAggregation) {
		ConstraintDetailsAggregate constraintDetailsAggregate = new ConstraintDetailsAggregate();
		
		constraintDetailsAggregate.relationshipOperator = RelationshipOperator.IS;
		constraintDetailsAggregate.attributeName = attributeName;
		constraintDetailsAggregate.aggregate = aggregate;
		constraintDetailsAggregate.criteriaForAggregation = criteriaForAggregation;
		
		return constraintDetailsAggregate;
	}
	
	public abstract Query<CacheEntry> acceptConstraintProcessorVisitor(ConstraintProcessVisitor visitor, long orbTypeInternalId);
	
	public abstract void acceptConstraintRegistrationVisitor(ConstraintRegistrationVisitor constraintVisitor);
	
	public abstract void acceptConstraintSetParentVisitor(ConstraintSetParentVisitor constraintSetParentVisitor); 
	
	public abstract void acceptConstraintDeleteChildCriteriaVisitor(ConstraintDeleteChildCriteriaVisitor constraintDeleteChildCriteriaVisitor);
	
	public abstract void acceptConstraintRenameChildCriteriaAttributeVisitor(ConstraintRenameChildCriteriaAttributeVisitor visitor);
}

