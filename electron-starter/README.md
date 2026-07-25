# Electron Starter

This folder contains a small Electron wrapper that starts the Spring Boot app from the repository root and shows a splash screen while the backend is coming up.

## Run

1. Install dependencies inside this folder:

   ```bash
   npm install
   ```

2. Start the Electron wrapper:

   ```bash
   npm run start
   ```

The wrapper launches `mvn spring-boot:run` in the repository root and waits for `http://127.0.0.1:8084/` to respond before switching to the app.

## Notes

- The backend URL can be overridden with `KONTOAUSZUEGE_URL`.
- On Windows, `mvn.cmd` is used automatically.
