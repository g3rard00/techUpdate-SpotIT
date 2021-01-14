package com.bonitasoft.acm.rest.api

interface BPMNamesConstants {
	
	
	def static final PROCESS_NAME = 'CreditCardDisputeResolution'
	def static final LIVINGAPP_TOKEN = 'dispute'
	def static final DASHBOARDPAGE_TOKEN = 'caseList'
	def static final CASEPAGE_TOKEN = 'case'
	

    def static final ACTIVITY_CONTAINER = 'Dynamic Activity Container'
    def static final HIDDEN_ACTIVITIES = [ACTIVITY_CONTAINER]
    def static final ACTIVITY_STATE_DATA_NAME = '$acmState'

    def static final REQUIRED_STATE = 'Required'
    def static final OPTIONAL_STATE = 'Optional'
    def static final DISCRETIONARY_STATE = 'Discretionary'
    def static final NOT_AVAILABLE_STATE = 'N/A'
	}
	