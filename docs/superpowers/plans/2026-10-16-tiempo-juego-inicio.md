# Tiempo de juego restante Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mostrar en Inicio el tiempo de juego que TEO ya calcula y permitir fijar el total en Mi TEO sin alterar volumen, brillo ni el peluche.

**Architecture:** Una función pura lee `playtime_limit_minutes`, `playtime_remaining_seconds` y `playtime_remaining_fraction` del estado. Mi TEO envía solo `playtime_limit_minutes` y confirma el cambio cuando la respuesta repite ese número.

**Tech Stack:** Kotlin, XML, ViewBinding, Fragments, ViewModels, Material Components, JUnit.

## Global Constraints

- El total se escribe a mano, de 0 a 240 minutos. 0 significa sin límite.
- «Aplicar tiempo» envía solo el total.
- Inicio no tiene controles de tiempo.
- Con límite 0 el bloque de Inicio no aparece.
- Sin dato o sin conexión, el valor usa «Sin datos» y la línea no se dibuja.
- Con restante 0 y límite mayor que 0 se muestra `00:00` y la línea vacía.
- Los segundos van con dos dígitos y los minutos con al menos dos.
- La vista previa de Inicio muestra `30:00` y fracción 0,5, sin consultar al peluche.
- El reloj, el bloqueo del botón y el reinicio al prender quedan en el peluche.

---

### Task 1: Lectura del tiempo restante

**Files:**
- Create: `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/PlaytimeReading.kt`
- Test: `ProyectoAndroid/app/src/test/java/com/example/aplicacionparacelular/ui/dashboard/PlaytimeReadingTest.kt`

- [x] Tests de oculto, ausente, `01:30`, `05:00`, `240:00`, `00:00` y fracción recortada.
- [x] Implementar `PlaytimeReading.from` y `formatSeconds`.

### Task 2: Total en Mi TEO

**Files:**
- Modify: `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/myteo/TeoSettingsRules.kt`
- Test: `ProyectoAndroid/app/src/test/java/com/example/aplicacionparacelular/myteo/TeoSettingsRulesTest.kt`
- Modify: `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/network/RobotApiClient.kt`
- Modify: `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/network/RobotConnectionManager.kt`

- [x] El cuerpo del tiempo solo contiene `playtime_limit_minutes`.
- [x] Un número inválido no arma cuerpo.
- [x] La confirmación exige el eco del mismo número.
- [x] El cuerpo de volumen y brillo sigue sin ese campo.

### Task 3: Pantallas

**Files:**
- Modify: `ProyectoAndroid/app/src/main/res/layout/fragment_dashboard.xml`
- Modify: `ProyectoAndroid/app/src/main/res/layout/fragment_teo_settings.xml`
- Modify: `ProyectoAndroid/app/src/main/res/values/strings.xml`
- Modify: `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/HomeScreenRenderer.kt`
- Modify: `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/DashboardFragment.kt`
- Modify: `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/myteo/TeoSettingsViewModel.kt`
- Modify: `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/myteo/TeoSettingsFragment.kt`
- Modify: vistas previas de Inicio y Mi TEO

- [x] Barra y valor en «Hoy con TEO».
- [x] Campo y «Aplicar tiempo» debajo de volumen y brillo.
- [x] La vista previa no consulta ni envía comandos al peluche.

### Task 4: Constitución

**Files:**
- Modify: `.specify/memory/constitution.md`

- [x] El tiempo de uso deja de estar aplazado. Pulsador y agenda sin TEO siguen aplazados.
