package org.testobject.rest.api.resource;


import org.testobject.rest.api.model.SessionReport;
import org.testobject.rest.api.model.PaginationObject;

public interface SessionReportResource {

    PaginationObject<SessionReport> getSessionReport(String user);

    PaginationObject<SessionReport> getSessionReport(String user, String userId,
                                                     long offset, int limit, int lastDays);
}
