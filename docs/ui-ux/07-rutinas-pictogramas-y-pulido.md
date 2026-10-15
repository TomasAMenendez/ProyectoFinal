# Rutinas: pictogramas opcionales y pulido visual

Decisiones del usuario, 14 de octubre de 2026: seleccionar un pictograma al crear o editar una rutina; la selección es opcional. TEO muestra la imagen temporalmente a pantalla completa y luego vuelven los ojos. Se autoriza implementar la función en Android y en el software del peluche. También se solicita pulir Rutinas tomando Inicio como base visual aprobada.

La duración inicial propuesta es de ocho segundos desde el inicio del recordatorio, reutilizando el tiempo actual de los pictogramas básicos. No se agrega otro ajuste al formulario en este bloque.

El [catálogo de originales](referencias/pictogramas/catalogo.json) registra los cinco PNG entregados. Se conservan los originales y sus nombres; los IDs estables permiten generar recursos con nombres compatibles con Android y resolver los mismos gráficos en la Raspberry.

## Prompt completo

```text
Ampliá el módulo de Rutinas ya implementado con pictogramas opcionales y mejorá su presentación para que tenga la coherencia visual del Inicio aprobado. Este pedido autoriza los cambios necesarios en ProyectoAndroid y ProyectoParaRasperrypiV5. Quiero la función conectada de verdad entre app, API, configuración, ejecución del recordatorio y pantalla del peluche, además de la vista previa.

Actualizá y revisá de forma breve la especificación, plan y tareas de specs/002-rutinas con esta ampliación y continuá hasta implementar y verificar. No recrees el módulo ni la constitución, ni te detengas al terminar cada etapa para pedirme otro comando. Conservá todos los cambios locales, la rama actual y AGENTS.md; no hagas commits, pushes ni cambios de rama. La autorización de este pedido amplía el alcance anterior, que conservaba el peluche sin cambios. ServidorDePC queda fuera salvo que se demuestre una dependencia imprescindible.

Leé y mirá estas referencias:
- docs/ui-ux/04-guia-visual-comun.md y .cursor/rules/visual-fidelity.mdc.
- docs/ui-ux/validacion/s25-inicio-detalles-2026-10-14.png: base visual aprobada.
- docs/ui-ux/bocetos/rutinas-elegida-2026-10-13.png, nueva-rutina-propuesta-2026-10-13.png y rutinas-dialogo-eliminar-referencia-2026-10-13.png.
- docs/ui-ux/validacion/s25-rutinas-listado-2026-10-14.png y s25-rutinas-nueva-2026-10-14.png: implementación actual a corregir.
- docs/ui-ux/referencias/pictogramas/catalogo.json y sus cinco PNG.

1. PICTOGRAMA OPCIONAL EN LA APP
En Nueva rutina y Editar rutina agregá «Pictograma en TEO (opcional)». El valor inicial de una rutina nueva es «Sin pictograma». Abrir un selector nativo con miniaturas, nombres legibles y selección visible: Baño, Cepillar los dientes, Comer, Jardín y Médico, más Sin pictograma. Poder elegir, cambiar y quitar la selección. Cancelar el selector conserva el valor anterior. Al editar, precargar el pictograma guardado. Conservar la selección ante errores y recreaciones del formulario. Elegir una imagen no envía nada al peluche hasta guardar la rutina.

Usá exactamente los PNG entregados, con sus proporciones y sin redibujarlos ni recortarlos. Los IDs del catálogo son banio, cepillar_dientes, comer, jardin y medico. Conservá los originales y prepará las copias necesarias en los recursos de Android y los assets de la Raspberry. La correspondencia de IDs, etiquetas e imágenes debe ser verificable en ambos proyectos; no buscar archivos de docs en tiempo de ejecución.

2. MODELO, API Y COMPATIBILIDAD
Agregá un campo opcional pictogram_id al modelo y al contrato de GET/POST /api/routines. Campo ausente, null o vacío significa sin pictograma para las rutinas antiguas. Sin pictograma es una opción válida: no exigir selección ni elegir automáticamente según el nombre de la rutina. Persistir y devolver la asociación por ID, sin enviar rutas del teléfono o recursos Android al peluche.

Crear, editar, activar, desactivar y eliminar deben conservar correctamente los pictogramas de todas las rutinas del lote. Editar otro campo no borra la selección; elegir Sin pictograma la elimina explícitamente. Conservar activación, anticipación, mensajes y demás campos existentes. Una imagen ausente o un ID no disponible no debe impedir que TEO diga el recordatorio ni provocar un cierre; resolver únicamente IDs del catálogo y manejar la ausencia de forma controlada. No simular sincronización ni resultados que la API no confirme.

3. EJECUCIÓN Y PANTALLA DE TEO
Al ejecutarse el recordatorio principal de una rutina con pictograma válido, mostrar su PNG a pantalla completa, centrado, sin deformación ni recortes. Usar inicialmente ocho segundos contados desde el inicio real de ese recordatorio; después restaurar los ojos y el estado vigente. Sin pictograma, el recordatorio continúa con la presentación normal y su audio. No usar otra imagen o un pictograma genérico como reemplazo automático.

Revisá routines.py, api_server.py, los consumidores del scheduler en app.py, SpeechWorker en workers.py, eye_display.py y eye_render.py. Hoy check_pending devuelve textos y EyeMode intenta resolver la imagen desde la primera rutina pendiente: eso puede asociar mal las imágenes cuando coinciden varias. Conservá juntos el ID de rutina, tipo de evento, mensaje y pictogram_id hasta su ejecución. Vinculá la imagen con su propio recordatorio y con la cola de voz; no mostrarla simplemente al encolar audio ni inferirla buscando el texto o la primera rutina pendiente.

El preaviso mantiene su comportamiento actual; el pictograma elegido corresponde al recordatorio principal. Verificar varias rutinas en el mismo ciclo, audio rechazado o cancelado y cambio a sueño. Evitar que un temporizador anterior quite la imagen de un evento posterior. Mantener la compatibilidad de las llamadas de voz existentes y respetar las prioridades de sueño, vinculación/PIN y otras presentaciones. Conservar los pictogramas básicos de otras funciones, como abrazo.

Implementar la imagen real en la ruta de la LCD física ST7789 y en la visualización de escritorio/canvas que corresponda. No darlo por implementado si solo se ve en una ventana de prueba o aparece el nombre del pictograma como texto. Reutilizar los renderizadores actuales, respetando dimensiones, orientación y brillo reales; no migrar el motor gráfico ni agregar dependencias sin una necesidad concreta.

4. PULIDO VISUAL DE RUTINAS
La distribución del listado sigue el boceto: hora destacada a la izquierda, nombre y mensaje en el centro, interruptor y estados a la derecha, Editar/Eliminar al pie. El encabezado debe alinear logo y título a la izquierda como Inicio. Reutilizá la jerarquía, colores, iconos, tipografía y botones ya afinados en TextAppearance.Teo.Home.* y Widget.Teo.Home.*, o extraé equivalentes compartidos conservando Inicio.

Corregí el tracking heredado, los márgenes acumulados, las acciones demasiado protagonistas, los interruptores visualmente pequeños y las alineaciones inconsistentes. Los textos del centro deben aprovechar su columna. Los controles del pie deben ser secundarios y la confirmación de eliminar tener el protagonismo destructivo. Conservar áreas de toque de 48 dp sin inflar innecesariamente toda la tarjeta. Agregar no debe dejar una franja vacía desproporcionada. Mantener estados y sincronización basados en datos reales.

En los formularios, usar etiquetas claras, campos alineados, separaciones regulares y un selector de pictograma integrado con el mismo estilo. La opción debe ser claramente opcional. Mantener Guardar/Cancelar, teclado, desplazamiento y datos ante fallos. Permitir crecer al formulario por el contenido; no reducir toda la letra ni forzar alturas para encajar la captura. Las medidas de la guía son puntos de partida ajustables mediante comparación visual. No rediseñar otras secciones.

5. VISTA PREVIA Y VERIFICACIÓN
Ampliá la vista previa existente con rutinas con y sin pictograma, elección, cambio, quitar, cancelar y edición. Debe usar los mismos layouts, catálogo y presentación que la app principal, con datos aislados de API, alarmas y almacenamiento reales. Conservar el selector de estados de Inicio y el primer uso de producción.

Probá el ida y vuelta del campo opcional, rutinas antiguas, edición sin pérdida, quitar selección, varias rutinas coincidentes, temporización, regreso a ojos y recursos ausentes. Ejecutá las pruebas pertinentes de Android y Python y compilá el APK. Exportá imágenes del renderer del peluche para comprobar que muestra los PNG reales a pantalla completa y después los ojos, aunque no esté disponible el hardware.

Con S25 o emulador disponible, capturá listado, alta, edición, selector y confirmación de eliminación; compará con los bocetos y con Inicio y corregí diferencias observables. Verificá también que Inicio conserve su presentación. La compilación no sustituye la comparación visual ni una prueba física con TEO.

Al terminar, entregá APK, capturas, resultados de pruebas y pendientes reales. Indicá los archivos y assets que deben actualizarse en la Raspberry y el procedimiento de despliegue existente: instalar el APK y actualizar el software del peluche son pasos separados. No afirmes que el peluche físico quedó actualizado o probado si eso no ocurrió. No despliegues en dispositivos remotos sin autorización de acceso. Consultá solo decisiones de producto no resueltas o bloqueos reales que impidan seguir.
```
