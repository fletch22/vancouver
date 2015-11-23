package com.fletch22.orb.query;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fletch22.orb.cache.query.CriteriaCollection;
import com.fletch22.orb.query.CriteriaFactory.Criteria;
import com.fletch22.orb.query.constraint.Constraint;
import com.fletch22.orb.query.constraint.ConstraintDetails;
import com.fletch22.orb.query.constraint.ConstraintDetailsList;
import com.fletch22.orb.query.constraint.ConstraintDetailsSingleValue;
import com.fletch22.orb.query.sort.CriteriaSortInfo;

@Component
public class CriteriaAttributeRenameHandler {

	public void handleAttributeRename(CriteriaCollection criteriaCollection, long orbTypeInternalId, String attributeOldName, String attributeNewName) {

		Set<Long> criteriaKey = criteriaCollection.getKeys();
		for (long id : criteriaKey) {
			Criteria criteria = criteriaCollection.getByQueryId(id);

			if (criteria.getOrbType().id == orbTypeInternalId) {
				renameInConstraints(criteria, attributeOldName, attributeNewName);
				renameInSortInfo(criteria, attributeOldName, attributeNewName);
			}
		}
	}

	private void renameInSortInfo(Criteria criteria, String attributeOldName, String attributeNewName) {

		List<CriteriaSortInfo> criteriaSortInfoList = criteria.getSortInfoList();
		for (CriteriaSortInfo criteriaSortInfo : criteriaSortInfoList) {

			if (criteriaSortInfo.sortAttributeName.equals(attributeOldName)) {
				criteriaSortInfo.sortAttributeName = attributeNewName;
			}
		}
	}

	private void renameInConstraints(Criteria criteria, String attributeOldName, String attributeNewName) {
		handleAttributeRename(criteria.logicalConstraint, attributeOldName, attributeNewName);
	}

	private void handleAttributeRename(LogicalConstraint logicalConstraint, String attributeOldName, String attributeNewName) {
		List<Constraint> constraintList = logicalConstraint.constraintList;

		for (Constraint constraintInner : constraintList) {

			if (constraintInner instanceof ConstraintDetailsSingleValue) {
				handleAttributeRename((ConstraintDetails) constraintInner, attributeOldName, attributeNewName);
			} else if (constraintInner instanceof ConstraintDetailsList) {
				handleAttributeRename((ConstraintDetails) constraintInner, attributeOldName, attributeNewName);
			} else if (constraintInner instanceof LogicalConstraint) {
				handleAttributeRename((LogicalConstraint) constraintInner, attributeOldName, attributeNewName);
			}
		}
	}

	private void handleAttributeRename(ConstraintDetails constraintDetails, String attributeOldName, String attributeNewName) {
		if (constraintDetails.attributeName.equals(attributeOldName)) {
			constraintDetails.attributeName = attributeNewName;
		}
	}
}