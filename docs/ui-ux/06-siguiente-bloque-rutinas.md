# Siguiente bloque: Rutinas

Adjuntar al pedido las imágenes `bocetos/rutinas-elegida-2026-10-13.png`, `bocetos/nueva-rutina-propuesta-2026-10-13.png` y `bocetos/rutinas-dialogo-eliminar-referencia-2026-10-13.png`.

Copiar el siguiente texto:

```text
Implementá el módulo completo de Rutinas sobre la app Android existente: listado, Nueva rutina, Editar rutina y confirmación para eliminar. Este pedido autoriza preparar y revisar los documentos necesarios e implementar el bloque de corrido. No te detengas al terminar cada etapa para pedirme otro comando.

Respetá AGENTS.md, .cursor/rules/visual-fidelity.mdc y .specify/memory/constitution.md. Usá las habilidades de Spec Kit instaladas para preparar especificación, plan y tareas breves del bloque nuevo en specs/002-rutinas; revisá y corregí sus inconsistencias antes de implementar. No recrees la constitución ni sobrescribas specs/001-inicio-base-visual. Conservá las verificaciones pendientes del primer bloque como pendientes. Continuá en la rama y checkout actuales, conservando todos los cambios existentes, sin commits, pushes ni cambios automáticos de rama.

Referencias que tenés que abrir y mirar:
- docs/ui-ux/bocetos/rutinas-elegida-2026-10-13.png
- docs/ui-ux/bocetos/nueva-rutina-propuesta-2026-10-13.png
- docs/ui-ux/bocetos/rutinas-dialogo-eliminar-referencia-2026-10-13.png
- docs/ui-ux/bocetos/seleccion-rutinas.md
- docs/ui-ux/04-guia-visual-comun.md
- docs/ui-ux/validacion/s25-inicio-detalles-2026-10-14.png

Para este bloque, usá el listado elegido y el formulario recibido como referencias visuales. Editar reutiliza el formulario de alta con sus datos precargados. Del boceto de eliminación tomá solo el diálogo; el fondo válido es el listado de Rutinas.

El Inicio actual ya está aprobado. Reutilizá su identidad, logo transparente, paleta, tipografía, iconos y navegación. La composición de Rutinas la define su propio boceto. Las medidas de la guía y los tokens son puntos de partida: ajustalas cuando se alejen del boceto, conservando áreas táctiles e insets. No restaures el espaciado de letras ni los márgenes anteriores de Inicio.

Fidelidad requerida:
- Listado: hora destacada a la izquierda; nombre y mensaje en el centro; interruptor y estado de activación a la derecha; Completada/Recordada cuando los datos lo indiquen; Editar y Eliminar visibles al pie. No sustituyas esta distribución por tarjetas genéricas apiladas. Conservá encabezado, Agregar rutina y barra inferior como en la referencia, con los ajustes de texto documentados.
- Nueva/Editar: pantalla completa con regreso, nombre, hora, mensaje, ayuda, acción principal y Cancelar. No conservar el diálogo beta como sustituto del formulario del boceto. El mensaje admite varias líneas; conservar datos y mostrar errores si falla la validación o el guardado.
- Eliminar: diálogo centrado con el nombre de la rutina y Cancelar/Eliminar, sobre el listado atenuado.

Conservá Kotlin, XML, ViewBinding, Fragments, ViewModels, Material Components, contratos y recordatorios existentes. La nueva rutina se crea activada; editar conserva su activación y los campos no editados. El mensaje vacío de alta usa la regla automática existente. No inventes estados de sincronización, completado ni éxito. No agregues opciones de anticipación o transición pendientes ni modifiques el peluche o el servidor.

Ampliá la vista previa de depuración para poder recorrer listado, alta, edición y eliminación sin TEO. Debe compartir los layouts y componentes de presentación reales, con ejemplos aislados de la API, los recordatorios y el almacenamiento de producción. No omitas el primer uso de la app real.

Verificá compilación y pruebas pertinentes. Si hay S25 o emulador disponible, instalá conservando datos, capturá cada vista y comparala con su boceto: columnas, alineaciones, jerarquía tipográfica, espacios, botones e iconos. Corregí las diferencias observables antes de entregar. Si modificás componentes compartidos, revisá también Inicio. Si falta un dispositivo o el peluche, indicá la limitación sin dar esas comprobaciones por hechas.

Avanzá con las decisiones ya documentadas. Preguntá solo por una decisión de producto realmente nueva o un bloqueo que impida seguir; no por cada elección de implementación. Al terminar, entregá el APK, las capturas disponibles y un resumen breve de lo comprobado y lo pendiente.
```
