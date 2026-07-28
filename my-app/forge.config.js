const { FusesPlugin } = require('@electron-forge/plugin-fuses');
const { FuseV1Options, FuseVersion } = require('@electron/fuses');

module.exports = {
  packagerConfig: {
    asar: true,
  },
  rebuildConfig: {},
  makers: [
    {
      name: '@electron-forge/maker-deb',
      config: {},
    },
    {
      name: '@electron-forge/maker-flatpak',
      config: {
        id: 'com.example.myapp',
        base: 'org.electronjs.Electron2.BaseApp',
        baseVersion: '25.08',
        runtime: 'org.freedesktop.Platform',
        runtimeVersion: '25.08',
        sdk: 'org.freedesktop.Sdk',
        sdkVersion: '25.08',
        // files to include in the flatpak bundle (optional)
        // files: [ ['path/on/host', '/path/in/flatpak'] ],
        finishArgs: [
          '--share=network',
          '--share=ipc',
          '--socket=wayland',
          '--socket=x11',
          '--socket=session-bus',
          '--socket=pulseaudio',
          '--device=dri',
          '--filesystem=home',
          '--env=TMPDIR=/var/tmp',
          '--talk-name=org.freedesktop.Notifications'
        ],
        // optional extra modules for flatpak-builder
        modules: []
      }
    }
  ],
  plugins: [
    {
      name: '@electron-forge/plugin-auto-unpack-natives',
      config: {},
    },
    // Fuses are used to enable/disable various Electron functionality
    // at package time, before code signing the application
    new FusesPlugin({
      version: FuseVersion.V1,
      [FuseV1Options.RunAsNode]: false,
      [FuseV1Options.EnableCookieEncryption]: true,
      [FuseV1Options.EnableNodeOptionsEnvironmentVariable]: false,
      [FuseV1Options.EnableNodeCliInspectArguments]: false,
      [FuseV1Options.EnableEmbeddedAsarIntegrityValidation]: true,
      [FuseV1Options.OnlyLoadAppFromAsar]: true,
    }),
  ],
};
