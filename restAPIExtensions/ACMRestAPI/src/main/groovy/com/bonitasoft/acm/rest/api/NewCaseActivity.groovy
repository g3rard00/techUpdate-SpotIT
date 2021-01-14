package com.bonitasoft.acm.rest.api

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.apache.http.HttpHeaders
import org.bonitasoft.web.extension.ResourceProvider
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.bonitasoft.engine.bpm.flownode.ManualTaskCreator
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController


class CreateCaseActivity implements RestApiController, CaseActivityHelper, BPMNamesConstants {

    def static final Logger LOGGER = LoggerFactory.getLogger(CreateCaseActivity.class)

    @Override
    RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
		
		def jsonBody = new JsonSlurper().parse(request.getReader())
		if(!jsonBody.name) {
			return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, """{"error" : "the parameter name is missing"}""")
        }
		
        if(!jsonBody.caseId) {
			return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, """{"error" : "the parameter caseId is missing"}""")
		}

		def processAPI = context.apiClient.getProcessAPI()

		def activityContainerInstance = findTaskInstance(jsonBody.caseId.toLong(), ACTIVITY_CONTAINER, processAPI)
		if(!activityContainerInstance) {
			return buildResponse(responseBuilder, HttpServletResponse.SC_NOT_FOUND, "No $ACTIVITY_CONTAINER found")
		}

		processAPI.assignUserTask(activityContainerInstance.id, context.apiSession.userId)
		processAPI.addManualUserTask(new ManualTaskCreator(activityContainerInstance.id, jsonBody.name).with{
					setDisplayName(jsonBody.name)
					setDescription(jsonBody.description)
					setAssignTo(context.apiSession.userId)
					setDueDate(null)
				})
		
        return buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder(name:jsonBody.name).toString())
    }

    RestApiResponse buildResponse(RestApiResponseBuilder responseBuilder, int httpStatus, Serializable body) {
        return responseBuilder.with {
            withResponseStatus(httpStatus)
            withResponse(body)
            build()
        }
    }


}