var IonicDeploy = {
  download: function(url, success, failure) {
  	cordova.exec(
  		success,
  		failure,
  		'IonicDeploy',
  		'download',
  		[url]
  	);
  },
  setup: function(url, success,failure) {
    cordova.exec(
      success,
      failure,
      'IonicDeploy',
      'setup',
      [url]
    );
  }
}

module.exports = IonicDeploy;
