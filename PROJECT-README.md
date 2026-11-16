# MR SHADOW DEVICE INFO project source

This Gradle Android project is packaged by `.github/workflows/package-project.yml` as a downloadable `shadow.zip` Actions artifact. Download that artifact, extract the outer artifact archive, then upload the contained `shadow.zip` to the repository root to trigger `.github/workflows/.build.yml`.

The app has no internet permission, backend or bot integration. `app/src/main/assets/apps.json` is intentionally empty until real app entries and URLs are provided.
