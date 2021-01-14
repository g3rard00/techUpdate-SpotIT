package com.bonitasoft.acm.rest.api

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.engine.bpm.flownode.ActivityInstance
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceCriterion
import org.bonitasoft.engine.bpm.flownode.ArchivedHumanTaskInstance
import org.bonitasoft.engine.bpm.flownode.ArchivedHumanTaskInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.flownode.ManualTaskInstance
import org.bonitasoft.engine.bpm.flownode.UserTaskInstance
import org.bonitasoft.engine.bpm.process.ProcessDefinition
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder

import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonBuilder

class CaseActivity implements RestApiController,CaseActivityHelper,BPMNamesConstants {

	private static final String PREFIX = '$'
	private static final String LIVINGAPP_TOKEN = 'cases'

	@Override
	RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
		def caseId = request.getParameter "caseId"
		if (!caseId) {
			return buildResponse(responseBuilder,
					HttpServletResponse.SC_BAD_REQUEST,
					"""{"error" : "the parameter caseId is missing"}""")
        }

        def ProcessAPI processAPI = context.apiClient.getProcessAPI()
        def pDef = processAPI.getProcessDefinition(processAPI.getProcessDefinitionIdFromProcessInstanceId(caseId.toLong()))

        //Retrieve pending tasks for current user
        def userPendingTask = processAPI.getPendingHumanTaskInstances(context.apiSession.userId,0, Integer.MAX_VALUE, ActivityInstanceCriterion.EXPECTED_END_DATE_ASC)
                .findAll{ !HIDDEN_ACTIVITIES.contains(it.name) && it.parentProcessInstanceId == caseId.toLong() }
                .collect{ toActivity(it, getACMStateValue(it,processAPI), pDef, request.contextPath) }


        //Retrieve all case tasks including ManualTasks
        def allCaseTasks = processAPI.searchHumanTaskInstances(new SearchOptionsBuilder(0, Integer.MAX_VALUE).with {
            filter(HumanTaskInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID, caseId.toLong())
            HIDDEN_ACTIVITIES.each {
                differentFrom(ArchivedHumanTaskInstanceSearchDescriptor.NAME, it)
            }
            done()
        }).result
        .collect{ toActivity(it, getACMStateValue(it,processAPI), pDef, request.contextPath) }

        // Check if required task is pending
        def containsPendingRequiredTasks = allCaseTasks.any{ it.acmState == REQUIRED_STATE }

        // Build the proper task list for the current user including ManualTasks
        def result = allCaseTasks.findAll {
            it.name in userPendingTask.name || it.isDynamicTask
        }

        result = result.sort{ a1,a2 -> valueOfState(a1.acmState) <=> valueOfState(a2.acmState) }

        //Append archived tasks
        def identityAPI = context.apiClient.getIdentityAPI()
        result.addAll(processAPI.searchArchivedHumanTasks(new SearchOptionsBuilder(0, Integer.MAX_VALUE).with {
            filter(ArchivedHumanTaskInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID, caseId)
            HIDDEN_ACTIVITIES.each {
                differentFrom(ArchivedHumanTaskInstanceSearchDescriptor.NAME, it)
            }
            done()
        }).result
        .findAll{
            //remove finished loop task instances
            it.parentActivityInstanceId == 0 || !isAnArchivedLoopInstance(it, processAPI)
        }
        .collect{ ArchivedHumanTaskInstance task ->
            def user = identityAPI.getUser(task.executedBy)
            [
                name:task.displayName ?: task.name,
                description:task.description ?: '',
                bpmState:task.state.capitalize(),
                executedBy:"$user.firstName $user.lastName"
            ]
        })

        buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder([
            activities: result,
            canResolveCase: !containsPendingRequiredTasks
        ]).toString())
    }

    def toActivity(HumanTaskInstance task, String acmState, ProcessDefinition pDef, String contextPath) {
        [
            name:task.displayName ?: task.name,
            url: canExecute(acmState) ? forge(pDef.name,pDef.version,task, contextPath) : null,
            description:task.description ?: '',
            target:linkTarget(task),
            bpmState:task.state.capitalize(),
            acmState:acmState,
            isDynamicTask: task instanceof ManualTaskInstance
        ]
    }


    def String forge(String processName,String processVersion,ActivityInstance instance, contextPath) {
        if(instance instanceof UserTaskInstance) {
            "$contextPath/portal/resource/taskInstance/$processName/$processVersion/$instance.name/content/?id=$instance.id&displayConfirmation=false&app=$LIVINGAPP_TOKEN"
        }else if(instance instanceof ManualTaskInstance) {
            "$contextPath/apps/$LIVINGAPP_TOKEN/manualTask?id=$instance.id"
        }
    }

    def String linkTarget(ActivityInstance instance) {
        if(instance instanceof UserTaskInstance) {
            '_self'
        }else if(instance instanceof ManualTaskInstance) {
            '_parent'
        }
    }

    RestApiResponse buildResponse(RestApiResponseBuilder responseBuilder, int httpStatus, Serializable body) {
        return responseBuilder.with {
            withResponseStatus(httpStatus)
            withResponse(body)
            build()
        }
    }


}