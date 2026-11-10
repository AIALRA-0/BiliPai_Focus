# Telegram / NagramX ThanosEffect port

The video-dismissal particle renderer uses the upstream GLES3 transform-feedback
implementation, replacing BiliPai's former GLES2 approximation. Sources were
checked on 2026-10-01 and pinned to these revisions:

- [NagramX `2db685af00a4352c877ecf96474cbf0494284715`](https://github.com/risin42/NagramX/tree/2db685af00a4352c877ecf96474cbf0494284715), `dev`.
- [Telegram `f2908b14133bbffbf7ab04f641ecb5bfaf533242`](https://github.com/DrKLO/Telegram/tree/f2908b14133bbffbf7ab04f641ecb5bfaf533242), `master`.

The two upstream `thanos_vertex.glsl` and `thanos_fragment.glsl` files are
byte-identical. BiliPai includes them unchanged in `app/src/main/res/raw/`.
Rendering and particle-grid code is ported from
[`ThanosEffect.java`](https://github.com/risin42/NagramX/blob/2db685af00a4352c877ecf96474cbf0494284715/TMessagesProj/src/main/java/org/telegram/ui/Components/ThanosEffect.java).
Upstream code is credited to Telegram and the NagramX contributors; the imported
shaders and port are distributed under GPL, consistent with this repository's
[GPL-3.0 license](../LICENSE). See upstream
[NagramX license](https://github.com/risin42/NagramX/blob/2db685af00a4352c877ecf96474cbf0494284715/LICENSE)
and [Telegram license](https://github.com/DrKLO/Telegram/blob/f2908b14133bbffbf7ab04f641ecb5bfaf533242/LICENSE).

## Preserved rendering behavior

- OpenGL ES 3 context, RGBA transparent `TextureView`, independent EGL thread.
- Original vertex and fragment shaders, random seed, texture filtering/wrapping,
  premultiplied texture colors, and disabled GL blending.
- Interleaved `outUV`, `outPosition`, `outVelocity`, `outTime` feedback: seven floats,
  28-byte stride, two GPU buffers swapped after every draw.
- Original rectangular grid calculation with 30,000 / 60,000 / 120,000 particle
  budgets. The grid rounds up to complete rows/columns as upstream does.
- Original per-particle lifetime, left-to-right activation, acceleration, fading,
  and sampling each particle's actual patch of the card texture.
- Normal animation parameters: `longevity = 1.5`, `timeScale = 1.15`, tail `0.9`.
  Completion occurs after approximately 2.09 seconds, not the former 0.82-second
  cutoff. First draw uses zero delta. Both legacy card presets share this timing;
  they only retain different list-collapse specifications.
- Frame scheduling follows `Choreographer`; there is no CPU particle simulation.

## Application integration

Telegram chat cells, grouped messages, avatars, and photo-editor branches depend
on Telegram-specific models and are outside the video-card integration. Their
snapshot boundary is replaced with a Compose `GraphicsLayer` capture. This
preserves transparent card corners and excludes the window background and
action sheet. The GL thread owns a separate software bitmap copy.

The now-playing bar instead uses window `PixelCopy` cropped to its window bounds,
with transparent capsule corners restored on the software bitmap. This preserves
its particle effect and visible blur while avoiding an extra offscreen HWUI draw
of the live backdrop and rotating artwork. A cancelled or timed-out copy keeps
its destination alive until the PixelCopy callback finishes.

The overlay attaches to the calling window's decor, using the card's window
coordinates. It occupies the full window, allowing dust to drift outside the
card bounds. The source remains visible until `TextureView` reports acquisition
of the first swapped frame; only then is it hidden. Existing list-collapse and
business-removal callbacks run after the effect completes.

Android `EGL14` and `HandlerThread` replace upstream EGL10 and `DispatchQueue`.
Device RAM class replaces Telegram `SharedConfig` performance classification;
the three upstream particle budgets remain unchanged. The calling window and
Compose effect own cancellation and cleanup. GL buffers, transform-feedback
object, texture, shaders, program, EGL context/surface, snapshot, and thread are
released on completion, failure, surface destruction, or composition disposal.

The old Xiaomi/Android-13 exclusion applied to the removed `GLSurfaceView`
implementation. History now checks GLES3 capability. Unsupported contexts and
recoverable rendering errors finish the requested removal without particles.
A five-second UI watchdog also handles missing texture callbacks. Batch history
deletion dissolves the selected cards in the current viewport concurrently, keeps
completed card slots transparent without collapsing the grid, and removes all
selected records together after completion. Offscreen selections do not wait for
animation callbacks. A six-second session fallback covers navigation or viewport
changes before a selected lazy row mounts.

Home “not interested” closes the action menu, dissolves the card, and opens the
reason sheet after reflow has started. Reason submission does not start a second
animation; dismissing the sheet records video-only feedback. Reduced motion or
unavailable GLES3 skips the particle stage. A six-second fallback covers lazy
cards that leave composition before starting. Home cards do not publish global
dissolve state or run the old continuous rotation/translation jiggle. During the
final 180 ms of the actual renderer timeline, home starts a 240 ms ease-out card
collapse without truncating particle rendering. Other visible cards retain their
visual positions across aligned-row regrouping and ease to their new slots over
240 ms. Gesture scrolling bypasses these compensating transforms. The existing
modal sheet presentation starts 180 ms after reflow begins; reduced-motion and
unavailable-GL paths show the reasons directly. History retains its separate
single/batch collapse policy.

## Verification

Static checks cover upstream shader identity, feedback order/stride, resource and
uniform wiring, particle-grid calculation, lifecycle cleanup paths, and interaction
entry points. Policy regression tests cover GLES capability, grid budgets/aspect,
small snapshots, and completion tail. Android API signatures were inspected from
the installed SDK.

Per the user's instruction, no Gradle/Kotlin/shader compilation, test execution,
or device validation was performed. Runtime parity, TextureView composition,
and device-driver behavior still require a separately authorized build and
on-device check.
