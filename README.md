# 🌸 Gracie Patches

<p align="center">🌷 · 💐 · 🪻 · 🌼 · 🌸</p>

> [!NOTE]
>
> *Just some patches I make in my free time, with help from AI.*
>
> Personal project. Not affiliated with the official Morphe team.

This is my personal collection of patches that I use with **[Morphe](https://morphe.software)**.  
Hobby project for learning and for the apps I use every day.

## How to use these patches

Tap the button, or add the link by hand in Morphe Manager:

<a href="https://morphe.software/add-source?github=Ximena-Moonlight/gracie-patches">
  <img src=".github/assets/add-to-morphe.svg" alt="Add to Morphe" height="48">
</a>

```
https://github.com/Ximena-Moonlight/gracie-patches
```

## 🩹 Patches list

<!-- PATCHES_START -->
> **[v1.0.8](https://github.com/Sabrina-Moonlight/gracie-patches/releases/tag/v1.0.8)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;7 patches total
<details open>
<summary>🌸 Orientation Control&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**Package name:** `net.mm2d.android.orientationfaker`

**🌻 Supported versions:**

| 7.10.3 |
| :---: |

| 🌷&nbsp;Patch | 💐&nbsp;Description | 🪻&nbsp;Options |
|----------|----------------|-----------|
| [Disable Ads](#disable-ads) | Removes ads from Orientation Control. |  |

</details>

<details open>
<summary>🌸 Quick Settings&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

**Package name:** `it.simonesestito.ntiles`

**🌻 Supported versions:**

| 12.0.3-17888438 |
| :---: |

| 🌷&nbsp;Patch | 💐&nbsp;Description | 🪻&nbsp;Options |
|----------|----------------|-----------|
| [Disable Pairip](#disable-pairip) | Skips the local PairIP license exit check so the app can keep running. |  |
| [Hide Ads Preference](#hide-ads-preference) | Uses a local preference so interstitial and banner ads stay off. |  |

</details>

<details open>
<summary>🌸 Screen Recording App&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

**Package name:** `de.twokit.screen.recording.app`

**🌻 Supported versions:**

| 1.11 |
| :---: |

| 🌷&nbsp;Patch | 💐&nbsp;Description | 🪻&nbsp;Options |
|----------|----------------|-----------|
| [Disable Pairip](#disable-pairip) | Skips the local Pairip license check so the app can keep running. |  |
| [Pro Options](#pro-options) | Uses a local preference so optional Pro options stay available. |  |

</details>

<details open>
<summary>🌸 Aesthetic Widgets&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**Package name:** `com.remi.widget`

**🌻 Supported versions:**

| 85.1.4 |
| :---: |

| 🌷&nbsp;Patch | 💐&nbsp;Description | 🪻&nbsp;Options |
|----------|----------------|-----------|
| [Local Features](#local-features) | Uses a local preference so optional widget features stay available offline. |  |

</details>

<details open>
<summary>🌼 Universal&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

| 🌷&nbsp;Patch | 💐&nbsp;Description | 🪻&nbsp;Options |
|----------|----------------|-----------|
| [Clone app](#clone-app) | Changes the app package name and label so you can install several clones. Pick a clone number (1–50). Each number is a different package (app.clone1, app.clone2…). On update, re-apply the same number on each clone. | • Clone number<br>• Custom package (optional)<br>• Update permissions<br>• Update providers |

</details>

<!-- PATCHES_END -->

## How these patches are made

With help from an AI, following the rules in [`AGENTS.md`](AGENTS.md) and the technical guide [`Morphe-Development-Guide.md`](Morphe-Development-Guide.md).

Typical flow:
1. The AI delivers the changed files.
2. They are uploaded together to `Drop-Files/` on GitHub.
3. **Sort Uploads** places them, then **Release** builds and publishes on its own.

## Building (usually not needed)

The **Release** workflow builds everything on its own. Manual steps only if needed:
- `./gradlew buildAndroid`
- The `.mpp` ends up in `patches/build/libs/patches-*.mpp`

See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for more details.

## License

Gracie Patches is licensed under the [GNU General Public License v3.0](LICENSE). See also [NOTICE](NOTICE).

<p align="center">🌸 · 💐 · 🪻 · 🌼 · 🌷</p>
