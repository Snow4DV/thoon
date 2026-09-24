;(function(config) {
  if (config.devServer && process.env.THOON_HTTPS) {
    config.devServer.server = 'https'
    config.devServer.host = '0.0.0.0'
  }
})(config);
