const { app, BrowserWindow, shell } = require('electron');
const { spawn } = require('child_process');
const fs = require('fs');
const http = require('http');
const path = require('path');

const REPO_ROOT = path.resolve(__dirname, '..');
const BACKEND_URL = process.env.KONTOAUSZUEGE_URL || 'http://127.0.0.1:8084';
const BACKEND_ENTRY = `${BACKEND_URL.replace(/\/$/, '')}/`;
const START_COMMAND = process.platform === 'win32' ? 'mvn.cmd' : 'mvn';

let splashWindow;
let mainWindow;
let backendProcess;
let shuttingDown = false;

function createSplashWindow() {
  splashWindow = new BrowserWindow({
    width: 620,
    height: 460,
    resizable: false,
    minimizable: false,
    maximizable: false,
    show: true,
    title: 'Kontoauszüge starten',
    backgroundColor: '#0f172a',
    webPreferences: {
      contextIsolation: false,
      nodeIntegration: true
    }
  });

  splashWindow.removeMenu();
  splashWindow.loadFile(path.join(__dirname, 'splash.html'));
  splashWindow.on('closed', () => {
    splashWindow = null;
  });
}

function createMainWindow() {
  mainWindow = new BrowserWindow({
    width: 1440,
    height: 960,
    show: true,
    backgroundColor: '#ffffff',
    title: 'Kontoauszüge App',
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
      webSecurity: true
    }
  });

  mainWindow.removeMenu();
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith(BACKEND_URL)) {
      return { action: 'allow' };
    }

    shell.openExternal(url);
    return { action: 'deny' };
  });


  mainWindow.loadURL(BACKEND_ENTRY, {
    userAgent: 'Kontoauszüge Electron App'
  });
  /*
  mainWindow.loadFile(path.join(__dirname, 'main.html'), {
    query: {
      target: BACKEND_ENTRY
    }
  });
*/
  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

function startBackend() {
  if (backendProcess) {
    return backendProcess;
  }
  // If a runnable JAR exists in target/, start it with `java -jar`.
  let cmd = START_COMMAND;
  let args = ['spring-boot:run'];
  try {
    // Search directories in order of preference. When packaged, the JAR
    // should live in the app's resources under `spring-boot-app`.
    const resourceDir = path.join(process.resourcesPath || REPO_ROOT, 'spring-boot-app');
    const targetDir = path.join(REPO_ROOT, 'target');
    const searchDirs = [resourceDir, targetDir];

    let jarPath = null;
    let jarDir = null;

    for (const dir of searchDirs) {
      try {
        if (!fs.existsSync(dir)) continue;
        const files = fs.readdirSync(dir).filter(f => f.endsWith('.jar'));
        const candidates = files.filter(f => !/\.original$/.test(f) && !/\.jar\.original$/.test(f));
        const jars = candidates.length ? candidates : files;
        if (jars.length) {
          jars.sort((a, b) => {
            const aStat = fs.statSync(path.join(dir, a));
            const bStat = fs.statSync(path.join(dir, b));
            return bStat.mtimeMs - aStat.mtimeMs;
          });
          jarPath = path.join(dir, jars[0]);
          jarDir = dir;
          break;
        }
      } catch (e) {
        // ignore and continue searching
        continue;
      }
    }

    if (!jarPath) {
      const err = new Error(`No runnable JAR found in any of: ${searchDirs.join(', ')}`);
      console.error(err.message);
      if (splashWindow) {
        splashWindow.webContents.send('startup-error', { message: err.message });
      }
      throw err;
    }

    cmd = 'java';
    // Pass -no-browser-start to Spring Boot so the backend doesn't open a browser
    args = ['-jar', jarPath, '-no-browser-start'];
    console.log('Starting backend from JAR:', jarPath, 'with -no-browser-start');

    backendProcess = spawn(cmd, args, {
      cwd: jarDir || REPO_ROOT,
      // Use a shell on Windows to ensure .cmd/.bat resolution works reliably
      shell: process.platform === 'win32',
      /*windowsHide: true,*/
      windowsHide: false,
      env: {
        ...process.env,
        SPRING_PROFILES_ACTIVE: process.env.SPRING_PROFILES_ACTIVE || 'default'
      }
    });
  } catch (err) {
    console.error('Failed to spawn backend process:', err);
    if (splashWindow) {
      splashWindow.webContents.send('startup-error', { message: err.message });
    }
    throw err;
  }

  backendProcess.stdout.on('data', (chunk) => {
    process.stdout.write(chunk);
  });

  backendProcess.stderr.on('data', (chunk) => {
    process.stderr.write(chunk);
  });

  backendProcess.on('exit', (code, signal) => {
    backendProcess = null;
    if (!shuttingDown) {
      console.log(`Spring Boot process exited (${code ?? signal})`);
      if (mainWindow) {
        mainWindow.webContents.send('backend-exited', {
          code,
          signal
        });
      }
    }
  });

  // Handle spawn errors (emitted asynchronously)
  backendProcess.on('error', (err) => {
    console.error('Backend process error:', err);
    if (splashWindow) {
      splashWindow.webContents.send('startup-error', { message: err.message });
    }
    backendProcess = null;
  });

  return backendProcess;
}

function waitForBackend(url, timeoutMs = 180000) {
  const startedAt = Date.now();

  return new Promise((resolve, reject) => {
    const poll = () => {
      const request = http.get(url, (response) => {
        response.resume();
        if (response.statusCode && response.statusCode < 500) {
          resolve(true);
          return;
        }

        if (Date.now() - startedAt > timeoutMs) {
          reject(new Error(`Backend did not become ready within ${timeoutMs} ms`));
          return;
        }

        setTimeout(poll, 1000);
      });

      request.on('error', () => {
        if (Date.now() - startedAt > timeoutMs) {
          reject(new Error(`Backend did not become ready within ${timeoutMs} ms`));
          return;
        }

        setTimeout(poll, 1000);
      });

      request.setTimeout(3000, () => {
        request.destroy(new Error('Backend readiness check timed out'));
      });
    };

    poll();
  });
}

function shutdownBackend() {
  shuttingDown = true;

  if (backendProcess) {
    if (process.platform === 'win32') {
      spawn('taskkill', ['/pid', String(backendProcess.pid), '/T', '/F'], {
        shell: false,
        windowsHide: true,
        stdio: 'ignore'
      });
    } else {
      backendProcess.kill('SIGTERM');
    }
    backendProcess = null;
  }
}

async function bootstrap() {
  createSplashWindow();
  try {
    startBackend();
  } catch (err) {
    console.error('startBackend failed:', err);
    if (splashWindow) {
      splashWindow.webContents.send('startup-error', { message: err.message });
    }
    return;
  }

  try {
    await waitForBackend(BACKEND_ENTRY);
  } catch (error) {
    if (splashWindow) {
      splashWindow.webContents.send('startup-error', {
        message: error.message
      });
    }
    console.error(error);
    return;
  }

  if (splashWindow) {
    splashWindow.close();
  }

  createMainWindow();
}

app.whenReady().then(bootstrap);

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    shutdownBackend();
    app.quit();
  }
});

app.on('before-quit', () => {
  shutdownBackend();
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    bootstrap();
  }
});
