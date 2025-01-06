(function () {
  const host = 'http://localhost:8020/';

  const modules = [
    '@id/virtual:vue-devtools-path:overlay.js',
    '@id/virtual:vue-inspector-path:load.js',
    '@vite/client',
    'src/main.ts',
  ];

  function createScriptNode(module) {
    const script = document.createElement('script');
    script.src = host + module;
    script.async = false;
    script.type = 'module';
    document.head.appendChild(script);
  }

  mw.loader.using(['@wikimedia/codex']).then(function () {
    modules.forEach(createScriptNode);
  });
})();
