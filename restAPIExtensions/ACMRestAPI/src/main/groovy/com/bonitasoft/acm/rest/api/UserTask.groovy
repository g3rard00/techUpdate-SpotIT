package com.bonitasoft.acm.rest.api

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder

import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonSlurper

class UserTask implements RestApiController {



	@Override
	RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
		def jsonBody = new JsonSlurper().parse(request.getReader())
		def processAPI = context.apiClient.getProcessAPI()
		if(!jsonBody.taskId) {
			return responseBuilder.with {
				withResponseStatus(HttpServletResponse.SC_BAD_REQUEST)
				withResponse("No taskId in payload")
				build()
			}
		}
		processAPI.assignAndExecuteUserTask(context.apiSession.userId, jsonBody.taskId.toLong(), jsonBody)
		if(jsonBody.content) {
			def processInstanceId = processAPI.getProcessInstanceIdFromActivityInstanceId(jsonBody.taskId.toLong())
			processAPI.addProcessComment(processInstanceId, jsonBody.content)
		}
		return responseBuilder.with {
			withResponseStatus(HttpServletResponse.SC_CREATED)
			build()
		}
	}
}
