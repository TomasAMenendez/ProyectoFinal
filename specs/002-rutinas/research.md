# Research: Rutinas

## Decisiones

- **Subtítulo**: usar «Organizá recordatorios para acompañar su día», no la frase del boceto. Lo fija `docs/ui-ux/bocetos/seleccion-rutinas.md`.
- **Sincronización**: no mostrar «Sincronizado con TEO». Una respuesta HTTP no acredita la ejecución. El error de guardado sí se muestra y revierte la lista local.
- **Completada / Recordada**: `GET /api/routines` ya puede agregar `reminded` y `completed`. Se leen y no se recalculan. Completada prevalece si vienen juntas.
- **Mensaje automático**: se conserva `¡Es hora de ` + nombre en minúsculas, solo en el alta con mensaje vacío.
- **Edición**: mismo layout, otro título y otro botón. No muestra la aclaración de alta. Un mensaje vacío conserva el anterior.
- **Eliminar**: solo el diálogo del boceto. El fondo incorrecto de esa imagen no se implementa.
- **Vista previa**: ejemplos en `src/debug`, sin `RobotConnectionManager`, sin `ReminderScheduler` y sin preferencias de producción.
