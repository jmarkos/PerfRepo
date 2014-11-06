package org.jboss.qa.perfrepo.web.security;

import java.util.Collection;

import javax.inject.Inject;

import org.jboss.qa.perfrepo.web.dao.PermissionDAO;
import org.jboss.qa.perfrepo.web.dao.TestDAO;
import org.jboss.qa.perfrepo.model.Entity;
import org.jboss.qa.perfrepo.model.Test;
import org.jboss.qa.perfrepo.model.auth.AccessLevel;
import org.jboss.qa.perfrepo.model.auth.AccessType;
import org.jboss.qa.perfrepo.model.auth.Permission;
import org.jboss.qa.perfrepo.model.auth.SecuredEntity;
import org.jboss.qa.perfrepo.model.report.Report;
import org.jboss.qa.perfrepo.web.service.UserService;

public class AuthorizationService {

	@Inject
	TestDAO testDAO;

	@Inject
	PermissionDAO permissionDAO;

	@Inject
	UserService userService;

	private boolean isUserAuthorizedForReport(Long userId, AccessType accessType, Report report) {
		Collection<Permission> permissions = permissionDAO.getByReport(report.getId());
		if (permissions != null) {
			for (Permission permission : permissions) {
				// if the user require read access, it is not important if the permission is for read or write
				// if the user require write access, the permission must be for write
				if (accessType.equals(AccessType.READ) || permission.getAccessType().equals(AccessType.WRITE)) {
					if (permission.getLevel().equals(AccessLevel.PUBLIC)) {
						//Public permission, the access is granted
						return true;
					} else if (permission.getUserId() != null && permission.getLevel().equals(AccessLevel.USER)) {
						//USER permission, the permission userId should be same as user, who require the access
						if (userId.equals(permission.getUserId())) {
							return true;
						}
					} else if (permission.getGroupId() != null && permission.getLevel().equals(AccessLevel.GROUP)) {
						//GROUP permission, user must be assigned in permission group
						if (userService.isUserInGroup(userId, permission.getGroupId())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean isUserAuthorizedForTest(Long userId, AccessType accessType, Test test) {
		return userService.isLoggedUserInGroup(test.getGroupId());
	}

	private Report getParentReportEntity(Entity<?> entity) {
		if (entity instanceof Report) {
			return (Report) entity;
		}
		return null;
	}

	private Test getParentTestEntity(Entity<?> entity) {
		if (entity instanceof Test) {
			return (Test) entity;
		}
		return testDAO.getTestByRelation(entity);
	}

	public boolean isUserAuthorizedFor(Long userId, AccessType accessType, Entity<?> entity) {
		SecuredEntity securedEntityAnnotation = entity.getClass().getAnnotation(SecuredEntity.class);
		if (securedEntityAnnotation != null) {
			switch (securedEntityAnnotation.type()) {
				case TEST:
					return isUserAuthorizedForTest(userId, accessType, getParentTestEntity(entity));
				case REPORT:
					return isUserAuthorizedForReport(userId, accessType, getParentReportEntity(entity));
				case USER:
					//TODO: add implementation
					return true;
				default:
					return false;
			}
		}
		return true;
	}

	public boolean isUserAuthorizedFor(AccessType accessType, Entity<?> entity) {
		return isUserAuthorizedFor(userService.getLoggedUser().getId(), accessType, entity);
	}
}