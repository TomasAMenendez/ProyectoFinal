# Plan: Agenda

## Composición

Turnos y Vacunas comparten el encabezado de Contenido: logo, título Agenda, Avisos y Mi TEO. Dos pestañas con indicador petróleo. Subtítulo centrado. En Turnos, Agregar turno a ancho completo y la línea «Los turnos se guardan en este teléfono». Cada tarjeta lleva icono de calendario, título, fecha y hora, notas si hay, y Eliminar discreto al final. En Vacunas, la línea «Recordatorios en tu teléfono» y tarjetas con jeringa, nombre que envuelve, estado Aplicada o Pendiente, edad de referencia y, solo si está pendiente, la fecha y Programar fecha o Cambiar fecha. Nuevo turno es pantalla completa, con regreso, sin navegación inferior: título, fecha y hora en dos columnas, notas, la aclaración local, Guardar turno y Cancelar. El diálogo de eliminación reutiliza el de Rutinas, con botones centrados.

## Datos

`Appointment` guarda `atMillis` junto con el texto de fecha. Un valor mayor que cero ordena y programa. Un texto `dd/MM/yyyy HH:mm` también se interpreta porque el año está escrito. Un texto como «15 de septiembre, 10:00», sin año, se muestra tal cual, queda después de los turnos fechados y no genera un aviso. El catálogo inicial de vacunas queda pendiente y sin fecha. Si la preferencia ya existe, se lee sin reescribir aplicaciones ni notas. JSON ilegible es un fallo de lectura, no una lista vacía.

## Avisos

`ReminderPlan` decide qué códigos cancelar y qué alarmas futuras crear. Cada turno usa dos códigos estables por id: a la hora y una hora antes. Cada vacuna usa dos: las 09:00 del día y del día anterior. Al reprogramar se cancelan los códigos guardados y los códigos viejos calculados con el título, excepto los que pertenecen a Rutinas. `ReminderScheduler` aplica ese plan con el mecanismo actual de alarma exacta o su alternativa. `BootReceiver` sigue restaurando.

## Vista previa

`AgendaPreviewHost` infla los mismos layouts. Los ejemplos viven en memoria. Los selectores son los diálogos nativos. No usa `MedicalViewModel` ni `ReminderScheduler`.
