# Propuestas de Agenda

Fecha: 13 de octubre de 2026. Etapa: preparación de bocetos, sin implementación.

## Turnos P14

Se recibió [esta propuesta de Turnos](agenda-turnos-propuesta-2026-10-13.png), todavía sin aprobación explícita del usuario. Usa el estilo de [Cuentos](cuentos-propuesta-2026-10-13.png), con título Agenda, pestañas Turnos/Vacunas y Agenda seleccionado en la navegación inferior. Corresponde a F47, F49 y F52 del [inventario](../01-inventario-y-pantallas.md). Para la implementación se propone unificar el indicador de pestaña con el azul petróleo y dejar espacio debajo de la última tarjeta.

La [agenda actual](../../../ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/medical/MedicalViewModel.kt) guarda los turnos localmente en el teléfono. El [listado y formulario beta](../../../ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/medical/MedicalFragment.kt) permiten consultar título, fecha/hora y notas opcionales, agregar y eliminar con confirmación. No se encontró edición de turnos. Los recordatorios son notificaciones Android, no órdenes narradas por TEO.

Se propone mostrar tres registros futuros ficticios, con fecha y hora legibles, título, notas cuando existan y una acción visible Eliminar. Agregar turno abre P15. No se agregan reservas médicas, confirmación con profesionales, sincronización con el peluche, filtros, calendario mensual o parámetros de anticipación. Eliminar quita el registro de la agenda; no cancela un turno con un consultorio. La confirmación reutilizará el estilo del diálogo ya bocetado, con textos propios de Agenda. La actualización de las alarmas al eliminar debe verificarse durante la implementación.

El escenario representa datos cargados y permisos de notificación concedidos. La posible disponibilidad de Agenda durante una desconexión sigue siendo una propuesta pendiente de revisar por el bloqueo de navegación actual; no se presenta como una función ya resuelta.

## Nuevo turno P15

Se recibió [esta propuesta de Nuevo turno](agenda-nuevo-turno-propuesta-2026-10-13.png), todavía sin aprobación explícita del usuario. Es un formulario dedicado con título del turno, fecha, hora, notas opcionales y Guardar turno/Cancelar. Título, fecha y hora son obligatorios. Fecha y hora se eligen con los selectores Android y se guardan juntas en el campo existente del modelo; separarlas visualmente no agrega datos al contrato actual. No se agregan profesional, dirección o intervalos de recordatorio como campos independientes, ni reservas con consultorios. La aclaración del formulario indica que los turnos se guardan en este teléfono. El escenario muestra campos completos y permisos de notificación concedidos, sin teclado ni selectores abiertos. Debe conservarse el formulario ante un error de validación; no se presenta como una corrección ya implementada. El encabezado y los iconos se unificarán con los demás formularios al implementar.

## Vacunas P16

Se recibió [esta propuesta de Vacunas](agenda-vacunas-propuesta-2026-10-13.png), todavía sin aprobación explícita del usuario. Muestra nombre, edad de referencia, estado Aplicada/Pendiente y fecha programada si existe. Corresponde a F50–F52. Marcar o desmarcar Aplicada reutiliza la operación actual de cambio de estado. Para pendientes, Programar fecha/Cambiar fecha hace visible la selección de fecha que la beta esconde en una pulsación prolongada. No se agregan alta o eliminación de vacunas, edición de nombres/edades, fecha de aplicación ni hora configurable.

La primera propuesta contiene una tarjeta vacía con una jeringa entre A y B; esa tarjeta es un error de generación y no forma parte del diseño ni del catálogo. Se recibió [esta revisión de la distribución](agenda-vacunas-revision-2026-10-13.png), todavía sin aprobación explícita, que quita la tarjeta vacía y unifica el icono de calendario. Introduce dos regresiones pendientes de resolver al implementar: B no tiene fecha ni control para programarla, y C tiene fecha pero el botón sigue diciendo Programar fecha.

Reglas de presentación: una vacuna aplicada no muestra un control de programación; una pendiente sin fecha muestra «Sin fecha programada» y «Programar fecha»; una pendiente con fecha muestra esa fecha y «Cambiar fecha». Las letras A/B/C y las fechas son datos ficticios, no reglas fijas del catálogo. La distribución de la revisión puede servir de referencia, aplicando estas condiciones en vez de copiar los errores de texto o visibilidad.

Se usarán nombres genéricos y datos ficticios para representar tres estados: aplicada, pendiente con fecha programada y pendiente sin fecha. Son datos del boceto, no un calendario médico real ni datos de un niño. La edad de referencia es una cadena de catálogo de solo lectura y la fecha programada la ingresa el adulto; no se promete cálculo automático por edad ni estados de vencimiento. La lista real del código no se modifica en esta etapa.
