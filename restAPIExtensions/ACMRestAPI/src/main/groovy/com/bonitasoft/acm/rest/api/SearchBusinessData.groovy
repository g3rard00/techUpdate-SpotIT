package com.bonitasoft.acm.rest.api

import org.bonitasoft.engine.bdm.dao.BusinessObjectDAO
import org.bonitasoft.engine.bpm.process.ProcessInstanceNotFoundException

import com.bonitasoft.engine.api.ProcessAPI

class SearchBusinessData {

	private ProcessAPI processAPI;

	SearchBusinessData(ProcessAPI processAPI){
		this.processAPI = processAPI;
	}

	def search(long caseId, String dataRef, BusinessObjectDAO dao) {
		def processInstanceContext
		try {
			processInstanceContext = processAPI.getProcessInstanceExecutionContext(caseId)
		}catch(ProcessInstanceNotFoundException e) {
			processInstanceContext = processAPI.getArchivedProcessInstanceExecutionContext(caseId)
		}
		if(processInstanceContext) {
			def ref = processInstanceContext[dataRef]
			if(ref) {
				return dao.findByPersistenceId(ref.storageId)
			}
		}
		return null
	}
}