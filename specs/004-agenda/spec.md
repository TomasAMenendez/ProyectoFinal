# Agenda: turnos y vacunas

## Qué se construye

Agenda conserva el registro local de turnos y vacunas. Tiene dos pestañas, Turnos y Vacunas, un formulario completo para un turno nuevo y recordatorios en el teléfono. No consulta al peluche ni modifica la Raspberry.

## Turnos

El adulto ve los turnos ordenados por fecha y hora, con título, fecha legible, notas si existen y una acción Eliminar. Agregar abre un formulario con título obligatorio, fecha, hora y notas opcionales. Los selectores de fecha y hora son los de Android. Un error de validación no borra lo escrito. Eliminar pide confirmación, identifica el turno y lo quita por su identificador. Los turnos se guardan en el teléfono; eliminar no cancela una reserva con un consultorio.

## Vacunas

Se conserva el catálogo y lo que el adulto ya guardó. Cada vacuna muestra nombre, edad de referencia y estado. Una aplicada no ofrece programar. Una pendiente sin fecha dice «Sin fecha programada» y permite «Programar fecha». Una pendiente con fecha la muestra y permite «Cambiar fecha». Cancelar el selector conserva la fecha anterior. Marcar o desmarcar Aplicada no inventa una fecha de aplicación.

## Datos y avisos

Una instalación sin datos empieza con turnos vacíos y vacunas pendientes, sin aplicaciones afirmadas. Los datos ya guardados no se reinician. Una lectura fallida no se presenta como lista vacía. Las fechas antiguas sin año se muestran como fueron guardadas y no se les asigna un año para ordenarlas ni para avisar.

Los avisos de un turno son a la hora indicada y una hora antes. Los de una vacuna son a las 09:00 del día programado y del día anterior. Solo se programan avisos futuros. Eliminar un turno, cambiar una fecha o marcar una vacuna aplicada cancela los avisos correspondientes. Dos turnos con el mismo título no comparten alarma. Los avisos de Rutinas no se cancelan. Si las notificaciones están desactivadas, se indica cómo habilitarlas y el registro igual se guarda.

## Vista previa

Usa los mismos layouts, con ejemplos en memoria. Permite agregar, ordenar y eliminar turnos, y marcar vacunas o cambiar su fecha con los selectores reales. No escribe preferencias, no programa alarmas y no llama a TEO.

## Fuera de alcance

Editar turnos, reservas, profesionales, direcciones, anticipación configurable, alta o baja de vacunas, cálculo por edad y recomendaciones médicas. Entrar a Agenda mientras la navegación está bloqueada por desconexión queda como decisión aparte.
