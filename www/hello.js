/*global cordova, module*/
module.exports = {
	greet: function (name, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, "Hello", "greet", [name]);
	},
	search: function (name, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, "Hello", "search", [name]);
	},
	scan: function (name, successCallback, errorCallback) {
		cordova.exec(successCallback, errorCallback, "Hello", "scan", [name]);
	}
};