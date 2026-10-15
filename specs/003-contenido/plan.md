# Implementation Plan: Contenido, música y cuentos

**Branch**: `001-inicio-base-visual` | **Date**: 2026-10-14 | **Spec**: [spec.md](spec.md)

## Summary

Contenido reemplaza la lista beta de música y cuentos por las pantallas de los bocetos, reutilizando la paleta, la tipografía y los controles de Inicio y Rutinas. La reproducción solo se marca cuando el estado posterior la confirma. La vista previa usa los mismos layouts con datos de ejemplo.

## Technical Context

**Language/Version**: Kotlin, Java 11, minSdk 24

**Primary Dependencies**: AndroidX, Material, ViewBinding, Navigation, LiveData. Sin dependencias nuevas.

**Storage**: Catálogos y archivos viven en TEO. La app no guarda una copia propia.

**Testing**: JUnit del módulo `:app`.

**Target Platform**: Android, Samsung Galaxy S25 como referencia visual.

**Project Type**: Aplicación móvil.

**Performance Goals**: La lectura de archivos no ocurre en el hilo principal.

**Constraints**: Formatos MP3, WAV, OGG, M4A y FLAC. PDF de hasta 8 MB. Título de hasta 80 caracteres. Sin pausa, porcentaje ni cancelación de subida. No desplegar en la Raspberry ni hacer commit.

**Scale/Scope**: Dos pestañas, dos formularios de cuento, confirmaciones y estados de catálogo.

## Constitution Check

El bloque se entrega completo, conserva Inicio y Rutinas, y no cambia el protocolo del peluche. La vista previa no consulta a TEO ni escribe datos reales.

## Mapa de composición

Encabezado igual al de Rutinas: logo y título a la izquierda, Avisos y Mi TEO a la derecha. Debajo, dos pestañas de ancho compartido; la elegida lleva texto petróleo y una línea inferior. El subtítulo va centrado y el botón Agregar ocupa el ancho, como acción principal.

Música: una tarjeta de envío solo mientras dura la subida, con indicador indeterminado y el nombre; luego «Sonando en TEO» en una fila (icono, nombre que envuelve, Detener); después una sola tarjeta de catálogo con filas al alto del nombre. La pieza activa muestra «En reproducción» en lugar de Reproducir. Eliminar queda como acción secundaria.

Cuentos: «Lectura en TEO» en una tarjeta y, debajo, una tarjeta por cuento con título, palabras y tres acciones. No se copia la distribución de Inicio.

Agregar cuento y Editar cuento ocupan la pantalla, sin navegación inferior. El alta muestra el PDF, Cambiar, título, ayuda y viñetas. La edición precarga título y un texto multilínea; Guardar y Cancelar quedan dentro del desplazamiento. Los diálogos de eliminación se centran sobre el listado real y nombran el elemento.

Las medidas salen del contenido y de los tokens ya validados, no de los píxeles del PNG.

## Project Structure

### Source Code

```text
ProyectoAndroid/app/src/main/java/.../ui/stories/
ProyectoAndroid/app/src/main/res/layout/fragment_stories.xml
ProyectoAndroid/app/src/main/res/layout/fragment_story_form.xml
ProyectoAndroid/app/src/main/res/layout/item_music_row.xml
ProyectoAndroid/app/src/main/res/layout/item_story_card.xml
ProyectoAndroid/app/src/debug/java/.../ui/preview/ContentPreviewHost.kt
```

## Contratos que se reutilizan

- `GET/POST /api/music`, `POST /api/music/play`, `POST /api/music/stop`, `DELETE /api/music/{nombre}`, `POST /api/music/upload`.
- `GET/POST /api/stories`, `GET/PUT/DELETE /api/stories/{id}`, `POST /api/stories/play`, `POST /api/stories/stop`, `POST /api/stories/upload`.
- El estado de reproducción se lee de `currently_playing` y `currently_reading` como ya hace Inicio.
- Un cuerpo con sueño o estado de error no confirma la acción aunque la llamada haya respondido.
