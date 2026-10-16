# Rutinas: tarjetas compactas y correcciones pendientes

Este prompt reúne las correcciones funcionales detectadas y los cambios visuales pedidos el 14 de octubre de 2026. Reemplaza el prompt de ajustes anterior. La propuesta es retirar del listado las etiquetas Completada y Recordada, conservando sus datos y la lógica existente. Las demás decisiones visuales de este bloque proceden del pedido del usuario.

## Prompt completo

```text
Ajustá el módulo de Rutinas ya implementado. Quiero tarjetas más limpias y compactas, coherentes con Inicio, y corregir dos casos pendientes de los pictogramas. Continuá directamente sobre el trabajo actual: actualizá brevemente los documentos afectados de specs/002-rutinas y llevá los ajustes hasta su verificación. Conservá los cambios locales, la rama actual y las preferencias de AGENTS.md. No hagas commits, pushes ni despliegues en la Raspberry.

Estas indicaciones actualizan los detalles del boceto anterior: ya no quiero el bloque derecho con interruptor, texto Activada/Desactivada y etiqueta de seguimiento. Inicio aprobado sigue siendo la referencia de colores, tipografía, superficies, botones e iconos.

1. TARJETAS MÁS LIMPIAS
- Conservar la hora a la izquierda, el nombre y el mensaje en el centro, y el interruptor a la derecha.
- Alinear hora y nombre en la parte superior del contenido; la hora no debe quedar centrada dentro de una columna alta con mucho vacío arriba y abajo. El interruptor acompaña esa primera zona y no reserva una columna ancha para textos que ya no están.
- Quitar el texto visible Activada/Desactivada. El interruptor conserva su estado accesible y un nombre que identifique la rutina.
- Retirar del listado las etiquetas Completada y Recordada. Conservar los campos, el contrato y la lógica interna; no reemplazarlas por otro indicador inventado. Hoy reminded se marca al generar el aviso y completed depende del reconocimiento de voz y de la primera rutina pendiente: esas señales no prueban la ejecución física de la actividad.
- La altura de la tarjeta depende del contenido. Reducir el espacio sobrante, sin alturas fijas, rellenos para igualar tarjetas ni una columna de hora que abarque también las acciones del pie. Mantener márgenes regulares y permitir crecer con texto ampliado.
- Diferenciar la jerarquía: hora legible y destacada, nombre con peso seminegrita y mensaje secundario más discreto, con interlineado cuidado y separación breve. Usar las familias y estilos de Inicio; no poner todo en negrita ni introducir otra fuente decorativa.
- Conservar el mensaje que escribió el usuario. Presentarlo como texto secundario, sin agregar rótulos o adornos que carguen la tarjeta. Los nombres largos deben envolver bien, con espacio suficiente al quitar los estados del lado derecho.
- Editar y Eliminar quedan como acciones secundarias al pie, de tamaño y peso discretos. Usar petróleo o un tono secundario coherente; reservar el rojo destacado para confirmar la eliminación. Mantener áreas táctiles de 48 dp sin agrandar innecesariamente la superficie visible de los botones.

2. RUTINAS DESACTIVADAS
- Mostrar hora, nombre y mensaje en tonos más tenues y la tarjeta con una superficie sutilmente apagada, siguiendo la coherencia de los estados desconectados de Inicio y las alarmas desactivadas de un teléfono.
- La rutina sigue siendo legible. No aplicar una opacidad indiscriminada a toda la tarjeta: el interruptor debe poder activarla y Editar/Eliminar deben seguir disponibles.
- Aplicar y restaurar todos los colores al enlazar cada elemento del RecyclerView, para evitar que el estilo tenue se transfiera a otra rutina por reciclado.
- Probar alternar varias veces entre activada y desactivada en la app y la vista previa.

3. FORMULARIO
Mover «Si lo dejás vacío, se usará un mensaje automático» inmediatamente después del campo Mensaje del recordatorio y antes de Pictograma en TEO. La ayuda se refiere al mensaje, no a la imagen. Mantener la selección opcional, las miniaturas originales, Sin pictograma, edición, cancelación y conservación de datos ante errores.

4. CORRECCIONES FUNCIONALES DE LOS PICTOGRAMAS
- Si comienza un recordatorio principal sin pictograma, retirar cualquier imagen de rutina anterior todavía visible. Cubrir también un ID desconocido o un archivo ausente. Respetar las presentaciones prioritarias y los pictogramas de otras funciones.
- En SpeechWorker, el callback actual muestra la imagen antes de abrir la salida de audio. Asociarlo al comienzo efectivo de la reproducción, una sola vez, y contar los ocho segundos desde ese momento. Si no se consigue reproducir audio, no mostrar el pictograma. Si el audio se cancela o falla después de empezar, retirar la imagen de ese evento sin quitar la de otro posterior.
- Agregar pruebas de regresión de ambos casos y de la integración real del callback con SpeechWorker, además de los mocks del despachador. Mantener el preaviso sin imagen y el retorno a los ojos y al estado vigente.

5. VERIFICACIÓN Y ENTREGA
Usar los mismos layouts y estilos en producción y vista previa. Conservar Inicio aprobado y su selector de estados. Ejecutar las pruebas pertinentes de Android y Python, compilar el APK y comparar capturas reales de Rutinas con Inicio en el S25 si está disponible. Revisar una rutina con nombre largo, otra con mensaje de varias líneas y otra desactivada. Capturar listado, formulario y confirmación de eliminación. Corregir diferencias visibles antes de cerrar el ajuste.

Entregar resultados, APK y capturas, y distinguir lo comprobado en el teléfono de lo pendiente en el peluche físico. Instalar el APK no actualiza la Raspberry. Consultar solo bloqueos reales o decisiones nuevas indispensables.
```
