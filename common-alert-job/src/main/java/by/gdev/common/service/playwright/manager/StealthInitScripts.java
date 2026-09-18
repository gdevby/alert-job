package by.gdev.common.service.playwright.manager;

/**
 * Init-script для снижения детекта automation в Chromium (PlaywrightManager).
 */
public final class StealthInitScripts {

    private StealthInitScripts() {
    }

    public static final String CONTEXT_SCRIPT =
            "(() => {" +
                    "  Object.defineProperty(navigator, 'webdriver', { get: () => undefined });" +
                    "  delete window.cdc_adoQpoasnfa76pfcZLmcfl_Array;" +
                    "  delete window.cdc_adoQpoasnfa76pfcZLmcfl_Promise;" +
                    "  delete window.cdc_adoQpoasnfa76pfcZLmcfl_Symbol;" +
                    "  if (!window.chrome) window.chrome = {};" +
                    "  window.chrome.runtime = window.chrome.runtime || {};" +
                    "  window.chrome.app = { isInstalled: false, InstallState: { DISABLED: 'disabled', INSTALLED: 'installed', NOT_INSTALLED: 'not_installed' }, RunningState: { CANNOT_RUN: 'cannot_run', READY_TO_RUN: 'ready_to_run', RUNNING: 'running' } };" +
                    "  window.chrome.webstore = { onInstallStageChanged: {}, onDownloadProgress: {} };" +
                    "  window.chrome.csi = function() { return { onloadT: Date.now(), startE: Date.now(), pageT: 100, tran: 15 }; };" +
                    "  const makePlugin = (name, filename, desc, mimes) => {" +
                    "    const plugin = Object.create(Plugin.prototype);" +
                    "    Object.defineProperties(plugin, { name: { value: name }, filename: { value: filename }, description: { value: desc }, length: { value: mimes.length } });" +
                    "    mimes.forEach((m, i) => { plugin[i] = m; });" +
                    "    return plugin;" +
                    "  };" +
                    "  const makeMime = (type, suffixes, desc) => {" +
                    "    const mime = Object.create(MimeType.prototype);" +
                    "    Object.defineProperties(mime, { type: { value: type }, suffixes: { value: suffixes }, description: { value: desc } });" +
                    "    return mime;" +
                    "  };" +
                    "  const pdfMime = makeMime('application/pdf', 'pdf', 'Portable Document Format');" +
                    "  const naclMime = makeMime('application/x-nacl', '', 'Native Client Executable');" +
                    "  const pnaclMime = makeMime('application/x-pnacl', '', 'Portable Native Client Executable');" +
                    "  const pluginsArr = [" +
                    "    makePlugin('PDF Viewer', 'internal-pdf-viewer', 'Portable Document Format', [pdfMime])," +
                    "    makePlugin('Chrome PDF Viewer', 'internal-pdf-viewer', 'Portable Document Format', [pdfMime])," +
                    "    makePlugin('Chromium PDF Viewer', 'internal-pdf-viewer', 'Portable Document Format', [pdfMime])," +
                    "    makePlugin('Microsoft Edge PDF Viewer', 'internal-pdf-viewer', 'Portable Document Format', [pdfMime])," +
                    "    makePlugin('WebKit built-in PDF', 'internal-pdf-viewer', 'Portable Document Format', [pdfMime])," +
                    "    makePlugin('Native Client', 'internal-nacl-plugin', '', [naclMime, pnaclMime])" +
                    "  ];" +
                    "  Object.defineProperty(navigator, 'plugins', {" +
                    "    get: () => {" +
                    "      const arr = [...pluginsArr];" +
                    "      arr.item = i => arr[i];" +
                    "      arr.namedItem = n => arr.find(p => p.name === n);" +
                    "      arr.refresh = () => {};" +
                    "      return arr;" +
                    "    }" +
                    "  });" +
                    "  Object.defineProperty(navigator, 'mimeTypes', {" +
                    "    get: () => {" +
                    "      const arr = [pdfMime, naclMime, pnaclMime];" +
                    "      arr.item = i => arr[i];" +
                    "      arr.namedItem = n => arr.find(m => m.type === n);" +
                    "      return arr;" +
                    "    }" +
                    "  });" +
                    "  Object.defineProperty(navigator, 'languages', { get: () => ['ru-RU', 'ru', 'en-US', 'en'] });" +
                    "  Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 8 });" +
                    "  Object.defineProperty(navigator, 'deviceMemory', { get: () => 8 });" +
                    "  Object.defineProperty(navigator, 'maxTouchPoints', { get: () => 0 });" +
                    "  Object.defineProperty(navigator, 'platform', { get: () => 'Win32' });" +
                    "  Object.defineProperty(navigator, 'vendor', { get: () => 'Google Inc.' });" +
                    "  Object.defineProperty(navigator, 'productSub', { get: () => '20030107' });" +
                    "  const patchGL = (proto) => {" +
                    "    if (!proto) return;" +
                    "    const orig = proto.getParameter;" +
                    "    proto.getParameter = function(p) {" +
                    "      if (p === 37445) return 'Intel Inc.';" +
                    "      if (p === 37446) return 'Intel Iris OpenGL Engine';" +
                    "      return orig.apply(this, arguments);" +
                    "    };" +
                    "  };" +
                    "  patchGL(window.WebGLRenderingContext && WebGLRenderingContext.prototype);" +
                    "  patchGL(window.WebGL2RenderingContext && WebGL2RenderingContext.prototype);" +
                    "  const origToDataURL = HTMLCanvasElement.prototype.toDataURL;" +
                    "  HTMLCanvasElement.prototype.toDataURL = function() {" +
                    "    const ctx = this.getContext('2d');" +
                    "    if (ctx) {" +
                    "      const img = ctx.getImageData(0, 0, this.width, this.height);" +
                    "      for (let i = 0; i < img.data.length; i += 1000) img.data[i] ^= 1;" +
                    "      ctx.putImageData(img, 0, 0);" +
                    "    }" +
                    "    return origToDataURL.apply(this, arguments);" +
                    "  };" +
                    "  Object.defineProperty(navigator, 'connection', { get: () => ({ downlink: 10, effectiveType: '4g', rtt: 50, saveData: false, onchange: null }) });" +
                    "  const origQuery = window.navigator.permissions && window.navigator.permissions.query;" +
                    "  if (origQuery) {" +
                    "    window.navigator.permissions.query = (params) => (" +
                    "      params.name === 'notifications'" +
                    "        ? Promise.resolve({ state: Notification.permission })" +
                    "        : origQuery(params)" +
                    "    );" +
                    "  }" +
                    "  const vw = 1366, vh = 768;" +
                    "  Object.defineProperty(window, 'outerWidth', { get: () => vw });" +
                    "  Object.defineProperty(window, 'outerHeight', { get: () => vh + 88 });" +
                    "  Object.defineProperty(window, 'innerWidth', { get: () => vw });" +
                    "  Object.defineProperty(window, 'innerHeight', { get: () => vh });" +
                    "  Error.prepareStackTrace = (err, stack) => stack;" +
                    "})();";
}
