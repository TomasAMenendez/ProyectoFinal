# Data Model: Rutinas

## Rutina

Identidad, nombre, hora `HH:mm`, mensaje, activación, anticipación y textos de transición ya existentes. `reminded` y `completed` son marcas de lectura. No se reenvían al guardar.

## Borrador

Nombre, hora y mensaje visibles. Alta: nombre obligatorio, activación verdadera, mensaje vacío reemplazado por la frase automática. Edición: copia la rutina y solo cambia los campos escritos; el mensaje vacío no pisa el anterior.

## Confirmación

Nombre de la rutina mostrada. Cancelar no cambia la colección. Eliminar quita esa identidad.
