package com.bonitasoft.acm.rest.api

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder

import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder

class DeleteDocument implements RestApiController {

	@Override
	RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
		def documentId = request.getParameter "documentId"
		if (documentId == null) {
			return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST,"""{"error" : "the parameter documentId is missing"}""")
        }
        def processAPI = context.apiClient.getProcessAPI()
        processAPI.removeDocument(documentId.toLong())

        return buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder([id:documentId]).toString())
    }


    RestApiResponse buildResponse(RestApiResponseBuilder responseBuilder, int httpStatus, Serializable body) {
        return responseBuilder.with {
            withResponseStatus(httpStatus)
            withResponse(body)
            build()
        }
    }
}
