function ButtonBarCtrl($scope, $http, $location, $log, $window, localStorageService, modalService) {
  'use strict';

  var vm = this;

  this.action = function action(button) {
    if (button.action === 'Remove from collection') {
      removeFromCollection(button);
      closeModal(button.closeOnSuccess);
    } else if (button.action === 'Add to collection') {
      addToCollection(button);
      closeModal(button.closeOnSuccess);
    } else if (button.action === 'Start process') {
      startProcess(button);
    } else if (button.action === 'Submit task') {
      submitTask(button);
    } else if (button.action === 'Open modal') {
      closeModal(button.closeOnSuccess);
      openModal(button.modalId);
    } else if (button.action === 'Close modal') {
      closeModal(true);
    } else if (button.url) {
      doRequest(button, button.action, button.url);
    }
  };

  function openModal(modalId) {
    modalService.open(modalId);
  }

  function closeModal(shouldClose) {
    if(shouldClose)
      modalService.close();
  }

  function removeFromCollection(button) {
    if (button.collectionToModify) {
      if (!Array.isArray(button.collectionToModify)) {
        throw 'Collection property for widget button should be an array, but was ' + button.collectionToModify;
      }
      var index = -1;
      if (button.collectionPosition === 'First') {
        index = 0;
      } else if (button.collectionPosition === 'Last') {
        index = button.collectionToModify.length - 1;
      } else if (button.collectionPosition === 'Item') {
        index = button.collectionToModify.indexOf(button.removeItem);
      }

      // Only remove element for valid index
      if (index !== -1) {
         button.collectionToModify.splice(index, 1);
      }
    }
  }

  function addToCollection(button) {
    if (!button.collectionToModify) {
      button.collectionToModify = [];
    }
    if (!Array.isArray(button.collectionToModify)) {
      throw 'Collection property for widget button should be an array, but was ' + button.collectionToModify;
    }
    var item = angular.copy(button.valueToAdd);

    if (button.collectionPosition === 'First') {
      button.collectionToModify.unshift(item);
    } else {
     button.collectionToModify.push(item);
    }
  }

  function startProcess(button) {
    var id = getUrlParam('id');
    if (id) {
      var prom = doRequest(button, 'POST', '../API/bpm/process/' + id + '/instantiation', getUserParam()).then(function() {
        localStorageService.delete($window.location.href);
      });

    } else {
      $log.log('Impossible to retrieve the process definition id value from the URL');
    }
  }

  /**
   * Execute a get/post request to an URL
   * It also bind custom data from success|error to a data
   * @return {void}
   */
  function doRequest(button, method, url, params) {
    vm.busy = true;
    var req = {
      method: method,
      url: url,
      data: angular.copy(button.dataToSend),
      params: params
    };
    return $http(req)
      .success(function(data, status) {
        button.dataFromSuccess = data;
        button.responseStatusCode = status;
        button.dataFromError = undefined;
        notifyParentFrame(button, { message: 'success', status: status, dataFromSuccess: data, dataFromError: undefined, responseStatusCode: status});
        if (button.targetUrlOnSuccess && method !== 'GET') {
          redirectIfNeeded(button);
        }
        closeModal(button.closeOnSuccess);
      })
      .error(function(data, status) {
        button.dataFromError = data;
        button.responseStatusCode = status;
        button.dataFromSuccess = undefined;
        notifyParentFrame(button, { message: 'error', status: status, dataFromError: data, dataFromSuccess: undefined, responseStatusCode: status});
      })
      .finally(function() {
        vm.busy = false;
      });
  }

  function redirectIfNeeded(button) {
    var iframeId = $window.frameElement ? $window.frameElement.id : null;
    //Redirect only if we are not in the portal or a living app
    if (!iframeId || iframeId && iframeId.indexOf('bonitaframe') !== 0) {
      $window.location.assign(button.targetUrlOnSuccess);
    }
  }

  function notifyParentFrame(button, additionalProperties) {
    if ($window.parent !== $window.self) {
      var dataToSend = angular.extend({}, button, additionalProperties);
      $window.parent.postMessage(JSON.stringify(dataToSend), '*');
    }
  }

  function getUserParam() {
    var userId = getUrlParam('user');
    if (userId) {
      return { 'user': userId };
    }
    return {};
  }

  /**
   * Extract the param value from a URL query
   * e.g. if param = "id", it extracts the id value in the following cases:
   *  1. http://localhost/bonita/portal/resource/process/ProcName/1.0/content/?id=8880000
   *  2. http://localhost/bonita/portal/resource/process/ProcName/1.0/content/?param=value&id=8880000&locale=en
   *  3. http://localhost/bonita/portal/resource/process/ProcName/1.0/content/?param=value&id=8880000&locale=en#hash=value
   * @returns {id}
   */
  function getUrlParam(param) {
    var paramValue = $location.absUrl().match('[//?&]' + param + '=([^&#]*)($|[&#])');
    if (paramValue) {
      return paramValue[1];
    }
    return '';
  }

  function submitTask(button) {
    var id;
    id = getUrlParam('id');
    if (id) {
      var params = getUserParam();
	    params.assign = button.assign;
        doRequest(button, 'POST', '../API/bpm/userTask/' + getUrlParam('id') + '/execution', params).then(function() {
        localStorageService.delete($window.location.href);
      });
    } else {
      $log.log('Impossible to retrieve the task id value from the URL');
    }
  }
}