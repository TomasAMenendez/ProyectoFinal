# Implementation Plan: Rutinas

**Branch**: `001-inicio-base-visual` | **Date**: 2026-10-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-rutinas/spec.md`

## Summary

Reemplazar el listado beta y sus diálogos de alta y edición por las pantallas del boceto, conservando el modelo, la API y los recordatorios. La vista previa debug recorre las mismas pantallas con ejemplos en memoria.

## Technical Context

**Language/Version**: Kotlin, Java 11

**Primary Dependencies**: XML, ViewBinding, Fragments, ViewModels, Material Components, Navigation

**Storage**: El peluche sigue guardando `routines_config.json` por `POST /api/routines`. La vista previa no escribe ese almacenamiento ni programa alarmas.

**Testing**: JUnit del borrador, la frase automática y las marcas visibles

**Target Platform**: App Android existente, minSdk 24, validación prevista en Samsung Galaxy S25

**Project Type**: Aplicación móvil

**Performance Goals**: El listado se desplaza con las rutinas que ya devuelve la API

**Constraints**: No modificar el servidor ni el peluche. No agregar anticipación ni transición. No cambiar Inicio. No crear rama ni commit.

**Scale/Scope**: Listado, formulario compartido de alta y edición, diálogo de eliminación y recorrido debug

## Constitution Check

- I. Se conservan `GET/POST /api/routines`, el modelo y `ReminderScheduler`.
- II. Sigue la base Kotlin/XML/ViewBinding/Fragments/ViewModels/Material.
- III. El boceto de Rutinas define columnas y formulario. La guía aporta colores, áreas táctiles e insets, y se ajusta donde el boceto es más compacto.
- IV. Logo, paleta, iconos y barra inferior salen de Inicio. No se toca su layout.
- V. Completada, Recordada y errores salen de datos o de la respuesta. No hay chip de sincronización inventado.
- VI. Texto en sp, controles de al menos 48 dp, formulario desplazable e insets.
- VII. Anticipación, transición, tiempo de uso, pulsador y agenda sin TEO quedan afuera.
- VIII. El bloque se especifica, planifica e implementa en `specs/002-rutinas` sin reabrir `specs/001-inicio-base-visual`.
- IX. Compilación y pruebas no sustituyen la comparación visual. Si el teléfono está bloqueado, la captura queda pendiente.

## Pictograma

El id vive en la rutina y en el evento `main` que devuelve el scheduler. `SpeechWorker.speak` avisa el inicio real de esa reproducción y su cancelación. El renderer compone el PNG contenido en el lienzo de la LCD y del escritorio; el preaviso no lleva imagen. Los archivos están en `res/drawable-nodpi` y en `ProyectoParaRasperrypiV5/assets/pictograms`. No se leen desde `docs` al ejecutar.

## Mapa de composición

Listado, de arriba hacia abajo: encabezado de Inicio con título Rutinas; subtítulo documentado; botón Agregar rutina alineado a la derecha; tarjetas en fila hora | texto | interruptor; Editar y Eliminar al pie derecho; barra inferior con Rutinas seleccionada.

Formulario: regreso y título, subtítulo, tarjeta con nombre, hora y mensaje de varias líneas, ayuda, aclaración solo en el alta, Guardar y Cancelar. Sin barra inferior.

Eliminar: diálogo blanco centrado, título, pregunta con el nombre entre comillas angulares, Cancelar en texto y Eliminar en rojo, sobre el listado atenuado.

Diferencia con la beta: la beta apila hora y nombre en una fila pobre, edita en un diálogo armado en código y esconde Eliminar en una pulsación larga. Eso se reemplaza.

## Project Structure

### Source Code

```text
ProyectoAndroid/app/src/main/java/.../ui/routines/
ProyectoAndroid/app/src/main/res/layout/fragment_routines.xml
ProyectoAndroid/app/src/main/res/layout/item_routine.xml
ProyectoAndroid/app/src/main/res/layout/fragment_routine_editor.xml
ProyectoAndroid/app/src/main/res/layout/dialog_delete_routine.xml
ProyectoAndroid/app/src/debug/java/.../ui/preview/
ProyectoAndroid/app/src/test/java/.../ui/routines/
```

**Structure Decision**: Un destino de listado y otro de formulario en la navegación actual. El diálogo no es un destino. La vista previa infla esos layouts sin el grafo real.

## Post-Design Constitution Check

Sin violaciones. El formulario nuevo no cambia el cuerpo enviado al peluche: identidad, activación, anticipación y transición se conservan aunque no se editen.
