# 🌸🧩 Gracie Patches

<p align="center">🌷 · 💐 · 🪻 · 🌼 · 🌸</p>

> [!NOTE]
>
> *Just some patches I make in my free time, with a little help from AI.*
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
> **[v1.0.26](https://github.com/Ximena-Moonlight/gracie-patches/releases/tag/v1.0.26)**&nbsp;&nbsp;·&nbsp;&nbsp;`main`&nbsp;&nbsp;·&nbsp;&nbsp;**11 patches** across **8 apps**

| # | App | Patches | Version | Package |
|---|-----|---------|---------|---------|
| 1 | **n0widgets** | 2 | `3.3.1` | [`com.n0white.n0widgets`](https://play.google.com/store/apps/details?id=com.n0white.n0widgets) |
| 2 | **Quick Settings** | 2 | `12.0.3-17888438` | [`it.simonesestito.ntiles`](https://play.google.com/store/apps/details?id=it.simonesestito.ntiles) |
| 3 | **Screen Recording App** | 2 | `1.11` | [`de.twokit.screen.recording.app`](https://play.google.com/store/apps/details?id=de.twokit.screen.recording.app) |
| 4 | **Orientation Control** | 1 | `7.10.3` | [`net.mm2d.android.orientationfaker`](https://play.google.com/store/apps/details?id=net.mm2d.android.orientationfaker) |
| 5 | **SpaceX Cloner** | 1 | `1.0.7.9` | [`com.momoerica.cloner.gp`](https://play.google.com/store/apps/details?id=com.momoerica.cloner.gp) |
| 6 | **Aesthetic Widgets** | 1 | `85.1.4` | [`com.remi.widget`](https://play.google.com/store/apps/details?id=com.remi.widget) |
| 7 | **Color Widgets** | 1 | `1.8.5` | [`colorwidgets.ios.widget.topwidgets`](https://play.google.com/store/apps/details?id=colorwidgets.ios.widget.topwidgets) |
| 8 | **ThemePik – Icon Changer** | 1 | `1.1.40` | [`com.eco.iconchanger.themes`](https://play.google.com/store/apps/details?id=com.eco.iconchanger.themes) |
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
