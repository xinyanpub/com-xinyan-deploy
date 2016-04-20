var Deploy = {
  download: function(url, success, failure) {
  	cordova.exec(
  		success,
  		failure,
  		'Deploy',
  		'download',
  		[url]
  	);
  },
  setup: function(url, success,failure) {
    cordova.exec(
      success,
      failure,
      'Deploy',
      'setup',
      [url]
    );
  }
}

module.exports = Deploy;
