;(function(config) {
  if (config.devServer) {
    config.devServer.proxy = [{
      context: ['/ollama'],
      target: process.env.THOON_OLLAMA || 'http://localhost:11434',
      pathRewrite: { '^/ollama': '' },
      changeOrigin: true,
      // Ollama answers 403 to a browser Origin outside OLLAMA_ORIGINS.
      on: { proxyReq: (proxyReq) => proxyReq.removeHeader('origin') },
    }]
  }
})(config);
