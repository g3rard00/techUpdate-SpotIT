package com.bonitasoft.acm.rest.api

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstanceSearchDescriptor
import org.bonitasoft.engine.search.Order
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder

class CaseHistory implements RestApiController, BPMNamesConstants {

	private static final Logger LOGGER = LoggerFactory.getLogger(CaseHistory.class)

	@Override
	RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
		def caseId = request.getParameter "caseId"
		if (!caseId) {
			return responseBuilder.with {
				withResponseStatus(HttpServletResponse.SC_BAD_REQUEST)
				withResponse("""{"error" : "the parameter caseId is missing"}""")
                build()
            }
        }

        def processAPI = context.apiClient.getProcessAPI()
        //Retrieve archived activities
        def result = processAPI.searchArchivedHumanTasks(new SearchOptionsBuilder(0, Integer.MAX_VALUE).with {
            filter(ArchivedActivityInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID, caseId.toLong())
            differentFrom(ArchivedActivityInstanceSearchDescriptor.NAME, ACTIVITY_CONTAINER)
            sort(ArchivedActivityInstanceSearchDescriptor.REACHED_STATE_DATE, Order.DESC)
            done()
        }).getResult()
        .collect{
            def user = context.apiClient.getIdentityAPI().getUser(it.executedBy)
            [
                displayName:it.displayName,
                displayDescription:it.displayDescription ?: "",
                reached_state_date:it.reachedStateDate,
                executedBy:user
            ]
        }
        def caseInstance = processAPI.getProcessInstance(caseId.toLong())
        def user = context.apiClient.getIdentityAPI().getUser(caseInstance.startedBy)
        result.add(	[
            displayName:'Case started',
            displayDescription:"",
            reached_state_date:caseInstance.startDate,
            executedBy:user
        ])

        return responseBuilder.with {
            withResponseStatus(HttpServletResponse.SC_OK)
            withResponse(new JsonBuilder(result).toString())
            build()
        }
    }

}
