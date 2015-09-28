var pubnubPushNotification = {
  register: function(successCallback, errorCallback, options) {
    cordova.exec(
      successCallback,
      errorCallback,
      'PubnubPushNotification',
      'register',
      [options]
    );
  },
  unregister: function (successCB, errorCB) {
    cordova.exec(
      successCB,
      errorCB,
      "PubnubPushNotification",
      "unregister",
      []
    );
  },
  setApplicationIconBadgeNumber: function (options) {
    cordova.exec(
      function(resp){},
      function(resp){},
      "PubnubPushNotification",
      "setApplicationIconBadgeNumber",
      [options]
    );
  },
  cancelNotification: function (successCallback, errorCallback) {
    cordova.exec(
      successCallback,
      errorCallback,
      "PubnubPushNotification",
      "cancelNotification",
      []
    );
  },
  onNotification: function(successCallback, errorCallback) {
    cordova.exec(
      successCallback,
      errorCallback,
      "PubnubPushNotification",
      "onNotification",
      []
    );
  }
};
module.exports = pubnubPushNotification;
