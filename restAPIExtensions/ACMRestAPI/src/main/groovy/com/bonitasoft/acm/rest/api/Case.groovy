package com.bonitasoft.acm.rest.api

import java.time.format.DateTimeFormatter

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.bpm.process.ProcessInstanceNotFoundException
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.engine.search.SearchResult
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder

import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.engine.bpm.process.impl.ProcessInstanceSearchDescriptor
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController
import com.company.model.DisputeDAO

import groovy.json.JsonBuilder

class Case implements RestApiController, BPMNamesConstants {
	

    @Override
    RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
        def contextPath = request.contextPath
        def processAPI = context.apiClient.getProcessAPI()
        def searchData = newSearchBusinessData(processAPI)

        def p = request.getParameter "p"
        def c = request.getParameter "c"

        if(!p) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "Parameter `p` is mandatory")
        }
        if(!c) {
            return buildResponse(responseBuilder, HttpServletResponse.SC_BAD_REQUEST, "Parameter `c` is mandatory")
        }

        def searchIndex = request.getParameter("s")
//TODO		ajouter parametre Process

        def pInt = p as int
        def cInt = c as int
        def searchResult = searchInstances(processAPI, pInt, cInt, searchIndex)
        def result = searchResult
                .result
                .collect {
                    toCase([
                        processAPI:processAPI,
                        apiClient: context.apiClient,
                        searchData:searchData,
                        isOpen:true,
                        contextPath:contextPath,
                        caseId:it.id
                    ])
                }

        return buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder(result).toString(), pInt, cInt, searchResult.count)
    }

    SearchResult searchInstances(ProcessAPI processAPI, p, c, searchIndex) {
        return processAPI.searchProcessInstances(new SearchOptionsBuilder(p * c, c).with {
            filter(ProcessInstanceSearchDescriptor.NAME, PROCESS_NAME)
            done()
        })
    }

    def toCase(caseInput) {
        def caseId = caseInput.caseId
        
		SearchBusinessData mySearchData = caseInput.searchData
		//TODO nom a changer
		def dispute = caseInput.searchData.search(caseId,'dispute_ref', caseInput.apiClient.getDAO(DisputeDAO))
        return [
            id: caseId,
            open: caseInput.isOpen,
            caseUrl:[
                href: caseUrl(caseId, caseInput.processAPI, caseInput.contextPath),
                target: '_top',//caseInput.isOpen ? '_target' : '_self',
            ],
            customer: "$dispute.customer",
        ]
    }

    def SearchBusinessData newSearchBusinessData(ProcessAPI processAPI) {
        new SearchBusinessData(processAPI)
    }

    def String caseUrl(long caseId, ProcessAPI processAPI, contextPath) {
        try {
            def instance = processAPI.getProcessInstance(caseId)
            instance ? "$contextPath/apps/$LIVINGAPP_TOKEN/$CASEPAGE_TOKEN?id=$caseId" : ''
        } catch(ProcessInstanceNotFoundException e) {
            def instance = processAPI.getArchivedProcessInstance(caseId)
            def pDef = processAPI.getProcessDefinition(instance.processDefinitionId)
            "$contextPath/portal/resource/processInstance/$pDef.name/$pDef.version/content/?id=$instance.sourceObjectId"
        }
    }

    def RestApiResponse buildResponse(RestApiResponseBuilder responseBuilder, int httpStatus, Serializable body) {
        return responseBuilder
                .withResponseStatus(httpStatus)
                .withResponse(body)
                .build()
    }

    def RestApiResponse buildResponse(RestApiResponseBuilder responseBuilder, int httpStatus, Serializable body, int p, int c, long totalSize) {
        return responseBuilder
                .withContentRange(p, c, totalSize)
                .withResponseStatus(httpStatus)
                .withResponse(body)
                .build()
    }
}