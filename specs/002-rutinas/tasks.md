# Tasks: Rutinas

**Input**: Design documents from `specs/002-rutinas/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/routines-ui.md, quickstart.md

## Phase 1

- [x] T001 Crear el borrador puro y sus pruebas en `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/routines/RoutineDraft.kt` y `ProyectoAndroid/app/src/test/java/com/example/aplicacionparacelular/ui/routines/RoutineDraftTest.kt`

## Phase 2

- [x] T002 Reemplazar el listado beta por `fragment_routines.xml` e `item_routine.xml`, con encabezado, subtítulo documentado y botón Agregar rutina
- [x] T003 Mostrar hora, nombre, mensaje, interruptor, Activada/Desactivada, Completada/Recordada solo con datos, y Editar/Eliminar en `RoutineAdapter`

## Phase 3

- [x] T004 Agregar `fragment_routine_editor.xml` y `RoutineEditorFragment` para alta y edición, con validación, frase automática y conservación de campos
- [x] T005 Hacer que `RoutinesViewModel` revierta la lista y informe el error si `POST /api/routines` falla, sin anunciar sincronización exitosa

## Phase 4

- [x] T006 Mostrar `dialog_delete_routine.xml` centrado sobre el listado, con el nombre y Cancelar/Eliminar

## Phase 5

- [x] T007 Extender la vista previa debug para recorrer listado, alta, edición y eliminación sin API, recordatorios ni almacenamiento real
- [x] T008 Aplicar el tema claro a Rutinas y ocultar la barra inferior solo en el formulario, sin cambiar el layout de Inicio
- [x] T009 Compilar, ejecutar pruebas e instalar en el S25 si está desbloqueado; capturar y corregir diferencias visibles. Dejar pendiente la captura si el teléfono está bloqueado.

## Ampliación

- [x] T010 Conservar `pictogram_id` opcional en el modelo Android, el borrador y el JSON, con pruebas de alta, edición, quitar y rutina antigua
- [x] T011 Agregar el selector de pictogramas al formulario real y a la vista previa, con los PNG del catálogo
- [x] T012 Ajustar listado, formulario y diálogo a los estilos de Inicio sin cambiar su layout
- [x] T013 Llevar el id junto al recordatorio en el scheduler y mostrarlo a pantalla completa ocho segundos en el renderer de ojos y la LCD, con pruebas y una imagen exportada
- [x] T014 Compilar el APK, correr las pruebas de Android y Python, y capturar las vistas si hay un dispositivo desbloqueado. No desplegar el peluche en este paso.

## Dependencies

T001 precede a T004. T002 y T003 preceden a T006 y T007. T005 precede al guardado real de T004. T008 puede hacerse junto con T002. T009 cierra el bloque.

## Parallel Example

T002 y T001 pueden escribirse en paralelo. T006 espera el ítem de T003.
